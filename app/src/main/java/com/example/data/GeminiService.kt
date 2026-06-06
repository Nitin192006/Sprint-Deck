package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini REST API Request & Response structures with Moshi ---

data class GeminiPart(
    val text: String
)

data class GeminiContent(
    val parts: List<GeminiPart>
)

data class GeminiGenerationConfig(
    val responseMimeType: String? = null,
    val temperature: Float? = null
)

data class GeminiRequest(
    val contents: List<GeminiContent>,
    val generationConfig: GeminiGenerationConfig? = null
)

data class GeminiPartResponse(
    val text: String?
)

data class GeminiContentResponse(
    val parts: List<GeminiPartResponse>?
)

data class GeminiCandidate(
    val content: GeminiContentResponse?
)

data class GeminiResponse(
    val candidates: List<GeminiCandidate>?
)

interface GeminiApiService {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @POST
    suspend fun generateContentWithProxy(
        @retrofit2.http.Url url: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

object RetrofitClient {
    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val geminiService: GeminiApiService = retrofit.create(GeminiApiService::class.java)
}

object GeminiRateLimiter {
    private val requestTimestamps = mutableListOf<Long>()
    private const val MAX_REQUESTS_PER_MINUTE = 3 // Standard safe limit of max 3 requests per 60 seconds
    private const val WINDOW_MS = 60_000L

    @Synchronized
    fun checkRateLimit(): Boolean {
        val now = System.currentTimeMillis()
        requestTimestamps.removeAll { now - it > WINDOW_MS }
        if (requestTimestamps.size >= MAX_REQUESTS_PER_MINUTE) {
            return false
        }
        requestTimestamps.add(now)
        return true
    }

    @Synchronized
    fun getSecondsToWait(): Long {
        val now = System.currentTimeMillis()
        val oldest = requestTimestamps.firstOrNull() ?: return 0L
        val elapsed = now - oldest
        val remaining = WINDOW_MS - elapsed
        return if (remaining > 0) (remaining + 999) / 1000 else 0L
    }
}

object GeminiService {
    fun isApiConfigured(context: android.content.Context): Boolean {
        val prefs = context.getSharedPreferences("hackathon_tracker_prefs", android.content.Context.MODE_PRIVATE)
        val customApiKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        val customProxyUrl = prefs.getString("custom_gemini_proxy_url", "") ?: ""
        val isUsingProxy = customProxyUrl.isNotEmpty() && customProxyUrl.startsWith("http") && customProxyUrl != "placeholder"
        return customApiKey.trim().isNotEmpty() || isUsingProxy
    }

    suspend fun extractHackathonsWithAI(context: android.content.Context, promptInput: String): List<Hackathon>? {
        if (!GeminiRateLimiter.checkRateLimit()) {
            val seconds = GeminiRateLimiter.getSecondsToWait()
            throw IllegalStateException("API query rate limit reached. Please wait $seconds seconds before searching again to ensure api stability.")
        }

        val prefs = context.getSharedPreferences("hackathon_tracker_prefs", android.content.Context.MODE_PRIVATE)
        val customApiKey = prefs.getString("custom_gemini_api_key", "") ?: ""
        val customProxyUrl = prefs.getString("custom_gemini_proxy_url", "") ?: ""
        val isUsingProxy = customProxyUrl.isNotEmpty() && customProxyUrl.startsWith("http") && customProxyUrl != "placeholder"
        val proxyUrl = customProxyUrl

        val apiKey = customApiKey.trim()
        
        if (!isUsingProxy && apiKey.isEmpty()) {
            throw IllegalStateException("Gemini API Key is missing. Please configure your custom Gemini API Key in the Settings menu or on the first-use setup screen.")
        }
        
        val systemPrompt = """
            You are an expert assistant. Based on this topic, request, or description, generate or retrieve a list of highly realistic, detailed upcoming hackathons from diverse hosting platforms like Devpost, HackIndia, Unstop, MLH, HackerEarth, Devfolio, and GitHub.
            Even if the user request is simple, expand and provide up to 5 diverse, high-quality hackathons.
            If the user input specifies a single specific idea, you can output 1 relevant item, but always wrap it in the list format.
            Each hackathon must have realistic descriptions, target prizes, correct team limits, and a real domain.
            
            You MUST output ONLY a JSON object with this exact structure:
            {
              "hackathons": [
                {
                  "title": "Short descriptive hackathon title",
                  "description": "Brief 2-3 sentence overview of the challenges, rules, and outcomes. Include realistic technologies used.",
                  "prizes": "Cash amount or other prizes, e.g., ${'$'}15,000 top cash & global mentorships",
                  "teamSize": "e.g., 1-4 members, 2-5 members, or Individual",
                  "domain": "One of: AI & Machine Learning, FinTech, Web3 & Blockchain, ClimateTech, Open Innovation",
                  "deadline": "A date in format YYYY-MM-DD. It MUST be a date between 2026-06-15 and 2026-08-15.",
                  "timeline": "A human-readable timeline label, e.g. June 20 to June 25, 2026",
                  "externalLink": "A real, specific individual event page URL from platforms like Devpost, Unstop, Devfolio, HackIndia, MLH, or GitHub. For example: 'https://unstop.com/competitions/blockchain-innovation-sprint-7123', 'https://devfolio.co/hackathons/gemini-spark', 'https://hackindia.com/challenge-xyz', or 'https://ethglobal-london.devpost.com'. NEVER output generic search or home URLs like 'https://devpost.com/hackathons?search=...' or 'https://unstop.com/competitions'."
                }
              ]
            }
            Do not include markdown codeblocks (no ```json or ```), no markdown headers, and no trailing characters. Output clean JSON only.
        """.trimIndent()

        val formattedInput = "Topic: $promptInput\nSystem instructions: $systemPrompt"
        
        val request = GeminiRequest(
            contents = listOf(
                GeminiContent(
                    parts = listOf(
                        GeminiPart(text = formattedInput)
                    )
                )
            ),
            generationConfig = GeminiGenerationConfig(
                responseMimeType = "application/json",
                temperature = 0.3f
            )
        )

        return try {
            val response = if (isUsingProxy) {
                RetrofitClient.geminiService.generateContentWithProxy(proxyUrl, request)
            } else {
                RetrofitClient.geminiService.generateContent(apiKey, request)
            }
            val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
            if (text != null) {
                val cleanedText = text.trim()
                val jsonStart = cleanedText.indexOf('{')
                val jsonEnd = cleanedText.lastIndexOf('}')
                val jsonToParse = if (jsonStart != -1 && jsonEnd != -1 && jsonEnd > jsonStart) {
                    cleanedText.substring(jsonStart, jsonEnd + 1)
                } else {
                    cleanedText
                }

                // Parse JSON list using Moshi helper
                val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                val adapter = moshi.adapter(Map::class.java)
                val map = adapter.fromJson(jsonToParse) ?: throw Exception("Invalid JSON formatting")
                
                val hackathonsList = map["hackathons"] as? List<*> ?: emptyList<Any>()
                val resultList = mutableListOf<Hackathon>()
                
                for (item in hackathonsList) {
                    val hMap = item as? Map<*, *> ?: continue
                    val title = hMap["title"]?.toString() ?: "AI Discovered Hackathon"
                    val description = hMap["description"]?.toString() ?: "Discussing innovative concepts."
                    val prizes = hMap["prizes"]?.toString() ?: "$5,000 cash prize"
                    val teamSize = hMap["teamSize"]?.toString() ?: "1-4 members"
                    val domain = hMap["domain"]?.toString() ?: "Open Innovation"
                    val deadline = hMap["deadline"]?.toString() ?: "2026-06-30"
                    val timeline = hMap["timeline"]?.toString() ?: "June 25 - June 30, 2026"
                    var externalLink = hMap["externalLink"]?.toString() ?: "https://devpost.com/hackathons"
                    
                    if (!externalLink.startsWith("http://") && !externalLink.startsWith("https://")) {
                        externalLink = "https://$externalLink"
                    }

                    resultList.add(
                        Hackathon(
                            title = title,
                            description = description,
                            prizes = prizes,
                            teamSize = teamSize,
                            domain = domain,
                            deadline = deadline,
                            timeline = timeline,
                            isRegistered = false,
                            isIncomingAlert = false,
                            isUserCreated = false, // AI results are registered as public, not manual entries
                            externalLink = externalLink
                        )
                    )
                }
                resultList
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }
}
