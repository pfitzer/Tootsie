# Tootsie - A Mastodon Client for Android

A modern, high-performance Mastodon client for Android built with Jetpack Compose and Kotlin.

## Development Status

**Current Phase**: Foundation Setup
**Version**: 1.0.0-alpha (pre-release)
**Project State**: New project - implementation starting from scratch

### Technical Analysis
This is a greenfield Android project. While CLAUDE.md lists ambitious features, the codebase currently contains only an empty Android Studio template with no implementation. All features listed (multi-account support, Jetpack Compose UI, Mastodon API integration) need to be built from the ground up.

---

## Features

### Planned
- Multi-account support
- Modern Jetpack Compose design
- Best performance
- Easy to use
- Supports all Mastodon API features

---

## Architecture

### Clean Architecture + MVVM + MVI
The app follows Clean Architecture principles with three layers:

**Presentation Layer (UI)**:
- Jetpack Compose for declarative UI
- MVVM pattern with ViewModels
- MVI-style state management with sealed UiState classes
- Single Activity architecture with Navigation Compose

**Domain Layer (Business Logic)**:
- Use cases encapsulating business operations
- Repository interfaces
- Pure Kotlin domain models
- Business rules and validation logic

**Data Layer (Data Sources)**:
- Repository implementations
- Room database for local persistence and offline support
- Retrofit + OkHttp for Mastodon API communication
- DataStore for app preferences and settings

### Key Architectural Patterns
- **Repository Pattern**: Abstract data sources behind interfaces
- **Dependency Injection**: Hilt for compile-time DI
- **Reactive Streams**: Coroutines + Flow for async operations
- **Offline-First**: Paging 3 RemoteMediator for cached timelines
- **Single Source of Truth**: Room database as canonical data source

---

## Technology Stack

### Core Technologies
- **Language**: Kotlin 2.0.21
- **Build System**: Gradle 8.13.2 with Version Catalogs (TOML)
- **Min SDK**: 31 (Android 12) - limits to ~86% of devices
- **Target SDK**: 36 (Android 15)
- **Java Version**: 11

### UI Framework
- **Jetpack Compose**: Modern declarative UI
- **Material Design 3**: Latest Material Design components
- **Navigation Compose**: Type-safe navigation
- **Coil**: Image loading optimized for Compose

### Architecture Components
- **Hilt**: Dependency injection framework
- **Room**: SQLite ORM for local database
- **ViewModel**: Lifecycle-aware state management
- **Paging 3**: Infinite scroll with offline caching
- **DataStore**: Preferences and settings storage

### Networking
- **Retrofit**: Type-safe HTTP client for Mastodon API
- **OkHttp**: HTTP client with interceptors
- **Kotlin Serialization**: JSON parsing (better performance than Gson)

### Background Processing
- **WorkManager**: Deferrable background tasks (notifications, sync)
- **Coroutines**: Structured concurrency
- **Flow**: Reactive data streams

### Media
- **ExoPlayer**: Video/audio playback
- **Coil**: Async image loading with caching

### Testing
- **JUnit**: Unit testing framework
- **MockK**: Mocking library for Kotlin
- **Compose Testing**: UI testing for Compose
- **Turbine**: Flow testing library
- **MockWebServer**: API mocking for integration tests

---

## Mastodon API Integration

### API Version Support
Targeting Mastodon API v1 and v2 (compatible with Mastodon 3.0+)

### Authentication
- **OAuth 2.0**: Standard web-based authorization flow
- **App Registration**: Dynamic app registration per instance
- **Multi-Instance**: Support any Mastodon-compatible instance
- **Token Storage**: Secure storage using EncryptedSharedPreferences

### Key API Endpoints
```
/api/v1/apps                        - Register application
/oauth/authorize                    - OAuth authorization
/oauth/token                        - Token exchange
/api/v1/accounts/verify_credentials - Verify user
/api/v1/timelines/home              - Home timeline
/api/v1/timelines/public            - Public timeline
/api/v1/statuses                    - Create/read/delete posts
/api/v1/statuses/:id/favourite      - Favorite post
/api/v1/statuses/:id/reblog         - Boost post
/api/v1/notifications               - User notifications
/api/v2/search                      - Search users/posts/hashtags
/api/v1/accounts/:id/follow         - Follow user
```

### Rate Limiting Strategy
- Respect X-RateLimit-* headers
- Implement exponential backoff on 429 responses
- Cache aggressively to minimize requests
- Batch operations where possible

### Instance Compatibility
- Feature detection via /api/v1/instance endpoint
- Graceful degradation for older Mastodon versions
- Support for Mastodon forks (Glitch, Hometown, etc.)

---

## Development Considerations

### Performance Targets
- Cold start time: < 2 seconds
- Timeline scroll: 60fps consistently
- Image loading: < 500ms on 4G connection
- Memory usage: < 200MB during normal use
- APK size: < 20MB

### Offline Support
- Timeline caching with Room + Paging RemoteMediator
- Queue failed posts for retry when online
- Offline indicators in UI
- Graceful error handling for network failures

### Accessibility
- Content descriptions for all images and icons
- TalkBack screen reader support
- High contrast mode compatibility
- Minimum touch targets: 48dp
- Dynamic font scaling support
- Keyboard navigation support

### Security
- HTTPS-only communication with API
- Secure token storage (EncryptedSharedPreferences)
- Input validation and sanitization
- XSS prevention in HTML content rendering
- ProGuard/R8 obfuscation for release builds
- No hardcoded secrets or API keys

### Localization (i18n)
- **Supported Languages**: English (default), German
- **Resource Strategy**: Android string resources with locale qualifiers
- **RTL Support**: Layout mirroring for right-to-left languages (future)
- **Date/Time Formatting**: Locale-aware formatting with DateTimeFormatter
- **Number Formatting**: Locale-specific number and currency formatting
- **Plurals**: Proper plural handling using Android quantity strings
- **Translation Coverage**: All user-facing strings must be externalized

### Multi-Account Architecture
- Separate Room databases per account OR single database with accountId scoping
- Quick account switching without full app reload
- Account-specific settings and preferences
- Cross-account posting capability
- Isolated notification channels per account

### Known Challenges
1. **HTML Content Rendering**: Mastodon posts contain HTML that must be safely rendered
2. **Media Upload Limits**: Different instances have different size/type limits
3. **Custom Emojis**: Need to download and cache custom emoji images
4. **Push Notifications**: Requires both FCM and Mastodon push subscription API
5. **Character Counting**: Mastodon has specific rules (URLs = 23 chars, etc.)
6. **Instance Diversity**: Different instances may have custom features/limits

---

## Project Structure

```
app/src/main/java/com/pfitzer/tootsie/
├── TootsieApplication.kt           # Hilt application entry point
├── MainActivity.kt                 # Single activity hosting Compose
│
├── di/                             # Dependency injection modules
│   ├── AppModule.kt
│   ├── DatabaseModule.kt
│   ├── NetworkModule.kt
│   └── RepositoryModule.kt
│
├── data/                           # Data layer
│   ├── local/
│   │   ├── dao/                    # Room DAOs
│   │   │   ├── AccountDao.kt
│   │   │   ├── StatusDao.kt
│   │   │   └── TimelineDao.kt
│   │   ├── entities/               # Room entities
│   │   │   ├── AccountEntity.kt
│   │   │   ├── StatusEntity.kt
│   │   │   └── TimelineRemoteKeys.kt
│   │   ├── datastore/              # Preferences
│   │   │   └── PreferencesManager.kt
│   │   └── TootsieDatabase.kt      # Room database
│   ├── remote/
│   │   ├── api/                    # Retrofit interfaces
│   │   │   ├── MastodonApiService.kt
│   │   │   ├── AuthApiService.kt
│   │   │   └── MediaApiService.kt
│   │   ├── dto/                    # Network DTOs
│   │   │   ├── StatusDto.kt
│   │   │   ├── AccountDto.kt
│   │   │   └── TokenResponse.kt
│   │   └── interceptors/
│   │       ├── AuthInterceptor.kt
│   │       └── LoggingInterceptor.kt
│   ├── repository/                 # Repository implementations
│   │   ├── AuthRepositoryImpl.kt
│   │   ├── TimelineRepositoryImpl.kt
│   │   └── StatusRepositoryImpl.kt
│   ├── mapper/                     # DTO ↔ Domain mappers
│   │   └── StatusMapper.kt
│   └── paging/                     # Paging sources
│       └── TimelinePagingSource.kt
│
├── domain/                         # Domain layer (business logic)
│   ├── model/                      # Domain models
│   │   ├── Account.kt
│   │   ├── Status.kt
│   │   ├── MediaAttachment.kt
│   │   └── AccessToken.kt
│   ├── repository/                 # Repository interfaces
│   │   ├── AuthRepository.kt
│   │   ├── TimelineRepository.kt
│   │   └── StatusRepository.kt
│   └── usecase/                    # Business logic use cases
│       ├── auth/
│       │   ├── LoginUseCase.kt
│       │   └── RegisterAppUseCase.kt
│       ├── timeline/
│       │   └── GetHomeTimelineUseCase.kt
│       └── status/
│           ├── FavoriteStatusUseCase.kt
│           └── PostStatusUseCase.kt
│
├── presentation/                   # Presentation layer (UI)
│   ├── navigation/
│   │   ├── TootsieNavHost.kt
│   │   └── TootsieDestinations.kt
│   ├── theme/
│   │   ├── Color.kt
│   │   ├── Theme.kt
│   │   └── Type.kt
│   ├── components/                 # Reusable UI components
│   │   ├── status/
│   │   │   ├── StatusCard.kt
│   │   │   └── StatusActions.kt
│   │   └── common/
│   │       └── AvatarImage.kt
│   ├── screens/
│   │   ├── auth/
│   │   │   ├── LoginScreen.kt
│   │   │   └── LoginViewModel.kt
│   │   ├── home/
│   │   │   ├── HomeScreen.kt
│   │   │   ├── HomeViewModel.kt
│   │   │   └── HomeUiState.kt
│   │   ├── compose/
│   │   │   └── ComposeScreen.kt
│   │   └── profile/
│   │       └── ProfileScreen.kt
│   └── util/
│       └── UiState.kt
│
└── util/                           # Cross-cutting utilities
    ├── Constants.kt
    ├── Result.kt                   # Result wrapper for domain
    └── Extensions.kt
```