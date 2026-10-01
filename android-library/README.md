# Script Library — Android

Native Android 10+ application for Null’s Brawl scripts. No application backend, advertising SDK or analytics. The app talks directly to `https://scripting.nulls.gg/api` and reads the author registry from the existing `android-script-library/list.txt` in this repository.

## Build and install

Open **Actions → Android APK**, download **Script-Library-APK**, unzip and install `app-debug.apk`. This first build is signed with the standard development key; a stable release signing key is needed before distributing future automatic APK updates. The app automatically refreshes its catalogue/session, not its APK binary. Build locally with JDK 17, Android SDK 35 and Gradle 8.9: `gradle testDebugUnitTest lintDebug assembleDebug` inside `android-library/`.

## Features

- Public scripts from the remotely maintained author list, random/new/old/alphabetical/recently updated order. Guest browsing and battle setup work without signing in when the service permits the author's script to launch.
- Partial search in titles, authors, descriptions, UUIDs and available indexed code. Code indexing is explicit, cancellable and limited to 24 MiB in memory; failures and coverage are shown. Unindexed/inaccessible code is not searched.
- Public author profiles, script details, sharing and Android deep links. Favourites require an account.
- Authenticated code viewing and downloads through `/scripts/{uuid}/content`, never through share tokens. Downloads use Android MediaStore at `/sdcard/Download/script_library/` and remove email addresses. Code sent to the editor/server is preserved as authored.
- Create/import/edit/name/description, explicit publication and confirmed deletion of own scripts. Creation and metadata updates are separate API calls, so a partially completed operation may leave a draft which remains editable in My scripts.
- All 32 currently verified battle settings, persisted per script. A fresh public share token is requested for each game launch, using the official `params:v2` Base62 room format. A script whose author has no Connect configuration cannot launch. Unknown additional parameter IDs are reported and linked to the official site rather than silently omitted.
- Lua syntax highlighting, light and dark themes, and 15 bundled languages selected from the device or in app settings.
- Optional AES-GCM credentials encrypted by Android Keystore; 401 triggers one re-login, 403 is never bypassed. Without Remember, credentials stay in memory only. Logout removes account catalogue and code; downloaded files remain under the user’s control.

## Contract and limits

Endpoints and room format were checked against the service’s public production frontend (AuthContext, scripts, ScriptPage, ScriptEditorPage and api assets on 2026-09-30). This is an independent client, not an official Null’s app. Service/API changes can require an app update. Accounts using an unsupported login method can manage their account via the official website. A separate server is not required, but an internet connection and the existing service remain necessary.

Live-account upload/publication and actual in-game launch require an account or installed game and cannot be fully verified in CI. Android automatic link verification also depends on the website publishing a matching Digital Asset Links file. Automated tests cover room encoding, battle values, public-data redaction, search, author-list parsing and sort semantics. CI also runs Android lint and compiles the APK.

Profiles contain no email, contact details or moderation information. The client only queries the explicit author registry and the signed-in user's own resources. It does not enumerate users or probe undocumented endpoints.
