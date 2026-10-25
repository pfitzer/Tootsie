# Changelog

All notable changes to Tootsie are documented here.

## [1.3.1] - 2026-10-25

### Fixed
- Removing an account now deletes all of its data on the device (cached timelines, drafts, filters, recent searches, push keys); an active push subscription is ended on the server first.
- Empty boxes in posts and bios that contain Unicode line separators (U+2028/U+2029).

### Under the hood
- Dependency updates; patched versions of vulnerable build tool dependencies (jose4j, jdom2, commons-lang3, httpclient); the Gradle wrapper verifies the distribution checksum.

## [1.3.0] - 2026-10-25

### New
- **Edit your posts**: change text, content warning, media and alt texts, and polls; edited posts show "edited" and their full edit history.
- **Delete & redraft**: delete a post and open it again as a draft, media included; plain delete is available as well.
- **Scheduled posts**: pick a publication time when composing; reschedule, edit or delete scheduled posts.
- **Translate posts** in other languages through your server's translation service, or hand them to an installed translator app.
- **Quote posts** via the boost button, respecting the author's quote policy (Mastodon 4.5+).
- **Followers and following lists** on every profile, and lists of who **favourited and boosted** a post.
- **Notifications for new posts** of an account (bell on the profile) and **private notes** about accounts.
- **Block entire domains** from a remote profile; manage blocked domains in the settings. The server of remote accounts is shown on their profile.
- **Mute, block and report** accounts and posts.
- **Reading position sync**: the home timeline position and read notifications are shared with the web interface and other apps (markers); new notifications are highlighted.
- **Offline-first timelines**: home, local and federated timelines are cached and stay scrollable without a connection.
- Threads load **missing replies from other servers** (Mastodon 4.5+).
- **Better search**: remote accounts and posts by URL or handle, previews, paging, recent searches.
- **Modern compose screen**: account switcher, language picker, autocomplete for mentions, hashtags and emojis, instance character limit with Mastodon-style counting, mark media as sensitive.
- **Drafts** keep their media, alt texts, poll and language.
- **Filters**: clearer list and a full-screen editor; keyword filters match the visible text of a post.
- Custom emojis in posts, content warnings, polls, names and bios.
- Sortable followed and trending hashtags, profile tabs (posts, replies, media, pinned) with a collapsing header, announcements, follow requests, text size setting.
- **Accessibility**: TalkBack actions on every post, counts read with their buttons, descriptions for media without alt text, poll options as radio buttons/checkboxes.

### Fixed
- Typing fast in the compose screen dropped characters.
- Alt texts edited before posting were not sent to the server.
- Notification polling (without UnifiedPush) showed notifications more than once.
- Push notifications: decryption per RFC 8291, notification permission on Android 13+, each account polled with its own credentials, polling stops when the server rejects a token.
- The timeline kept jumping to the top when going back from a thread.
- An empty band above every screen title (status bar inset applied twice).
- Content warnings now hide text, media and polls; sensitive media is blurred.
- Favourite, boost and bookmark state is shown consistently in every list.
- Many smaller fixes in search, notifications, polls, lists and profiles.

### Security & privacy
- Access tokens and instance URLs are encrypted with a non-exportable Android Keystore key.
- HTTP body logging only in debug builds.
- Requests without an instance fail instead of falling back to mastodon.social.

### Under the hood
- Retrofit 3, OkHttp 5, Kotlin 2.4, Compose BOM 2026.06, Room schema 7 with tested migrations, R8 rules for release builds, many dependency updates.

## [1.2.3-alpha] - 2026-01-27

First public alpha.

[1.3.1]: https://github.com/pfitzer/Tootsie/compare/v1.3.0...v1.3.1
[1.3.0]: https://github.com/pfitzer/Tootsie/compare/1.2.3-alpha...v1.3.0
[1.2.3-alpha]: https://github.com/pfitzer/Tootsie/releases/tag/1.2.3-alpha
