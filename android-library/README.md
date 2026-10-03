# Script Library — Android

Native Android 10+ application for Null’s Brawl scripts. No advertising SDK. Optional account synchronization and usage statistics use a Supabase backend. The app talks directly to `https://scripting.nulls.gg/api` and reads the author registry from the existing `android-script-library/list.txt` in this repository.

## Build and install

Install the signed `Script-Library.apk` from [Releases](https://github.com/segnpa66-lab/nb-scripts-authors/releases). At launch the app checks the latest release with a five-second timeout. When a newer version exists, it downloads the release APK, verifies the GitHub SHA-256 digest and opens Android's package installer. Android requires user confirmation; it cannot silently install. Older debug builds use a different signing certificate and require one uninstall/reinstall before stable release updates can work.

Build locally with JDK 17, Android SDK 35 and Gradle 8.9: `gradle testDebugUnitTest lintDebug assembleDebug` inside `android-library/`. The CI release job runs on `v*` tags. It needs `RELEASE_KEYSTORE_BASE64`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, and `RELEASE_KEY_PASSWORD` repository secrets. Back up the matching keystore privately: losing it prevents in-place updates to installed release APKs. Generated APKs and ZIPs belong in Actions or Releases, not Git.

## Features

- Public scripts from the remotely maintained author list, random/new/old/alphabetical/recently updated order. Guest browsing and battle setup work without signing in when the service permits the author's script to launch.
- Partial search in titles, authors, descriptions, UUIDs and available indexed code. Code indexing is explicit, cancellable and limited to 24 MiB in memory; failures and coverage are shown. Unindexed/inaccessible code is not searched.
- Public author profiles, script details, sharing and Android deep links. Favourites require an account.
- Authenticated code viewing and downloads through `/scripts/{uuid}/content`, never through share tokens. Downloads use Android MediaStore at `/sdcard/Download/script_library/`.
- Create/import/edit/name/description, explicit publication and confirmed deletion of own scripts. Creation and metadata updates are separate API calls, so a partially completed operation may leave a draft which remains editable in My scripts.
- All 32 currently verified battle settings, persisted per script. A fresh public share token is requested for each game launch, using the official `params:v2` Base62 room format. A script whose author has no Connect configuration cannot launch. Unknown additional parameter IDs are reported and linked to the official site rather than silently omitted.
- Lua syntax highlighting, light and dark themes, and 15 bundled languages selected from the device or in app settings.
- Optional AES-GCM credentials encrypted by Android Keystore; 401 triggers one re-login, 403 is never bypassed. Without Remember, credentials stay in memory only. Logout removes account catalogue and code; downloaded files remain under the user’s control.

## Contract and limits

Endpoints and room format were checked against the service’s public production frontend (AuthContext, scripts, ScriptPage, ScriptEditorPage and api assets on 2026-09-30). This is an independent client, not an official Null’s app. Service/API changes can require an app update. Accounts using an unsupported login method can manage their account via the official website. Browsing and script operations use the existing service directly; optional synchronization and usage statistics require the Supabase backend. An internet connection remains necessary.

Live-account upload/publication and actual in-game launch require an account or installed game and cannot be fully verified in CI. Android automatic link verification also depends on the website publishing a matching Digital Asset Links file. Automated tests cover room encoding, battle values, public-data redaction, search, author-list parsing and sort semantics. CI normally runs Android lint and compiles the APK; commits marked `[skip checks]` compile without running tests or lint.

Profiles contain no email, contact details or moderation information. The client only queries the explicit author registry and the signed-in user's own resources. It does not enumerate users or probe undocumented endpoints.

## Usage statistics

Usage reporting is enabled by default and can be disabled with “Send usage statistics” in settings. Reports contain a random installation identifier, platform (`android`), app version and activity dates. When signed in, the server stores a keyed hash of the account UUID to combine activity across devices. These are pseudonymous statistics, not fully anonymous data. Passwords and script contents are not stored in the analytics tables. Account sessions are validated separately for authenticated requests.

The owner can view first launches, active installations/accounts and sign-ins. Clearing application data creates a new installation identifier; signed-in activity is deduplicated by account hash. GitHub APK download counts are shown separately and include repeat downloads; they do not measure installations. Disabling reporting stops future reports, but does not remove previously recorded statistics.
