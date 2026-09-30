# Validation

GitHub Actions compiled the full Android app and passed all eight JUnit tests. The first lint pass identified one incorrect literal Typeface constant; it was replaced with Typeface.NORMAL. Device-transfer backup exclusions were added. Final lint and Android 15 emulator smoke checks are run by the current workflow.

Live account upload, publication and in-game launch require an account and an installed game and remain unverified in CI.
