# Veylo

An Android video and music client based on [Flow Android Client](https://github.com/Flow-Tube/Flow), with Veylo branding and a teal theme.

## Features

- Video and music playback, background audio, and picture-in-picture
- Subscriptions, playlists, local history, and offline downloads
- On-device recommendations powered by FlowNeuroEngine
- In-app updates from this repository's GitHub Releases in the `github` build

## Development

Use JDK 21 and the Android SDK for API 37.

```sh
./gradlew :app:assembleGithubDebug
./gradlew ktlintCheck
./gradlew :app:testGithubDebugUnitTest --tests 'io.github.aedev.flow.data.update.*'
```

The `foss` flavor does not include the installer. Debug builds use a separate application ID and are intended for testing.

## Releases

The release workflow builds signed APKs from version tags. Configure the repository's Actions signing secrets before publishing a release. See [RELEASING.md](RELEASING.md) for the signing setup, version requirements, and asset names expected by the updater.

## Credits and license

Veylo is derived from Flow Android Client by A-EDev and contributors. FlowNeuroEngine is credited to the original project. Existing copyright notices are retained.

Licensed under [GNU GPL version 3](License). Distributed versions must include access to the corresponding source code under the same license.
