# Releasing Veylo

The `github` flavor checks this repository's latest GitHub Release. The `foss` flavor has no installer.

## Signing setup

Create a permanent Android signing keystore and keep a private backup. Add these repository Actions secrets:

| Secret | Value |
| --- | --- |
| `ANDROID_KEYSTORE_BASE64` | Base64-encoded contents of the signing keystore |
| `STORE_PASSWORD` | Keystore password |
| `KEY_ALIAS` | Signing-key alias |
| `KEY_PASSWORD` | Signing-key password |

The workflow restores the key only during the release job and removes it afterward. It refuses to publish when the signing configuration is incomplete. Never commit the key or passwords.

## Publish

1. Test the changes on a device and run the checks.
2. Set the intended `versionName` and a larger `versionCode` in `app/build.gradle.kts`.
3. Commit the version change to `main`.
4. Create and push the corresponding tag, for example `v3.0.0` for `versionName = "3.0.0"`.

The tag must point to a commit on `main` and match the source version. The workflow builds signed release APKs, verifies their signatures, and publishes the assets with release notes and checksums.

The updater expects `Veylo.apk` for the universal build and `veylo-<ABI>.apk` for architecture-specific builds. Foss, nightly, and unrelated APKs are excluded from stable update selection. Android still asks the user to approve installation.

## Existing test installations

The earlier shared APK was a debug `foss` build with package ID `io.github.loyalty001.veylo.debug` and no in-app updater. The official release uses `io.github.loyalty001.veylo`. Install the official release once to enable future in-app updates. Export any data needed from the test installation first. Later official releases must retain the same application ID and signing key, and increase the version code.

The original Flow signing key is not used. Original developer credits, license notices, and the corresponding source code must remain available with distributed releases.
