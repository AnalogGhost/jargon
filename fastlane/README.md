fastlane documentation
----

# Installation

Make sure you have the latest version of the Xcode command line tools installed:

```sh
xcode-select --install
```

For _fastlane_ installation instructions, see [Installing _fastlane_](https://docs.fastlane.tools/#installing-fastlane)

# Available Actions

## Android

### android playstore

```sh
[bundle exec] fastlane android playstore
```

Build the Play release AAB and upload it plus store listing metadata to Google Play

Defaults to a dry run (validate_only) against the internal track — pass track:production

and validate_only:false to actually publish.

### android metadata

```sh
[bundle exec] fastlane android metadata
```

Upload only the store-listing text (titles/descriptions) to Google Play — no build,

no AAB, no images, no changelogs. For listing-copy fixes between releases.

Defaults to a dry run; pass validate_only:false to publish.

----

This README.md is auto-generated and will be re-generated every time [_fastlane_](https://fastlane.tools) is run.

More information about _fastlane_ can be found on [fastlane.tools](https://fastlane.tools).

The documentation of _fastlane_ can be found on [docs.fastlane.tools](https://docs.fastlane.tools).
