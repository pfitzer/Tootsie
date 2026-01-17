# Tootsie - Project TODO List

This list contains suggested next tasks for the Tootsie Mastodon client, ranging from new features to architectural improvements and technical debt.

### 🚀 Feature Enhancements

- [ ] **Keyword and Context Filters (High Priority)**
    - **Description**: Implement the ability for users to filter out posts containing specific keywords or from specific contexts (e.g., home, notifications). This is a core Mastodon feature.
    - **Scope**: Create a `FiltersScreen`, update `StatusRepository` to handle Mastodon Filter APIs, and integrate filtering logic into `RemoteMediator` or `StatusDao`.
- [x] **Poll Support**
    - **Description**: Specialized UI and logic for viewing, interacting with, and creating Mastodon polls.
    - **Scope**: Create a `PollComponent` for the status item, implement `VoteUseCase`, update the `Status` domain model, and implement poll creation in the compose screen.
- [ ] **Account Profile Editing**
    - **Description**: Allow users to update their own profile (display name, bio, avatar, header, and metadata fields).
    - **Scope**: Add an "Edit Profile" button to the `ProfileScreen` (when viewing own profile), create an `EditProfileViewModel`, and implement the corresponding API calls in `AccountRepository`.
- [ ] **Lists Management UI**
    - **Description**: Ability to create, delete, and add/remove accounts from lists within the app.
    - **Scope**: Create a `ManageListsScreen` and a `ManageListMembersScreen`.

### 🛠 Architectural & Technical Debt

- [ ] **Use Case Consistency Refactoring**
    - **Description**: Standardize all business logic into UseCases across all ViewModels to fully align with Clean Architecture.
    - **Refactor**: Move remaining direct repository logic in `HomeViewModel`, `ProfileViewModel`, etc., to specialized UseCases.
- [ ] **Accessibility Audit & Implementation**
    - **Description**: Improve TalkBack support and ensure all interactive elements have proper content descriptions and touch targets.
    - **Scope**: Update components in `com.pfitzer.tootsie.presentation.components` with proper semantics and labels.

### 🎨 UI/UX Improvements

- [ ] **Media Viewer Enhancements**
    - **Description**: Add zoom-to-pinch and swipe-to-dismiss gestures to the image viewer.
    - **Scope**: Update the `ImageViewerScreen` using Compose's transformation gestures.
