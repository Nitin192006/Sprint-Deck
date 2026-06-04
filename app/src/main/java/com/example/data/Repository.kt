package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class HackathonRepository(private val dao: HackathonTrackerDao) {

    suspend fun findUserByUsername(username: String): User? = dao.getUserByUsername(username)
    suspend fun findUserByEmail(email: String): User? = dao.getUserByEmail(email)
    suspend fun findUserById(id: Int): User? = dao.getUserById(id)
    suspend fun registerNewUser(user: User): Long = dao.insertUser(user)
    suspend fun updateUser(user: User) = dao.updateUser(user)
    suspend fun resetUserPassword(username: String, email: String, newPass: String): Boolean {
        return dao.resetUserPassword(username, email, newPass) > 0
    }

    val allHackathons: Flow<List<Hackathon>> = dao.getAllHackathons()

    suspend fun insertHackathon(hackathon: Hackathon) = dao.insertHackathon(hackathon)
    suspend fun updateHackathon(hackathon: Hackathon) = dao.updateHackathon(hackathon)
    suspend fun deleteHackathon(hackathon: Hackathon) = dao.deleteHackathon(hackathon)

    suspend fun registerForHackathon(hackathonId: Int, userId: Int, reminderDays: Int) {
        dao.updateRegistrationStatus(hackathonId, true, userId, reminderDays)
    }

    suspend fun unregisterFromHackathon(hackathonId: Int) {
        dao.updateRegistrationStatus(hackathonId, false, null, 1)
    }

    suspend fun prepopulateIfEmpty() {
        // Query to check if seed is necessary
        val currentList = dao.getAllHackathons().first()
        if (currentList.isEmpty()) {
            val initialList = listOf(
                // ---- AI & MACHINE LEARNING ----
                Hackathon(
                    title = "Global AI Solutions Hackathon",
                    description = "Build visual or LLM-based solutions addressing climate energy consumption, accessibility, or medical diagnosis. Powered by Gemini Flash models.",
                    prizes = "$15,000 top prize & cloud credits",
                    teamSize = "1-4 members",
                    domain = "AI & Machine Learning",
                    deadline = "2026-06-12",
                    timeline = "June 12 - June 15, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://unstop.com/competitions/global-ai-solutions-2026"
                ),
                Hackathon(
                    title = "Gemini Developers Global Sprint",
                    description = "Unleash multimodal agent systems, structured JSON schemas, and function-call workflows using the latest Gemini models.",
                    prizes = "$40,000 total cash & hardware kits",
                    teamSize = "2-5 members",
                    domain = "AI & Machine Learning",
                    deadline = "2026-06-25",
                    timeline = "June 25 - June 29, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://gemini-sprint.devpost.com"
                ),
                Hackathon(
                    title = "LLM Agent Safety Innovations",
                    description = "Focus on red-teaming, alignment guarantees, jailbreak prevention, and robust structured pipelines for LLM agents.",
                    prizes = "$20,000 rewards & mentorship",
                    teamSize = "Individual or Pairs",
                    domain = "AI & Machine Learning",
                    deadline = "2026-07-08",
                    timeline = "July 10 - July 14, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://llm-safety-agents.devpost.com"
                ),

                // ---- FINTECH ----
                Hackathon(
                    title = "FinTech Innovation Sprint",
                    description = "Create customizable micro-payment tools, embedded savings trackers, or secure transaction wrappers.",
                    prizes = "$30,000 global prize pool",
                    teamSize = "2-5 members",
                    domain = "FinTech",
                    deadline = "2026-06-22",
                    timeline = "June 22 - June 28, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://fintech-sprint.devpost.com"
                ),
                Hackathon(
                    title = "Decentralized Banking Hackathon",
                    description = "Build smart yield protocols, cross-border payment APIs, or transparent lending books.",
                    prizes = "$25,000 cash rewards",
                    teamSize = "1-4 members",
                    domain = "FinTech",
                    deadline = "2026-07-05",
                    timeline = "July 05 - July 10, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://devfolio.co/hackathons/decentral-banking"
                ),
                Hackathon(
                    title = "AI Tax & Compliance Assistant",
                    description = "Optimize automated state filings, dynamic ledger reconciliations, or intelligent expense auditing pipelines.",
                    prizes = "$18,000 developer grant",
                    teamSize = "Individual or Pairs",
                    domain = "FinTech",
                    deadline = "2026-07-20",
                    timeline = "July 22 - July 26, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://ai-tax-assistant.devpost.com"
                ),

                // ---- WEB3 & BLOCKCHAIN ----
                Hackathon(
                    title = "Web3 Borderless Developers",
                    description = "Develop consumer-facing applications linking cryptographic asset registration to physical products.",
                    prizes = "$10,000 cash & seed stage",
                    teamSize = "Individual or Pairs",
                    domain = "Web3 & Blockchain",
                    deadline = "2026-06-29",
                    timeline = "July 01 - July 05, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://web3-borderless.devpost.com"
                ),
                Hackathon(
                    title = "Zero Knowledge Proofs Global Hack",
                    description = "Use zero-knowledge cryptographic primitives to safely prove identity without revealing sensitive records.",
                    prizes = "$35,000 validator pools",
                    teamSize = "2-5 members",
                    domain = "Web3 & Blockchain",
                    deadline = "2026-07-15",
                    timeline = "July 17 - July 22, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://zkp-global-hack.devpost.com"
                ),
                Hackathon(
                    title = "DAO Governance & Voting Systems",
                    description = "Formulate transparent, quadratic voting platforms with Sybil-resistant identity proofing.",
                    prizes = "$12,000 voting grant",
                    teamSize = "1-4 members",
                    domain = "Web3 & Blockchain",
                    deadline = "2026-07-28",
                    timeline = "July 28 - July 31, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://dao-voting-gov.devpost.com"
                ),

                // ---- CLIMATETECH ----
                Hackathon(
                    title = "Sustainable Tech Global",
                    description = "Design IoT networks, power grid optimizations, or localized logistics chains seeking to mitigate emissions.",
                    prizes = "$25,000 green credits & cash",
                    teamSize = "1-4 members",
                    domain = "ClimateTech",
                    deadline = "2026-07-10",
                    timeline = "July 12 - July 18, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://hackindia.com/challenge/sustainable-tech"
                ),
                Hackathon(
                    title = "Renewable Grid Optimizer",
                    description = "Forecast planetary solar/wind yields, dispatch batteries, or coordinate intelligent peer-to-peer microgrids.",
                    prizes = "$22,000 dynamic climate fund",
                    teamSize = "2-5 members",
                    domain = "ClimateTech",
                    deadline = "2026-06-18",
                    timeline = "June 18 - June 20, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://unstop.com/competitions/renewable-grid-opt"
                ),
                Hackathon(
                    title = "Urban Carbon Tracker",
                    description = "Track and offset shipping, retail, or real-estate carbon footprints using localized open-source indices.",
                    prizes = "$15,000 green rewards",
                    teamSize = "Individual or Pairs",
                    domain = "ClimateTech",
                    deadline = "2026-07-24",
                    timeline = "July 25 - July 28, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://urban-carbon-track.devpost.com"
                ),

                // ---- OPEN INNOVATION ----
                Hackathon(
                    title = "AI Studio Developer Challenge",
                    description = "Pitch any creative idea that leverages server-side Gemini APIs for helpful consumer integrations.",
                    prizes = "$50,000 major grand prize",
                    teamSize = "1-4 members",
                    domain = "Open Innovation",
                    deadline = "2026-06-16",
                    timeline = "June 16 - June 22, 2026",
                    isRegistered = false,
                    isIncomingAlert = true,
                    externalLink = "https://aistudio-challenge.devpost.com"
                ),
                Hackathon(
                    title = "Next-Gen Accessibility Hack",
                    description = "Build assistive visual tools, natural-voice screens, or gesture-controlled inputs for mobile operating systems.",
                    prizes = "$20,000 foundation sponsorship",
                    teamSize = "Individual or Pairs",
                    domain = "Open Innovation",
                    deadline = "2026-07-12",
                    timeline = "July 12 - July 15, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://nextgen-accessibility.devpost.com"
                ),
                Hackathon(
                    title = "Disaster Relief Tech Sprint",
                    description = "Coordinate localized routing charts, volunteer matching, or emergency signal beacons during extreme conditions.",
                    prizes = "$30,000 rescue funding",
                    teamSize = "2-5 members",
                    domain = "Open Innovation",
                    deadline = "2026-07-29",
                    timeline = "August 01 - August 05, 2026",
                    isRegistered = false,
                    isIncomingAlert = false,
                    externalLink = "https://disaster-rescue.devpost.com"
                )
            )
            dao.insertAll(initialList)
        }
    }
}
