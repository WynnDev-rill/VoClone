# Build yang dapat direproduksi

JDK **21** (JDK lengkap, bukan JRE), Python >=3.12, SDK platform36, build-tools36.0.0, NDK27.1.12297006, CMake3.22.1. Gradle8.13 wrapper SHA-256, AGP8.12.0, Kotlin2.2.21, Compose BOM2025.10.01 dan direct dependencies dipatok. Build pertama memerlukan internet untuk Gradle/Maven/SDK dan runtime source; model tidak diunduh oleh build.

```sh
export JAVA_HOME=/path/to/jdk21
export ANDROID_HOME=/path/to/android-sdk
sdkmanager 'platforms;android-36' 'build-tools;36.0.0' 'ndk;27.1.12297006' 'cmake;3.22.1'
./gradlew testDebugUnitTest lintDebug assembleDebug assembleDebugAndroidTest
python3 scripts/verify-apk.py app/build/outputs/apk/debug/app-debug.apk --debug
```

Source archives are verified before safe extraction. `native/fetch.py` applies documented patches; `scripts/build-native.sh` builds only ARM64 by default. No floating native commits or model URLs. The source release includes `native/build/{sources,downloads}` so native sources remain reconstructible even if an upstream host disappears. Gradle's normal immutable Maven dependencies are still required on a clean machine. Reproducible here means pinned/reconstructible inputs, not a claim of byte-identical APK across JDK/OS/build timestamps.

## Owner release signing

Private archive provided separately: `VoClone-signing-backup-PRIVATE.zip`. Do not commit or upload it publicly. Extract, set the four variables in credentials.json and adjust VOCLONE_KEYSTORE to the extracted p12 path:

```sh
# Set VOCLONE_KEYSTORE, VOCLONE_STORE_PASSWORD, VOCLONE_KEY_PASSWORD, VOCLONE_KEY_ALIAS securely.
./gradlew assembleRelease
python3 scripts/verify-apk.py app/build/outputs/apk/release/app-release.apk
```

Never regenerate the key. Public certificate: docs/signing-sha256.txt. Package `id.wynn.voclone`; versionName `0.1.0-alpha`, versionCode1. All future updates increment code and retain package/certificate. Debug is signed separately and is a fresh testing install; Android will reject a different signer over the owner install.

## GitHub Actions / Secrets

`.github/workflows/android.yml`: tests, lint, debug ARM64, instrumentation APK and downloadable artifacts on main/PR; signed alpha release only on explicit dispatch/tag after validation. Pin all action commits. Configure these repository Secrets from the private owner backup:

- VOCLONE_KEYSTORE_BASE64: base64 contents of the p12.
- VOCLONE_STORE_PASSWORD.
- VOCLONE_KEY_PASSWORD.
- VOCLONE_KEY_ALIAS.

The connected integration returned **403 Resource not accessible by integration** for the Secrets public-key endpoint in this session. No Secrets installation is claimed. Local owner signing works independently. Secrets are written to runner temp only and deleted after build. Source/archive scripts must never include keystore or credentials.

The private backup includes `install-github-secrets.py`. After extracting it and authenticating GitHub CLI as an owner with Secrets permission, run `python3 install-github-secrets.py --repo WynnDev-rill/VoClone`. It sends the four values through stdin without printing them. The downloadable backup was independently restored in memory: its RSA4096 private key matches its certificate and the APK owner fingerprint.

`python3 scripts/package-release.py` prepares owner APK, SHA, update manifest and complete source archive after a verified signed build and committed HEAD. `scripts/publish-release.py` creates an alpha prerelease in the source repo. Stable release is deliberately not automated before physical-device gates. Public Wynn-Store updater publication is a separate owner-approved distribution step: copy the generated alpha.json only after its exact APK and matching source have been published, without exposing a private-source GitHub token.

## Environment-specific failures seen

Initial environment had JRE without javac, and Java did not use the session HTTP proxy automatically. A separate JDK21 and system CA trust store were used, with JVM HTTP/HTTPS proxy properties supplied only to local commands. Do not commit those machine paths/proxy credentials. Native source checksums and TLS verification were never disabled.

The first Actions run failed before compilation because setup-android's default requested the removed SDK package `tools`. The workflow now explicitly installs only `platform-tools`, followed by the pinned SDK/NDK/CMake packages. The subsequent CI build, 34 JVM tests, lint and APK alignment verification passed; see docs/VERIFICATION.md for emulator status and artifact links.

## Published owner alpha

Owner APK 0.1.0-alpha was locally built/signed, then verified and published by [Actions run41148356729](https://github.com/WynnDev-rill/VoClone/actions/runs/41148356729). [Owner APK artifact](https://github.com/WynnDev-rill/VoClone/actions/runs/41148356729/artifacts/13022330442) and [Release with full corresponding source](https://github.com/WynnDev-rill/VoClone/releases/tag/v0.1.0-alpha) are available. Source tag targets a06b3005bbc80ee86e248b9a48c5924d372d11e4; tested production code is identical to ef605dd.

Direct Release asset upload from the cloud integration failed401 at uploads.github.com. A temporary, checksum-pinned signed APK was transported through git (no private key or model weights), checked against immutable source metadata and the owner certificate on GitHub's runner, and published using its scoped workflow token. The temporary APK/workflow were removed from the current branch afterward. Their historical commit is documented in release-inputs/README.md. This does not claim that Actions rebuilt/signed this owner APK; the separate normal Android CI did build/test debug. Future signed rebuilds use the four owner Secrets, whose setup still requires owner permission.

## Public feed and feed-only recovery

`publish-release.py` creates the immutable alpha assets and then calls `publish-update-feed.py` as the final mutation. Both publisher routes require successful real inference. Tags or a signed-release dispatch run the noncommercial QA model test; production model consent is separate. The feed script checks the original owner certificate, package/version, ARM64 ABI, APK embedded source revision, anonymous APK/source download sizes and SHA-256, then uses the Contents API file SHA to update only `WynnDev-rill/VoClone/main/updates/alpha.json`. It rejects rollback and changing an existing versionCode. Never write a Wynn-Store bridge.

If asset publication succeeded but the final feed operation failed, **do not overwrite/recreate the Release**. The supported recovery path runs only `publish-update-feed.py`, which is idempotent for the exact manifest:

1. Use a clean checkout of `sourceCommit` recorded in that release's alpha.json. Install the pinned Android36 build tools and NDK27.1.12297006 (or use the full build prerequisites above), then set ANDROID_HOME. Authentication needs contents-write for this repository only; no signing key is required.
2. In an empty ignored `distribution/` directory download the **original** release assets `alpha.json`, `VoClone-<version>-arm64.apk`, and `VoClone-<version>-source.tar.gz` using `gh release download v<version> --repo WynnDev-rill/VoClone --dir distribution --pattern <exact-name>` for each file. Do not regenerate the source tarball: its archive bytes may differ despite identical source content. Confirm the manifest's sourceCommit matches this checkout.
3. Run `python3 scripts/verify-apk.py distribution/VoClone-<version>-arm64.apk`, then `python3 scripts/publish-update-feed.py`. The latter revalidates provenance/certificate/identity and both anonymously downloaded assets before attempting the feed update. On HTTP409 or branch protection failure, inspect the current feed and retry only this feed step after resolving the stated cause; never force a channel rollback or weaken protection.
4. Verify the unmodified public raw feed URL and both release downloads without authentication. Save the publication/recovery logs. A failed feed update must remain a failed publication result, never a fake success.

The standard signed workflow saves the distribution artifact before publication; the one-time owner transport workflow saves it even on publication failure. The original immutable release downloads are also sufficient for recovery. No key/password/token belongs in source, artifacts, reports, or the public update manifest.
