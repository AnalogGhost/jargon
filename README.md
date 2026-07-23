# Jargon

A fully offline Android reader for the Jargon File — the hacker culture dictionary that gave the word "hacker" its original meaning.

[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![CI](https://github.com/AnalogGhost/jargon/actions/workflows/ci.yml/badge.svg)](https://github.com/AnalogGhost/jargon/actions/workflows/ci.yml)

## Features

- Browse 2,300+ entries alphabetically, with a fast-scroll index
- Instant search across terms and definitions
- Tap any cross-referenced term to jump straight to it
- Favorite entries and filter down to just those
- "Surprise me" with a random entry
- Zero permissions, zero network access — the dictionary ships inside the app; nothing ever leaves the device

## Building

### Requirements

- JDK 21 (install via [SDKMAN](https://sdkman.io): `sdk install java 21.0.5-tem`)
- Android SDK with platform API 36 and build-tools 36+ (install via Android Studio or `sdkmanager`)

### Clone and build (debug)

```bash
git clone https://github.com/AnalogGhost/jargon.git
cd jargon
echo "sdk.dir=$HOME/Android/Sdk" > local.properties
./gradlew assembleDebug
```

APK: `app/build/outputs/apk/debug/app-debug.apk`

### Run unit tests

```bash
./gradlew test
```

### Install on device via ADB

```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## Release builds

### Reproducible build (for F-Droid submission)

Use the Docker build script to produce an unsigned APK in the same environment F-Droid uses:

```bash
bash docker-build.sh
```

APK: `app/build/outputs/apk/release/app-release-unsigned.apk`

F-Droid builds from source and applies its own signature. Build from the tagged commit before making any further commits.

CI runs this script twice on every push and fails if the two builds don't produce byte-identical output — see [`.github/workflows/ci.yml`](.github/workflows/ci.yml).

### Plain build (for local testing, not submission)

```bash
./gradlew assembleRelease
```

Produces the same unsigned, minified APK, but from your host toolchain rather than F-Droid's exact build environment — fine for a quick smoke test, not for verifying reproducibility. See [FDROID_PUBLISHING.md](FDROID_PUBLISHING.md) for the full submission process.

## Dictionary content

Entries are parsed from the actively-maintained [Jargon File community edition](https://github.com/agiacalone/jargonfile) (a continuation of the file originally compiled by Eric S. Raymond and Guy L. Steele), licensed CC BY-SA 4.0. The upstream source is pinned in `tools/jargon-source/` and parsed into `app/src/main/assets/jargon.json` by `tools/build_jargon_json.py` — nothing is fetched over the network at build time. See `tools/jargon-source/SOURCE.md` for the pinned commit and how to refresh it.

App source code is GPL-3.0; dictionary content stays CC BY-SA 4.0. Both are credited in-app on the About screen.

## Project structure

```
app/src/main/kotlin/com/hackerapps/jargon/
├── data/
│   ├── DictionaryEntry.kt         # Entry model
│   ├── DictionaryRepository.kt    # Loads jargon.json from assets
│   ├── FavoritesRepository.kt     # DataStore-backed favorite ids
│   └── EntryFilters.kt            # Search / favorites-filter / term lookup
└── ui/
    ├── MainActivity.kt
    ├── JargonNavHost.kt
    ├── JargonViewModel.kt
    ├── EntryListScreen.kt         # Browse, search, alphabet scrubber
    ├── EntryDetailScreen.kt       # Definition, favorite toggle
    ├── LinkedText.kt              # Tappable cross-reference rendering
    ├── AboutScreen.kt
    └── theme/

tools/
├── build_jargon_json.py           # Upstream DocBook XML -> jargon.json
└── jargon-source/                 # Pinned upstream source + its license
```

## License

Copyright (C) 2026 Contributors

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

See [LICENSE](LICENSE) for the full text.
