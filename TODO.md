# Tootsie - Project TODO List

This list contains suggested next tasks for the Tootsie Mastodon client, ranging from new features to architectural improvements and technical debt.

### 🚀 Feature Enhancements

- [ ] **Keyword and Context Filters (High Priority)**
    - **Description**: Implement the ability for users to filter out posts containing specific keywords or from specific contexts (e.g., home, notifications). This is a core Mastodon feature.
    - **Scope**: Create a `FiltersScreen`, update `StatusRepository` to handle Mastodon Filter APIs, and integrate filtering logic into `RemoteMediator` or `StatusDao`.
- [x] **Notification Management & Swipe-to-Dismiss**
    - **Description**: Fix notification deletion and implement swipe-to-dismiss for better UX.
    - **Scope**: Updated `NotificationsViewModel` and `NotificationsScreen` with `SwipeToDismissBox`.
- [x] **UX Polish: Swipe-to-Refresh & Scroll-to-Top**
    - **Description**: Ensure pull-to-refresh works everywhere and automatically scroll to top when new content arrives.
    - **Scope**: Fixed `TimelineList` refresh logic and added `LaunchedEffect` scroll triggers in all main screens.
- [x] **Poll Support**
    - **Description**: Specialized UI and logic for viewing, interacting with, and creating Mastodon polls.
    - **Scope**: Create a `PollComponent` for the status item, implement `VoteUseCase`, update the `Status` domain model, and implement poll creation in the compose screen.
- [x] **Account Profile Editing**
    - **Description**: Allow users to update their own profile (display name, bio, avatar, header, and metadata fields).
    - **Scope**: Added "Edit Profile" button to the `ProfileScreen` (when viewing own profile), created `EditProfileViewModel`, and implemented the corresponding API calls in `AccountRepository`.
- [x] **Lists Management UI**
    - **Description**: Ability to create, delete, and add/remove accounts from lists within the app.
    - **Scope**: Created `ListsScreen`, `ManageListScreen`, and associated ViewModels/UseCases.
- [x] **Followed Tags Management**
    - **Description**: Ability to view and manage followed hashtags.
    - **Scope**: Implemented `FollowedTagsScreen`, unfollow functionality, and follow/unfollow capability in `HashtagTimelineScreen` header.
- [x] **Trending Section Enhancements**
    - **Description**: Improve the UI for trending hashtags, posts, and links.
    - **Scope**: Created unified Trending screen with tabs and improved cards with trend charts.
- [x] **Private Messaging (Direct Messages)**
    - **Description**: Implement Mastodon's direct messaging (now "Conversations") feature to allow private communication between users.
    - **Scope**: Create a `ConversationsScreen`, implement `ConversationRepository`, and add a direct message option in the `ComposeScreen`.

### 🛠 Architectural & Technical Debt

- [x] **Use Case Consistency Refactoring**
    - **Description**: Standardize all business logic into UseCases across all ViewModels to fully align with Clean Architecture.
    - **Refactor**: Move remaining direct repository logic in `HomeViewModel`, `ProfileViewModel`, etc., to specialized UseCases.
- [ ] **Accessibility Audit & Implementation**
    - **Description**: Improve TalkBack support and ensure all interactive elements have proper content descriptions and touch targets.
    - **Scope**: Update components in `com.pfitzer.tootsie.presentation.components` with proper semantics and labels.

### 🎨 UI/UX Improvements

- [x] **Media Viewer Enhancements**
    - **Description**: Add zoom-to-pinch and swipe-to-dismiss gestures to the image viewer.
    - **Scope**: Update the `ImageViewerScreen` using Compose's transformation gestures.
