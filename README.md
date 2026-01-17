# Tootsie - Mastodon Client for Android

Tootsie is a modern, high-performance, and privacy-respecting Mastodon client for Android. Built from the ground up with Jetpack Compose and Kotlin, it follows Clean Architecture principles to provide a smooth, offline-first experience for the Fediverse.

## 🚀 Features

- **Multi-Account Support**: Seamlessly switch between multiple Mastodon accounts without app reloads.
- **Offline-First**: Robust caching with Room database and Paging 3 for smooth scrolling even without a connection.
- **Modern UI**: Fully built with Jetpack Compose and Material Design 3.
- **Privacy First**: Uses **UnifiedPush** for FOSS-friendly notifications (no Firebase/FCM required).
- **Rich Media**: Support for images, videos (ExoPlayer), and audio attachments.
- **Advanced Content**: 
    - Draft auto-save and management.
    - Custom emoji support with picker.
    - Full poll support (view, vote, and create).
    - Advanced timeline filters (hide boosts, replies, or show only media).
- **Global Reach**: Fully internationalized with support for English and German.
- **Fediverse Ready**: Universal search for accounts, posts, and hashtags; WebFinger support.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/) (2.0.21)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) with Material Design 3
- **Architecture**: Clean Architecture (Presentation, Domain, Data) + MVVM + MVI
- **Dependency Injection**: [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Preferences**: [DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Networking**: [Retrofit](https://square.github.io/retrofit/) + [OkHttp](https://square.github.io/okhttp/)
- **Serialization**: [Kotlinx Serialization](https://github.com/Kotlin/kotlinx.serialization)
- **Push Notifications**: [UnifiedPush](https://unifiedpush.org/)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Paging**: [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- **Background Tasks**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)

## 📋 Requirements

- Android Studio Ladybug | 2024.2.1 or newer
- JDK 11
- Android Device/Emulator running Android 12 (API 31) or higher

## ⚙️ Setup & Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/pfitzer/tootsie.git
   cd tootsie
   ```

2. **Open in Android Studio**:
   Import the project and let Gradle sync.

3. **Build & Run**:
   Select the `app` configuration and run it on your device or emulator.

### Build Commands (Gradle)
- **Assemble Debug APK**: `./gradlew assembleDebug`
- **Assemble Release APK**: `./gradlew assembleRelease`
- **Run Unit Tests**: `./gradlew test`
- **Run Android Instrumented Tests**: `./gradlew connectedAndroidTest`

## 📂 Project Structure

```
app/src/main/java/com/pfitzer/tootsie/
├── TootsieApplication.kt      # Hilt application entry point
├── MainActivity.kt            # Single activity hosting Compose
├── di/                        # Dependency injection modules
├── data/                      # Data layer (Room, Retrofit, DataStore)
│   ├── local/                 # DAOs, Entities, Database
│   ├── remote/                # API interfaces, DTOs, Interceptors
│   ├── repository/            # Repository implementations
│   └── paging/                # Paging sources & RemoteMediators
├── domain/                    # Domain layer (Business logic)
│   ├── model/                 # Pure Kotlin domain models
│   ├── repository/            # Repository interfaces
│   └── usecase/               # Use cases (Action-based logic)
├── presentation/              # Presentation layer (UI)
│   ├── screens/               # Screen-level Composables & ViewModels
│   ├── components/            # Reusable UI components
│   ├── navigation/            # Type-safe navigation configuration
│   └── theme/                 # Material3 Theme & Styling
└── util/                      # Cross-cutting utilities
```

## ⚠️ Critical Development Guidelines

When contributing to Tootsie, please adhere to these architectural guardrails:

1. **DataStore Reads**: Never use `.collect {}` on `DataStore.data` for one-time reads as it is a hot flow and will block infinitely. Always use `.first()`.
2. **OkHttp Interceptors**: Never use `runBlocking` or suspend functions inside Interceptors. Use the **Provider Pattern** with `AtomicReference` for thread-safe, synchronous access to state (tokens, base URLs).
3. **Initialization**: Avoid blocking DataStore reads in `init` blocks or Hilt construction. Use explicit `initializeProviders()` methods called from ViewModels (e.g., `SplashViewModel`).
4. **Threading**: Ensure all DataStore operations and heavy IO run on `Dispatchers.IO`.
5. **Strings**: All user-facing strings **must** be placed in `strings.xml` for internationalization.

## 📝 TODOs & Future Work

- [ ] Full RFC 8291 Web Push encryption implementation.
- [ ] Keyword and context filters.
- [ ] Accessibility optimizations (TalkBack contrast modes).
- [ ] Performance profiling for timeline scrolling.
- [ ] Notification deduplication in polling mode.
- [ ] Profile editing (bio, avatar, etc.).

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
