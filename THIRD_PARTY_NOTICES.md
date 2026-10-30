# Third-party notices and model terms

VoClone application: AGPL-3.0. Original LICENSE is retained. Selected Android JNI/audio code adapted from VoiceStudio (debpalash), revision `4d4cb0badf99c7518e3f4c69487df6e8bfeff43f`, AGPL-3.0. Corresponding source/build scripts/notices accompany owner APK releases. Source archive includes checked-in LAME and fetched, patched native runtime sources plus original verified archives.

Updater derived from WynnDev-rill/Wynn-Store ScreenGuard Pro, revision `70273a33f48c4ea8c30bd49546aa40efd5b5188c`, GPL-3.0. Original terms retained; combined application under AGPL-3.0. No Shizuku, monitoring or privileged components used.

Native runtime sources are pinned and checksum-verified in `native/pins.json`: omnivoice.cpp MIT; its GGML fork MIT; whisper.cpp + bundled GGML MIT. Notices packaged in `app/src/main/assets/licenses`. VoClone changes allocation, cancellation, CPU bounds, JNI safety and namespace only; see fetch/patch scripts and architecture.

LLVM libc++ shared runtime from NDK27.1: Apache-2.0 with LLVM exceptions and retained legacy libc++ notices, packaged separately.

LAME 3.100 encoder subset: LGPL-2.0-or-later, original COPYING, source.json, headers and config retained under `native/mp3/Lame`. Full release archive SHA-256 `ddfe36cab873794038ae2c1210557ad34857a4b6bdc515785d1da9e175b1da1e`. Adapted VoiceStudio Mp3Encoder wrapper remains AGPL. Rebuild/relink instructions are the regular native/app build; a developer may use their own debug key for a modified install. Original owner key is required only for updating an existing owner installation.

Plus Jakarta Sans: Google Fonts / SIL Open Font License, local bundled font, OFL packaged. App icon/reference image supplied by project owner. Selected Google Material round icons: Apache-2.0; exact source URLs and SHA-256 in docs/icon-sources.json. Only 15 vectors packaged; no full icon catalogue dependency. Kotlin, AndroidX Compose/Material3/WorkManager/Lifecycle/Core, kotlinx-coroutines/serialization and OkHttp: Apache-2.0; JUnit EPL-1.0; Robolectric MIT (test only). No test framework in release APK.

## Weights, tokenizer and model licence consent

Weights are NOT inside APK/source archive. Downloads are direct from converter/runtime authors' repositories at immutable revisions, with exact size/SHA. This is not a claim that a community GGUF conversion is an official upstream model release.

- OmniVoice base Q4_K_M: noncommercial upstream weight restriction; historical conversion card specifically CC-BY-NC-4.0. Newer conversion metadata says Apache-2.0, conflicting with original model card; do not silently treat it as commercial clearance. Credit Han Zhu/coauthors and Serveurperso conversion.
- OmniVoice tokenizer Q4_K_M: underlying Higgs Audio 2 / Boson and Meta Llama 3 terms apply independently of conversion metadata. Full agreements are packaged and visible. No claim all training/voice/distribution permissions are cleared. User acceptance includes actual displayed licence documents and asset hashes, not just a summary string.
- Whisper Tiny GGML: original OpenAI Whisper code/weights MIT; converted format from whisper.cpp author ggerganov. Runtime licence and original model licence included separately.

Required notice (quoted wording retained from upstream agreement):

Built with Higgs Materials licensed from Boson AI USA, Inc., Copyright Boson AI USA, Inc., All Rights Reserved and Meta Llama 3 licensed under the Meta Llama 3 Community License, Copyright Meta Platforms, Inc., All Right Reserved". based on Meta Llama 3

Meta Llama 3 is licensed under the Meta Llama 3 Community License, Copyright © Meta Platforms, Inc. All Rights Reserved.
Boson Higgs Audio 2 is licensed under the Boson Community License, Copyright © Boson AI USA, Inc. All Rights Reserved.

Use voices only when owned or permitted. Application licence does not override model/tokenizer terms or voice rights. Generated audio is marked synthetic. Source privacy is retained; public distribution requires both APK and corresponding source, never signing credentials.

Host QA sample: JFK speech fixture from pinned whisper.cpp samples; testing only, not an app voice/profile. Synthetic tone in codec instrumentation verifies the encoder/decoder only and is never an AI fallback.
