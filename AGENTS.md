# Repository guidelines

## Project structure

Jargon is an offline Android reader built with Kotlin and Jetpack Compose. Application code is under `app/src/main/kotlin/com/hackerapps/jargon/`, unit tests are under `app/src/test/`, and the bundled dictionary is `app/src/main/assets/jargon.json`. The source-data import tooling lives under `tools/`; store metadata and release automation live in `fastlane/`.

## Development commands

- `./gradlew assembleDebug` builds a debug APK.
- `./gradlew test` runs unit tests.
- `./gradlew assembleFossRelease` builds a local FOSS release artifact.
- `bash docker-build.sh` performs the reproducible F-Droid build.

Use JDK 21 and an Android SDK with API 36 and current build tools. Keep SDK paths and signing credentials in ignored local files.

## Coding and testing

- Preserve fully offline operation and the zero-permission design.
- Keep dictionary parsing and filtering in the data layer and Compose code in the UI layer.
- Treat `tools/jargon-source/` as pinned upstream source. Do not refresh it or regenerate `jargon.json` unless the task requires a content update.
- Run `./gradlew test` and the relevant assemble task before committing behavior or build changes.
- Do not commit keystores, credentials, APKs, AABs, or build output.

## Releases

Release and metadata commands can create tags or update external stores. Run them only when the user explicitly requests a release.
