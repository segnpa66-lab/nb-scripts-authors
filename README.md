# Script Library

`list.txt` is the author registry. Keep one author profile URL per line. Both the Android app and the site read the current `android-script-library` branch; removing a line removes that author on the next successful refresh.

The Android source is in [`android-library/`](android-library/). Its build instructions and release requirements are in [`android-library/README.md`](android-library/README.md). The web source is maintained separately in a private repository; the public Pages repository contains the built site.

APK builds run in [Actions](https://github.com/segnpa66-lab/nb-scripts-authors/actions). Signed installable APKs are published in [Releases](https://github.com/segnpa66-lab/nb-scripts-authors/releases). Build artifacts are not committed to this repository.
