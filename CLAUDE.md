# Tootsie - A Mastodon Client for Android

A modern, high-performance Mastodon client for Android built with Jetpack Compose and Kotlin.

## Development Status

**Current Phase**: Phase 4 In Progress - Polish & Advanced Features 🔨
**Version**: 1.0.0-rc4 (release candidate 4)
**Project State**: Full-featured Mastodon client with bookmarks and favorites screens

### Completed Features ✅

**Phase 1 - Core Infrastructure:**
- **OAuth 2.0 Authentication**: Full OAuth flow with dynamic app registration
- **Multi-Instance Support**: Users can connect to any Mastodon instance
- **Timeline Viewing**: Home and public timeline display with Paging 3
- **Offline Caching**: Timeline posts cached in Room database
- **Authentication Persistence**: Token and credentials stored securely in DataStore
- **Clean Architecture**: Repository pattern, use cases, ViewModels all implemented
- **Provider Pattern**: Thread-safe synchronous access to auth state for interceptors

**Phase 2a - Core Interactions:**
- **Post Interactions**: Favorite, boost, unbookmark, and bookmark posts
- **Status Context**: View conversation threads with ancestors and descendants
- **Reply Functionality**: Reply to posts with in-reply-to support

**Phase 2b - User Profiles & Notifications:**
- **User Profiles**: View account details, follower/following counts, bio, and posts
- **Follow/Unfollow**: Follow and unfollow accounts with relationship management
- **Notifications**: Real-time notifications for mentions, follows, favorites, and boosts
- **Notification Actions**: Dismiss individual notifications or clear all

**Phase 2c - Content Creation:**
- **Compose Posts**: Create new posts with character count and validation
- **Media Upload**: Upload images with alt text (accessibility descriptions)
- **Image Picker**: Android PhotoPicker integration for selecting media
- **Media Preview**: Grid view with edit and remove controls
- **Visibility Controls**: Public, unlisted, private, and direct message options
- **Content Warnings**: Add content warning text to posts

**Phase 2d - Search:**
- **Universal Search**: Search for accounts, posts, and hashtags across the Fediverse
- **Debounced Search**: Real-time search with 300ms debounce for performance
- **Filter Results**: Filter by type (All, Accounts, Posts, Hashtags)
- **WebFinger Support**: Lookup remote accounts via WebFinger protocol
- **Full Navigation**:
  - Click accounts to view their profiles
  - Click posts to view full status details with conversation threading
  - Click hashtags to search for all posts with that tag
- **Smart Hashtag Filtering**: Clicking a hashtag automatically filters to show only posts
- **Search Result Cards**: Rich preview cards showing avatars, follower counts, and post content
- **Empty States**: Helpful messaging when no results found or search hasn't started
- **Error Handling**: Graceful error messages with retry capability

**Phase 3a - Video & Audio Upload:**
- **Video Upload**: Upload mp4, webm, m4v, mov files via Android PhotoPicker
- **Audio Upload**: Upload mp3, ogg, wav files (backend ready, UI supports selection)
- **Media Type Indicators**: Visual badges showing VIDEO, AUDIO, or GIF labels
- **Video Preview**: Thumbnail with centered play button overlay
- **Audio Preview**: Large "AUDIO" text label on colored background
- **Format Support**: 11 total file formats (4 image, 4 video, 3 audio)
- **Unified Upload**: Same upload flow and alt text support for all media types

**Phase 3b - Draft Post Saving:**
- **Auto-save**: Drafts automatically saved every 5 seconds while typing
- **Draft Management**: View, resume, and delete saved drafts
- **Draft Persistence**: Drafts stored in Room database with full content preservation
- **Smart Triggering**: Auto-save triggered on content, media, visibility, or content warning changes
- **Draft Cleanup**: Automatically deleted when post is successfully published
- **Draft Metadata**: Stores content, media IDs, visibility, content warnings, reply context
- **Drafts Screen**: Dedicated UI showing draft previews with timestamps and metadata badges
- **Resume Editing**: Click any draft to continue where you left off
- **Navigation**: Easy access to drafts from compose screen toolbar

**Phase 3c - Advanced Timeline Filters:**
- **Hide Boosts**: Filter out all reblogged posts from timeline
- **Hide Replies**: Filter out all reply posts from timeline
- **Show Only Media**: Display only posts that contain media attachments
- **Filter Persistence**: Filter preferences saved in DataStore, persist across app restarts
- **Visual Indicator**: Badge on filter button shows when filters are active
- **Real-time Filtering**: Timeline automatically refreshes when filters change
- **Filter Dialog**: Clean UI with switches for each filter option
- **Performance**: Filtering happens in paging layer, maintains smooth scrolling

**Phase 3d - Multi-Account Support:**
- **Account Switching**: Switch between logged-in accounts seamlessly
- **Settings Screen**: Account management UI with list, switching, and removal
- **Data Isolation**: Account-specific timeline caches and drafts
- **Database Migrations**: Room v2 → v3 → v4 with composite keys and account scoping
- **Atomic Operations**: Thread-safe account switching updates database, preferences, and providers
- **Smart Removal**: Auto-switch to another account or logout when removing active account
- **Reactive Filtering**: flatMapLatest operator for automatic UI updates on account switch
- **Automatic Timeline Refresh**: Timeline instantly shows new account's posts when switching (no manual refresh)
- **Account List UI**: Rich account cards with avatars, display names, and active indicators
- **Navigation Integration**: Settings accessible from home screen toolbar
- **Session Persistence**: Stay on current screen when switching accounts

### Current Implementation Status
**Phase 2 (Core Features)** - Complete ✅
- ✅ Post interactions (favorite, boost, reply, bookmark)
- ✅ Compose/create posts with media upload and alt text
- ✅ User profiles with follow/unfollow
- ✅ Notifications with dismiss and clear actions
- ✅ Search functionality (accounts, statuses, hashtags)
- ✅ Content warnings and visibility controls
- ✅ Media preview and management
- ✅ Conversation threading

**Phase 3 (Advanced Features)** - Complete ✅
- ✅ Video & audio upload support (Phase 3a complete)
- ✅ Draft post saving with auto-save (Phase 3b complete)
- ✅ Advanced timeline filters (Phase 3c complete)
- ✅ Multi-account support (Phase 3d complete)

**Phase 4 (Polish & Advanced Features)** - In Progress 🔨
- ✅ Bookmarks & Favorites screens (Phase 4a complete)
- ⏳ Custom emoji support
- ⏳ Push notifications (FCM)
- ⏳ Lists support
- ⏳ Translation support

### Recent Updates (December 2024)

**Material Design Icons Implementation:**
- ✅ Added material-icons-extended library for full icon set (2,500+ icons)
- ✅ Replaced placeholder icons with semantically correct Material Design icons
- ✅ AttachFile icon for media attachment button (was Email)
- ✅ Reply icon for reply actions (was ArrowForward)
- ✅ Bookmark/BookmarkBorder icons for save actions (was Star)
- ✅ Proper icon semantics throughout the app
- ✅ Continuous blue border for replies in conversation threads
- ✅ Improved reply visualization in status detail screen

**Phase 3d - Multi-Account Support:**
- ✅ Database migration v2 → v3 → v4 with account scoping and cleanup
- ✅ AccountManagementRepository with atomic account switching
- ✅ Account-specific timeline caches (ownerAccountId in StatusEntity)
- ✅ Account-specific drafts (accountId in DraftEntity)
- ✅ Composite primary keys in TimelineRemoteKeys for account isolation
- ✅ SettingsScreen with account list and management UI
- ✅ SettingsViewModel with MVI pattern for state management
- ✅ Account switching without app reload (stays on current screen)
- ✅ Smart account removal (auto-switch or logout logic)
- ✅ Reactive filtering with flatMapLatest for draft/timeline updates
- ✅ Navigation integration (Settings icon in HomeScreen)
- ✅ 4 new use cases (GetAll, Switch, Add, Remove)
- ✅ Updated all repositories for account scoping
- ✅ Updated DAOs with account filtering parameters
- ✅ Updated TimelinePagingSource to cache with ownerAccountId
- ✅ HomeViewModel observes active account for automatic timeline refresh
- ✅ Bug fix: Migration v3→v4 cleans up orphaned cache with NULL ownerAccountId
- ✅ Bug fix: SettingsViewModel properly detects active account using repository
- ✅ Bug fix: Timeline auto-refreshes when switching accounts (no manual refresh needed)

**Phase 3c - Advanced Timeline Filters:**
- ✅ Hide boosts toggle (filters reblogged posts)
- ✅ Hide replies toggle (filters reply posts)
- ✅ Show only media toggle (filters non-media posts)
- ✅ Filter persistence across app restarts
- ✅ Visual badge indicator for active filters
- ✅ Real-time timeline refresh on filter changes
- ✅ Filter dialog with switches
- ✅ Filtering in paging layer for performance

**Phase 3b - Draft Post Saving:**
- ✅ Auto-save every 5 seconds while composing
- ✅ Draft persistence in Room database
- ✅ Drafts screen with preview cards
- ✅ Resume editing from drafts list
- ✅ Auto-delete on successful post
- ✅ Metadata badges (CW, Media)
- ✅ Relative timestamps (e.g., "5m ago")
- ✅ Navigation from compose toolbar

**Phase 3a - Video & Audio Upload:**
- ✅ Video upload support: mp4, webm, m4v, mov
- ✅ Audio upload support: mp3, ogg, wav
- ✅ Media type visual indicators (VIDEO/AUDIO/GIF badges)
- ✅ Video preview with play button overlay
- ✅ Audio preview with text label
- ✅ 11 total supported formats across image/video/audio

**Search Navigation & UX Improvements:**
- ✅ Fixed navigation: Accounts, posts, and hashtags now clickable
- ✅ Smart hashtag filtering: Clicking hashtags shows only posts with that tag
- ✅ Proper filter state: Filter chip automatically switches when clicking hashtags
- ✅ Complete navigation flow: Search → Profile, Search → Status Detail, Search → Hashtag posts

**HTML Rendering & Rich Content:**
- ✅ Rich HTML rendering with clickable hashtags, mentions, and links
- ✅ Hashtag navigation to search with automatic filtering
- ✅ Mention navigation to user profiles
- ✅ External link handling with browser integration
- ✅ Media attachment display (images, videos, audio)
- ✅ HTML parsing with proper span extraction
- ✅ Fixed status click functionality in conversation threads

**Media Upload UI:**
- ✅ Complete UI implementation with image picker integration
- ✅ Media preview grid with edit and remove controls
- ✅ Alt text dialog for accessibility
- ✅ AttachFile icon for media attachment (proper Material Design)

**Phase 4a - Bookmarks & Favorites Screens:**
- ✅ Bookmarks screen with paginated list
- ✅ Favorites screen with paginated list
- ✅ Full status interactions (favorite, boost, bookmark, reply)
- ✅ Navigation from home screen menu
- ✅ Status detail navigation
- ✅ Profile navigation
- ✅ Empty states and loading indicators
- ✅ Error handling

### Next Steps
Phase 4 - Polish & Advanced Features (Remaining):
- Custom emoji support
- Push notifications (FCM integration)
- Accessibility improvements (TalkBack optimization, contrast modes)
- Performance optimizations (timeline scroll, memory usage)
- Lists support (create and manage lists)
- Filters support (keyword and context filters)
- Translation support for posts
- Trending posts and hashtags

---

## Features

### Implemented
- Multi-account support with seamless switching
- Modern Jetpack Compose design with Material Design 3
- Offline-first architecture with Room caching
- Full OAuth 2.0 authentication flow
- Timeline viewing (Home, Public, Federated)
- Post interactions (favorite, boost, reply, bookmark)
- Rich media support (images, videos, audio)
- Draft auto-save and management
- Advanced timeline filters
- Search (accounts, posts, hashtags)
- Notifications with actions
- User profiles with follow/unfollow
- Content warnings and visibility controls
- HTML rendering with clickable links and hashtags
- Bookmarks screen with pagination
- Favorites screen with pagination

### Planned
- Custom emoji support
- Push notifications (FCM)
- Lists and filters
- Bookmarks and favorites screens
- Translation support
- Trending content

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

### Multi-Account Architecture (Implemented ✅)
- **Single Database with Account Scoping**: All data uses accountId/ownerAccountId fields for isolation
- **Atomic Account Switching**: Database → PreferencesManager → Provider updates in correct order
- **No App Reload**: UI automatically updates via Flow observation when switching accounts
- **Account-Specific Data**: Timeline caches and drafts completely isolated per account
- **Smart Account Removal**: Auto-switches to another account or logs out if removing active account
- **Reactive UI Updates**: flatMapLatest operator for automatic content updates on account switch
- **Automatic Timeline Refresh**: HomeViewModel observes active account and auto-refreshes timeline on switch
- **Thread-Safe Provider Pattern**: TokenProvider and InstanceUrlProvider with AtomicReference
- **Database Migrations v2 → v3 → v4**: Preserves existing data while adding account scoping and cleanup
- **Composite Primary Keys**: TimelineRemoteKeys uses (statusId, timelineType, ownerAccountId)
- **Settings Screen**: Rich UI with account list, switching, and removal capabilities

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

#### 1. DataStore Flow Consumption Pattern ⚠️ CRITICAL

**Problem**: Using `.collect {}` on `DataStore.data` Flow causes infinite blocking.

**Why**: `DataStore.data` is a **hot Flow** that continuously emits values and never completes. Using `.collect {}` on it blocks forever waiting for more emissions.

**Solution**: Always use `.first()` for one-time reads from DataStore:

```kotlin
// ❌ WRONG - Blocks forever
suspend fun getAccessToken(): String? {
    val preferences = dataStore.data.map { it[KEY_ACCESS_TOKEN] }
    var token: String? = null
    preferences.collect { token = it }  // Infinite blocking!
    return token
}

// ✅ CORRECT - Gets first emission and completes
suspend fun getAccessToken(): String? {
    return dataStore.data.map { it[KEY_ACCESS_TOKEN] }.first()
}
```

**Key Takeaway**: For one-time DataStore reads, use `.first()`. Only use `.collect {}` when you want to continuously observe changes (e.g., in UI layer with `collectAsState()`).

---

#### 2. OkHttp Interceptor Threading Constraints ⚠️ CRITICAL

**Problem**: Using `runBlocking` or suspend functions in OkHttp interceptors causes deadlocks.

**Why**:
- OkHttp interceptors run on worker threads from a limited thread pool
- `runBlocking` blocks the thread while waiting for coroutine completion
- If the coroutine needs resources from the same thread pool, deadlock occurs
- DataStore operations on Main dispatcher compound the problem

**Solution**: Use synchronous in-memory caching with the Provider pattern:

```kotlin
// ❌ WRONG - Causes deadlock
class AuthInterceptor(private val dataStore: DataStore<Preferences>) : Interceptor {
    override fun intercept(chain: Chain): Response {
        val token = runBlocking {
            dataStore.data.map { it[KEY_TOKEN] }.first()  // Deadlock!
        }
        // ...
    }
}

// ✅ CORRECT - Synchronous access with in-memory cache
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

#### 3. Provider Initialization Lifecycle ⚠️ IMPORTANT

**Problem**: Initializing providers too early (in init blocks or during Hilt construction) causes deadlocks or failures.

**Why**:
- Hilt dependency construction happens during app startup
- DataStore may not be fully initialized yet
- Init blocks with blocking code freeze the app before it starts

**Solution**: Explicit initialization in ViewModel after app starts:

```kotlin
// ❌ WRONG - Init block runs during Hilt construction
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

// ✅ CORRECT - Explicit initialization in ViewModel
@Singleton
class AuthRepositoryImpl @Inject constructor(...) {
    suspend fun initializeProviders() {
        val token = preferencesManager.getAccessToken()
        tokenProvider.setToken(token)
        // ...
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
            // ... continue with auth check
        }
    }
}
```

**Key Takeaway**: Don't initialize providers in repository init blocks. Use explicit initialization methods called from ViewModels with proper dispatcher context.

---

#### 4. Coroutine Dispatcher Selection for DataStore

**Problem**: Reading from DataStore on Main dispatcher can cause deadlocks in certain contexts.

**Why**: DataStore uses the IO dispatcher internally, but coordinating with Main dispatcher can cause issues when the caller is blocking or in a constrained context.

**Solution**: Always use `Dispatchers.IO` when reading from DataStore in initialization code:

```kotlin
// ❌ RISKY - May deadlock depending on context
viewModelScope.launch {  // Defaults to Dispatchers.Main
    authRepository.initializeProviders()  // Reads from DataStore
}

// ✅ SAFE - Explicitly uses IO dispatcher
viewModelScope.launch {
    withContext(Dispatchers.IO) {
        authRepository.initializeProviders()  // Safe DataStore access
    }
}
```

**Key Takeaway**: Wrap DataStore operations in `withContext(Dispatchers.IO)` to ensure they run on the correct dispatcher, especially during initialization.

---

#### 5. AtomicReference for Thread-Safe Caching

**Problem**: Multiple threads accessing shared state (tokens, URLs) in providers.

**Solution**: Use `AtomicReference` for thread-safe reads/writes without locks:

```kotlin
class TokenProviderImpl : TokenProvider {
    private val token = AtomicReference<String?>(null)

    override fun getTokenSync(): String? = token.get()  // Thread-safe read
    override fun setToken(token: String?) {
        this.token.set(token)  // Thread-safe write
    }
}
```

**Key Takeaway**: `AtomicReference` provides lock-free thread safety for simple get/set operations, perfect for caching authentication state.

---

#### 6. Debugging Threading Issues

**Techniques that helped identify the deadlock issues**:

1. **Comprehensive Logging**: Add logs before and after every async operation:
   ```kotlin
   Log.d(TAG, "Starting operation X...")
   val result = suspendOperation()
   Log.d(TAG, "Completed operation X: $result")
   ```
   If you see "Starting" but never "Completed", you've found the hanging point.

2. **Thread Name Logging**: Log the current thread to identify dispatcher issues:
   ```kotlin
   Log.d(TAG, "Running on thread: ${Thread.currentThread().name}")
   ```

3. **Check for `.collect()` on Infinite Flows**: Search codebase for patterns like:
   ```kotlin
   flow.collect { }  // Without proper scope or termination
   ```

4. **Verify Interceptor Constraints**: Never use blocking operations in OkHttp interceptors.

5. **Monitor Logcat for Hang Patterns**: If logs stop suddenly with no error, it's likely a deadlock or infinite wait.

**Key Takeaway**: When debugging hangs, add granular logging around all async operations and thread transitions. The absence of expected log messages reveals the exact blocking point.

---

### Implementation Patterns Summary

**For Authentication/Network Layer**:
- ✅ Use Provider pattern with `AtomicReference` for synchronous access in interceptors
- ✅ Initialize providers explicitly from ViewModels, not in init blocks
- ✅ Always use `.first()` for one-time DataStore reads
- ✅ Wrap DataStore operations in `withContext(Dispatchers.IO)`
- ❌ Never use `runBlocking` in OkHttp interceptors
- ❌ Never use `.collect {}` on infinite Flows for one-time reads

**For DataStore Operations**:
- ✅ Use `.first()` for one-time reads
- ✅ Use Flow properties for reactive observations in UI
- ✅ Run reads on `Dispatchers.IO` during initialization
- ❌ Never use `.collect {}` without proper scope/termination
- ❌ Don't read from DataStore in init blocks or Hilt construction

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