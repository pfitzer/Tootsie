# Security Policy

## Supported versions

Security fixes are made for the latest release only. Please update to the newest version from the Play Store or the [GitHub releases](https://github.com/pfitzer/Tootsie/releases) before reporting.

## Reporting a vulnerability

Please **do not open a public issue** for security problems.

Report them privately through GitHub: **Security → Report a vulnerability** in this repository ([direct link](https://github.com/pfitzer/Tootsie/security/advisories/new)). Include:

- what is affected (version, Android version, Mastodon server version if relevant)
- steps to reproduce or a proof of concept
- the impact you expect (e.g. leaked access token, account takeover, data exposure)

You can expect a first answer within a week. Once a fix is released, the advisory is published and you are credited, unless you prefer otherwise.

## Scope

In scope are the Tootsie app and this repository, for example the storage of access tokens, the OAuth login, push notification decryption, and the handling of content from Mastodon servers (HTML, links, media).

Not in scope are vulnerabilities in Mastodon servers or in third-party apps (UnifiedPush distributors, translator apps); please report those to their maintainers.
