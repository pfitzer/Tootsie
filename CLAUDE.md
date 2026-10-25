# Tootsie - A Mastodon Client for Android

A modern, high-performance Mastodon client for Android built with Jetpack Compose and Kotlin.

## Project Status

**Version**: 1.2.3-alpha
**Project State**: Full-featured Mastodon client with keyword filters and complete internationalization support

---

## GitHub Issues & Project Management

### Issue Tracking
All features, bugs, and enhancements are tracked via GitHub Issues at:
https://github.com/pfitzer/Tootsie/issues

### Using GitHub CLI (gh)

The `gh` CLI is used for all GitHub operations. Common commands:

**Issues:**
```bash
# List open issues
gh issue list

# View specific issue
gh issue view 123

# Create new issue
gh issue create --title "Bug: description" --body "Details here"

# Close issue
gh issue close 123
```

**Pull Requests:**
```bash
# Create PR from current branch
gh pr create --title "Fix: description" --body "Details"

# View PR
gh pr view 123

# List open PRs
gh pr list

# Check PR status
gh pr checks
```

**Workflow:**
1. Check existing issues: `gh issue list`
2. Create branch for issue: `git checkout -b feature/issue-XX-description`
3. Implement changes
4. Commit and push: `git push -u origin feature/issue-XX-description`
5. Create PR: `gh pr create`

---

## Features

### Implemented
- Multi-account support with seamless switching
- Modern Jetpack Compose design with Material Design 3
- Offline-first architecture with Room caching
- Full OAuth 2.0 authentication flow
- Timeline viewing (Home, Local, Federated) with tab switching
- Post interactions (favorite, boost, reply, bookmark)
- Rich media support (images, videos, audio)
- Draft auto-save and management
- Advanced timeline filters (hide boosts, hide replies, show only media)
- Search (accounts, posts, hashtags)
- Notifications with actions
- User profiles with follow/unfollow, followers and following lists
- Content warnings and visibility controls
- HTML rendering with clickable links and hashtags
- Bookmarks screen with pagination
- Favorites screen with pagination
- Lists management (create, view, update, delete)
- List timelines (view posts from specific lists)
- Custom emoji picker with shortcode insertion
- Trending content (hashtags, posts, links/articles)
- Followed tags (view, unfollow, navigate to hashtag timelines)
- Internationalization (English & German language support)
- Push notifications (UnifiedPush with WorkManager fallback)
- Keyword filters (create, delete, filter by context)
- Text size customization (small, medium, large, extra large)
- Profile editing
- Editing own posts (text, content warning, media and alt texts, poll), edit history of edited posts
- Deleting own posts, delete & redraft (opens the post as a draft, media kept)
- Domain blocking (block a remote user's server from the profile, manage blocked domains in settings)
- Scheduled posts (schedule from the compose screen, reschedule, edit or delete them)

### Planned
See open GitHub issues for planned features: `gh issue list --label enhancement`

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
- **Offline-First**: Paging 3 RemoteMediator for cached timelines (`TimelineRemoteMediator`: API → Room, the UI pages through Room; filters are applied to the paged data)
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
- **Material Icons Extended**: Full Material Design icon set (2,500+ icons)
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
- **Token Storage**: AES-256-GCM encryption with a non-exportable Android Keystore key (`CredentialCipher`), stored in DataStore (active account) and Room (all accounts)

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

## UnifiedPush Integration

### Overview
Tootsie implements **100% FOSS push notifications** using UnifiedPush - NO Google Firebase/FCM required. This provides privacy-respecting, decentralized push notifications compatible with any UnifiedPush distributor app.

### Architecture

**Core Components:**
1. **UnifiedPushService** (MessagingReceiver)
   - Receives 4 event types: NEW_ENDPOINT, MESSAGE, UNREGISTERED, REGISTRATION_FAILED
   - Delegates to UnifiedPushHandler interface in TootsieApplication

2. **UnifiedPushManager** (@Singleton)
   - Manages registration/unregistration with distributors
   - Detects available distributors (ntfy, NextPush, Conversations)
   - Uses accountId as "instance" for multi-account support

3. **TootsieApplication** (UnifiedPushHandler implementation)
   - Handles push callbacks with application-scoped coroutines
   - Manages state transitions: DISABLED → ACTIVE → POLLING
   - Coordinates between UnifiedPush, Mastodon API, and WorkManager

4. **NotificationPollWorker** (@HiltWorker)
   - 15-minute periodic fallback when no distributor available
   - Fetches recent notifications via Mastodon API
   - Cancels automatically when UnifiedPush becomes active

5. **NotificationHelper** (@Singleton)
   - Creates Android notification channels (Mentions, Follows, Boosts)
   - Displays notifications with proper formatting and deep links
   - Handles both push (PushPayload) and polling (Notification) sources

### VAPID Key Generation
- **Algorithm**: P-256 elliptic curve (secp256r1) using BouncyCastle
- **Storage**: Keys stored per-account in DataStore with base64url encoding
- **Generation**: On-demand during first push enable
- **Persistence**: Keys persist across app restarts for subscription continuity

### Push Flow

**Enable Push:**
1. User toggles push ON in settings
2. Generate VAPID keys (public, private) and auth secret
3. Save config to DataStore with status=DISABLED
4. Register with UnifiedPush (triggers dialog to select distributor)
5. UnifiedPush calls `handleNewEndpoint()` with push endpoint
6. Subscribe to Mastodon push API with endpoint + VAPID public key
7. Save endpoint to config, update status=ACTIVE
8. Cancel WorkManager polling (no longer needed)

**Receive Push:**
1. Mastodon server sends encrypted payload to UnifiedPush distributor
2. Distributor forwards to UnifiedPushService
3. UnifiedPushService calls `handleMessage()`
4. Decrypt payload (RFC 8291 aes128gcm: ECDH with the subscription key pair + auth secret, HKDF, AES-128-GCM)
5. Display notification via NotificationHelper

**Fallback to Polling:**
1. No distributor available OR registration fails
2. UnifiedPush calls `handleRegistrationFailed()`
3. Update status=POLLING, save to DataStore
4. Schedule WorkManager periodic worker (15-minute interval)
5. Worker fetches notifications newer than the last shown one (`last_notification_id_{accountId}`, also updated by push messages; it never moves back), the first poll only stores where to start
6. Display notifications for each new item

**Disable Push:**
1. User toggles push OFF in settings
2. Unregister from UnifiedPush distributor
3. Unsubscribe from Mastodon push API
4. Delete config from DataStore
5. Cancel WorkManager polling
6. Update status=DISABLED

### Multi-Account Support
- Each account has independent push subscription
- AccountId used as UnifiedPush "instance" parameter
- Separate VAPID keys per account stored in DataStore
- Config keys: `push_enabled_{accountId}`, `push_endpoint_{accountId}`, etc.
- Notifications tagged with accountId for proper routing

### Dependencies
- **UnifiedPush Connector**: 2.4.0 (JitPack repository)
- **BouncyCastle**: 1.70 (cryptography provider for EC keys)
- **Hilt Work**: 1.2.0 (dependency injection for WorkManager)
- **WorkManager**: Periodic background tasks for fallback polling

### Known Limitations
1. **Encryption**: Only standard RFC 8291 `aes128gcm` is supported (subscriptions are created with `standard: true`, Mastodon >= 4.4). UnifiedPush forwards no HTTP headers, so the legacy `aesgcm` scheme can not be decrypted; legacy subscriptions are re-created automatically on the first undecryptable message
2. **Distributor Detection**: Uses `getSavedDistributor()` (requires user to select distributor)
3. **Notification Persistence**: Notifications cleared on tap (no inbox storage)

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
- Secure token storage (`CredentialCipher`: Android Keystore AES-GCM; Room DB and DataStore excluded from backup)
- Input validation and sanitization
- XSS prevention in HTML content rendering
- ProGuard/R8 obfuscation for release builds
- No hardcoded secrets or API keys

### Localization (i18n)
- **Supported Languages**: English (default), German (100% complete)
- **String Resources**: 230+ externalized strings in values/strings.xml and values-de/strings.xml
- **Resource Strategy**: Android string resources with locale qualifiers
- **Complete Coverage**: All screens (25+), components (10+), error messages, empty states
- **Formatted Strings**: Proper placeholders (%1$s, %1$d) for dynamic content
- **Accessibility**: All content descriptions localized for TalkBack support
- **Zero Hardcoded Strings**: All user-facing text externalized (verified with build)
- **ViewModel Messages**: ViewModels never build user-facing strings. Error/success messages in UI state are `UiText?` (`presentation/util/UiText.kt`, e.g. `UiText.of(R.string.error_favorite)`), resolved in the UI with `asString()` (or `asString(context)` inside `LaunchedEffect`). `UiTextResourcesTest` checks that every `error_*`/`success_*` string resolves in EN and DE

### Multi-Account Architecture
- **Single Database with Account Scoping**: All data uses accountId/ownerAccountId fields for isolation
- **Atomic Account Switching**: Database → PreferencesManager → Provider updates in correct order
- **No App Reload**: UI automatically updates via Flow observation when switching accounts
- **Account-Specific Data**: Timeline caches and drafts completely isolated per account
- **Smart Account Removal**: Auto-switches to another account or logs out if removing active account
- **Reactive UI Updates**: flatMapLatest operator for automatic content updates on account switch
- **Thread-Safe Provider Pattern**: TokenProvider and InstanceUrlProvider with AtomicReference
- **Composite Primary Keys**: cached statuses use (id, timelineType, ownerAccountId), so a status can be in several timelines and accounts; the full status is stored as JSON (`statusJson`)

### Known Challenges
1. **HTML Content Rendering**: Mastodon posts contain HTML that must be safely rendered
2. **Media Upload Limits**: Different instances have different size/type limits
3. **Custom Emojis**: Need to download and cache custom emoji images
4. **Push Notifications**: Requires both FCM and Mastodon push subscription API
5. **Character Counting**: Mastodon has specific rules (URLs = 23 chars, etc.)
6. **Instance Diversity**: Different instances may have custom features/limits

---

## Lessons Learned

### Critical Architecture Patterns Discovered During Development

#### 1. DataStore Flow Consumption Pattern

**Problem**: Using `.collect {}` on `DataStore.data` Flow causes infinite blocking.

**Why**: `DataStore.data` is a **hot Flow** that continuously emits values and never completes. Using `.collect {}` on it blocks forever waiting for more emissions.

**Solution**: Always use `.first()` for one-time reads from DataStore:

```kotlin
// WRONG - Blocks forever
suspend fun getAccessToken(): String? {
    val preferences = dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    var token: String? = null
    preferences.collect { token = it }  // Infinite blocking!
    return token
}

// CORRECT - Gets first emission and completes
suspend fun getAccessToken(): String? {
    return dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
}
```

**Key Takeaway**: For one-time DataStore reads, use `.first()`. Only use `.collect {}` when you want to continuously observe changes (e.g., in UI layer with `collectAsState()`).

---

#### 2. OkHttp Interceptor Threading Constraints

**Problem**: Using `runBlocking` or suspend functions in OkHttp interceptors causes deadlocks.

**Why**:
- OkHttp interceptors run on worker threads from a limited thread pool
- `runBlocking` blocks the thread while waiting for coroutine completion
- If the coroutine needs resources from the same thread pool, deadlock occurs
- DataStore operations on Main dispatcher compound the problem

**Solution**: Use synchronous in-memory caching with the Provider pattern:

```kotlin
// WRONG - Causes deadlock
class AuthInterceptor(private val dataStore: DataStore<Preferences>) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = runBlocking {
            dataStore.data.map { it[KEY_TOKEN] }.first()  // Deadlock!
        }
        // ...
    }
}

// CORRECT - Synchronous access with in-memory cache
interface TokenProvider {
    fun getTokenSync(): String?
    fun setToken(token: String?)
}

class TokenProviderImpl : TokenProvider {
    private val token = AtomicReference<String?>(null)
    override fun getTokenSync(): String? = token.get()
    override fun setToken(token: String?) { this.token.set(token) }
}

class AuthInterceptor(private val tokenProvider: TokenProvider) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = tokenProvider.getTokenSync()  // Fast, non-blocking
        // ...
    }
}
```

**Key Takeaway**: Never use `runBlocking` or suspend functions in OkHttp interceptors. Use the Provider pattern with `AtomicReference` for thread-safe, synchronous access.

---

#### 3. Provider Initialization Lifecycle

**Problem**: Initializing providers too early (in init blocks or during Hilt construction) causes deadlocks or failures.

**Solution**: Explicit initialization in ViewModel after app starts:

```kotlin
// WRONG - Init block runs during Hilt construction
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val tokenProvider: TokenProvider
) {
    init {
        runBlocking {  // Blocks app startup!
            val token = preferencesManager.getAccessToken()
            tokenProvider.setToken(token)
        }
    }
}

// CORRECT - Explicit initialization in ViewModel
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val tokenProvider: TokenProvider
) {
    suspend fun initializeProviders() {
        val token = preferencesManager.getAccessToken()
        tokenProvider.setToken(token)
    }
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val authRepository: AuthRepositoryImpl
) : ViewModel() {
    init {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                authRepository.initializeProviders()
            }
        }
    }
}
```

**Key Takeaway**: Don't initialize providers in repository init blocks. Use explicit initialization methods called from ViewModels with proper dispatcher context.

**Background entry points**: WorkManager workers and UnifiedPush callbacks can start a cold process without the splash screen. Every entry point that calls the API must first call `AuthRepositoryImpl.ensureInitialized()` (idempotent, runs once per process on `Dispatchers.IO`). Otherwise requests go out without a token.

---

#### 4. Coroutine Dispatcher Selection for DataStore

**Problem**: Reading from DataStore on Main dispatcher can cause deadlocks in certain contexts.

**Solution**: Always use `Dispatchers.IO` when reading from DataStore in initialization code:

```kotlin
// RISKY - May deadlock depending on context
viewModelScope.launch {  // Defaults to Dispatchers.Main
    authRepository.initializeProviders()  // Reads from DataStore
}

// SAFE - Explicitly uses IO dispatcher
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        authRepository.initializeProviders()  // Safe DataStore access
    }
}
```

---

#### 5. Room Schema Changes Require a Version Bump and Schema File

**Rule**: Every change to a Room entity (new column, table, index, etc.) must:
1. Bump `version` in `TootsieDatabase`
2. Add a migration (manual `MIGRATION_X_Y` in `TootsieDatabase.ALL_MIGRATIONS`, or an `AutoMigration`)
3. Commit the generated schema file `app/schemas/com.pfitzer.tootsie.data.local.TootsieDatabase/<version>.json`
4. Add a test to `TootsieDatabaseMigrationTest` (instrumented, `MigrationTestHelper`)

**Why**: The Room Gradle plugin exports a schema JSON per version. `MigrationTestHelper` creates the old database from these files, so a missing schema file makes the migration untestable. Never edit an already committed schema file.

Run the migration tests with a connected emulator (not part of CI, which only runs unit tests):
```bash
./gradlew :app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.pfitzer.tootsie.data.local.TootsieDatabaseMigrationTest
```

**Note**: Schema version 1 was never released and is handled by `fallbackToDestructiveMigrationFrom(1)`. Schema 3 never existed in a commit; `3.json` is structurally identical to `4.json` (the 3→4 migration only deletes data).

---

### Implementation Patterns Summary

**For Authentication/Network Layer**:
- Use Provider pattern with `AtomicReference` for synchronous access in interceptors
- Initialize providers explicitly from ViewModels, not in init blocks
- Always use `.first()` for one-time DataStore reads
- Wrap DataStore operations in `withContext(Dispatchers.IO)`
- Never use `runBlocking` in OkHttp interceptors
- Never use `.collect {}` on infinite Flows for one-time reads

**For DataStore Operations**:
- Use `.first()` for one-time reads
- Use Flow properties for reactive observations in UI
- Run reads on `Dispatchers.IO` during initialization
- Never use `.collect {}` without proper scope/termination
- Don't read from DataStore in init blocks or Hilt construction

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
│   │   │   └── StatusEntity.kt
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
│       └── TimelineRemoteMediator.kt
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
