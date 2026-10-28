# Contributing to Tootsie

Thanks for your interest in Tootsie! Bug reports, ideas, translations and code are all welcome.

By taking part you agree to follow the [Code of Conduct](CODE_OF_CONDUCT.md).

## Reporting bugs and requesting features

- Search the [existing issues](https://github.com/pfitzer/Tootsie/issues) first, maybe it is already known.
- Use the **Bug report** or **Feature request** template when opening a new issue.
- For bugs, include the Tootsie version, your Android version and device, your Mastodon instance (and its version, if you know it) and the steps to reproduce.
- **Security vulnerabilities** must not be reported in a public issue. Use [private vulnerability reporting](https://github.com/pfitzer/Tootsie/security/advisories/new) instead.

## Development setup

Requirements and build commands are described in the [README](README.md#-requirements). In short:

- Android Studio with support for Android Gradle Plugin 9.4, JDK 21, Android SDK 37
- A device or emulator with Android 12 (API 31) or higher

```bash
./gradlew assembleDebug        # build
./gradlew testDebugUnitTest    # unit tests
./gradlew lintDebug            # lint (not part of CI, run it locally)
```

## Making changes

1. Fork the repository and create a branch from `main`, e.g. `feature/issue-42-short-description` or `fix/issue-42-short-description`.
2. Keep pull requests focused on one issue or topic.
3. Follow the architecture (Clean Architecture + MVVM, Hilt, Room as single source of truth) and the [critical development guidelines](README.md#%EF%B8%8F-critical-development-guidelines). The most important points:
   - All user-facing strings go into `res/values/strings.xml` **and** `res/values-de/strings.xml`. ViewModels expose messages as `UiText`, never as hardcoded strings.
   - Every Room entity change needs a version bump, a migration, the generated schema file in `app/schemas/` and a migration test in `TootsieDatabaseMigrationTest`.
   - Never use `runBlocking` or suspend functions in OkHttp interceptors; read DataStore once with `.first()`, on `Dispatchers.IO`.
4. Add or update unit tests for your change and make sure `./gradlew testDebugUnitTest` and `./gradlew lintDebug` pass.
5. Check accessibility: content descriptions for images and icons, touch targets of at least 48dp.

## Commit messages

Commits use a short type prefix and an imperative summary in English, referencing the issue where there is one:

```
feat: Scheduled posts (#21)
fix: Line separators rendered as empty boxes (#4)
docs: Child safety standards page
build: Raise vulnerable transitive dependencies
```

Common types: `feat`, `fix`, `docs`, `build`, `deps`, `refactor`, `test`, `release`.

## Pull requests

- Open the pull request against `main` and describe what changed and why; link the issue (`Fixes #42`).
- Add screenshots for UI changes (light and dark mode, if relevant).
- The unit tests run automatically for pull requests that are not drafts. Open a draft while you are still working on it.

## Translations

Tootsie is available in English and German. To improve a translation or add a new language, add or edit `app/src/main/res/values-<language>/strings.xml` and keep the placeholders (`%1$s`, `%1$d`) intact. A partial translation is fine, missing strings fall back to English.

## License

By contributing, you agree that your contributions are licensed under the [MIT License](LICENSE).
