### Tootsie - Project Overview & Guidelines

#### Architecture
- **Clean Architecture**: Organized into `presentation`, `domain`, and `data` layers.
- **MVVM + MVI**: ViewModels manage state using `UiState` and `Action` patterns.
- **Single Activity**: `MainActivity` hosts `TootsieNavHost` using Jetpack Compose Navigation.

#### Tech Stack
- **Language**: Kotlin 2.0.21
- **UI**: Jetpack Compose with Material Design 3
- **Dependency Injection**: Hilt
- **Local Database**: Room (offline-first caching)
- **Preferences**: DataStore
- **Networking**: Retrofit + OkHttp
- **Push**: UnifiedPush (privacy-respecting, no FCM)
- **Paging**: Paging 3 for timelines

#### Critical Lessons Learned (Guardrails)
1. **DataStore**: Always use `.first()` for one-time reads from `DataStore.data` Flow to avoid infinite blocking.
2. **OkHttp Interceptors**: Never use `runBlocking` or suspend functions in interceptors. Use the **Provider Pattern** with `AtomicReference` for synchronous access.
3. **Initialization**: Avoid blocking DataStore reads in `init` blocks or Hilt construction. Use explicit `initializeProviders()` methods called from ViewModels (e.g., `SplashViewModel`).
4. **Threading**: Ensure DataStore operations run on `Dispatchers.IO`.

#### Coding Standards
- **MVI Pattern**: Use `UiState` (StateFlow) and `Action` (sealed class/interface) in ViewModels.
- **Resources**: All user-facing strings must be in `strings.xml` (Internationalization is mandatory: English and German supported).
- **Icons**: Use `androidx.compose.material.icons.Icons` and `Material Icons Extended`.
- **Navigation**: Use type-safe navigation with `TootsieDestinations`.

#### Multi-Account Handling
- All data in Room is scoped with `ownerAccountId`.
- Account switching is handled atomically via `AccountManagementRepository`.
- Timeline refresh is reactive, observing the active account.

#### Push Notifications
- Uses UnifiedPush with `WorkManager` fallback for polling.
- Each account has its own push configuration and VAPID keys.

#### Versioning
- **Semantic Versioning**: `major.minor.patch` (e.g., `1.0.0`).
- **Git Flow**: `main` branch for production releases.
- **Release Candidates**: `rc-` branches for testing.
- **Beta Releases**: `beta-` branches for testing.
- **Alpha Releases**: `alpha-` branches for testing.
- **Hotfixes**: `hotfix-` branches for critical fixes.
- **Version Code**: `1000000 + major * 100000 + minor * 1000 + patch`.
- **Version Name**: `major.minor.patch`.
- **Release Notes**: `CHANGELOG.md`.
- **Versioning Scheme**: `v1.0.0-rc1`.
- **Versioning Tool**: [Semantic Versioning Tool](https://semver.org/).

---
See also: [Junie Agent Definition](agent.md) for persona and working approach.
