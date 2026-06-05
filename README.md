# 🚀 Sprint Deck

Sprint Deck is a professional, minimalist tracker for discovering, registering, and managing hackathons. It helps developers and students stay organized with active deadlines, featuring AI-powered enrichment, persistent local storage, alarms, and optional cloud synchronization.

---

## ✨ Features

- **📊 Hackathon Deck View**: An interactive dashboard showing your upcoming hackathons with colorful indicators, progress tracking, and organized status filters.
- **🧠 AI-Powered Enrichment (Gemini API)**: Auto-enrich hackathon details, schedules, topic suggestions, and team building outlines powered by the Gemini 3.5 API.
- **⏰ Smart Reminders & Immersive Alarm**: Never miss a registration or submission deadline. Set custom reminders that trigger active notifications and an immersive overlay alarm.
- **💾 Local Persistence (Room Database)**: Save all your data locally. Designed to be offline-first and fast with full reactive updates.
- **☁️ Cloud Sync (Firebase)**: Synchronize your hackathons across devices with background cloud backup support.
- **🎨 Modern Material 3 UI**: Beautiful Jetpack Compose layout using modern transitions, dynamic shape themes, edge-to-edge support, and responsive layouts.

---

## 🛠️ Tech Stack & Architecture

Sprint Deck is built with modern Android development guidelines:

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose (Material Design 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Repository Pattern
- **Persistence**: SQLite via Android Jetpack Room Database
- **Background Operations & Alarms**: Android AlarmManager, Broadcast Receivers, and System Notifications
- **AI Integration**: Google Gemini API via Kotlin Services
- **Asynchronous Flow**: Kotlin Coroutines & StateFlow for fully reactive UI updates
- **Dependency Management**: Centralized Gradle Version Catalog (`libs.versions.toml` with Kotlin DSL)

---

## 📂 Project Structure

```
.
├── app
│   └── src
│       └── main
│           ├── java/com/example
│           │   ├── MainActivity.kt        # Primary navigation, main screen layouts
│           │   ├── data/                  # Data structures and underlying services
│           │   │   ├── Database.kt        # Room database & DAO configuration
│           │   │   ├── GeminiService.kt   # System connector for AI enrichment
│           │   │   ├── FirebaseSyncService.kt # Remote storage synchronization wrapper
│           │   │   ├── Repository.kt      # Data manager merging local and sync logic
│           │   │   └── ReminderAlarmReceiver.kt # Handles back-end alarm alarms & background events
│           │   └── ui/                    # Presentation layer components
│           │       ├── AlarmActivity.kt   # Immersive fullscreen screen for critical alarms
│           │       ├── HackathonViewModel.kt # Stateholder & task-flow coordinator
│           │       └── theme/             # Color Scheme, Typography, and Shapes (Material 3)
│           └── res/                       # Resource drawings, vectors, values, and XML layouts
├── .env.example                           # Configuration templates for your secrets
└── build.gradle.kts                       # Build configuration
```

---

## 🔑 Gemini AI & Environment Configuration

Sprint Deck uses secure client-side storage to enable AI integrations without risking API key exposure in shared builds.

### 🧠 Configure Gemini AI API Key (In-App)
When first using the application or inside **Settings > Secure API Credentials**, you can enter your own Google Gemini API key:
- **🔒 Secure Local-Only Storage**: All entered API keys are saved directly into your device's private `SharedPreferences` sandbox. They are **never** synced to Firebase, committed to Git, or uploaded to any cloud database.
- **✨ Get Your Free Key**:
  1. Go to [Google AI Studio](https://aistudio.google.com/)
  2. Click **Get API key** and create a key in a new context.
  3. Copy/paste the key directly into the application's setup overlay or settings panel.

### ☁️ Environment Injection (Optional Cloud/Build Configuration)
For customized distributions or cloud synchronization:
1. Copy `.env.example` to `.env` in the root of your workspace:
   ```bash
   cp .env.example .env
   ```
2. Set up your Firebase credentials under `.env`:
   ```properties
   # Firebase Database configuration (Optional)
   FIREBASE_API_KEY=your_optional_firebase_sync_key
   ```

---

## 🏃 Getting Started & Building

To build the application, make sure you have the Android SDK & Gradle installed. Alternately, use Google AI Studio's cloud builder:

### 1. Build Compilation
Compile the project and check for validation issues:
```bash
gradle compileDebugJavaWithJavac
```

### 2. Generate Debug APK
Build the final debug package (`.apk`):
```bash
gradle assembleDebug
```
The compiled output will be exported to `/app/build/outputs/apk/debug/app-debug.apk`.

---

## 🌐 Deploying to GitHub

Ready to push Sprint Deck to your personal GitHub? You can do so directly from Google AI Studio or manually.

### Method A: Direct Sync (Recommended)
1. Open the project inside **Google AI Studio**.
2. Click the settings icon or export options in the top-right menu.
3. Select **Push to GitHub** to authenticate and automatically create/push to a new repository.

### Method B: Manual Git Commands
If you downloaded the code as a ZIP archive, run the following commands in your local terminal:
```bash
# Initialize local git repository
git init

# Add all files to staging
git add .

# Create the initial commit
git commit -m "Initial commit: Sprint Deck Hackathon Tracker"

# Create a new repository on GitHub (via web interface)
# Run these commands to link and push:
git branch -M main
git remote add origin https://github.com/your-username/sprint-deck.git
git push -u origin main
```

---

## 📄 License & Attributions

Sprint Deck is licensed under the MIT License. See `LICENSE` for details.
All icons are designed in SVG and natively parsed into premium Material Vector Graphics.
