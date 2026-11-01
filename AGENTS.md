## graphify

This project has a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships.

When the user types `/graphify`, use the installed graphify skill or instructions before doing anything else.

Rules:
- When graphify-out/graph.json exists, run `graphify query "<question>"` before your first grep, search or source-file read for a codebase question, and put this rule in every subagent prompt that explores code. Read raw files only for lines the graph or an earlier read already located. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- Dirty graphify-out/ files are expected after hooks or incremental updates; dirty graph files are not a reason to skip graphify. Only skip graphify if the task is about stale or incorrect graph output, or the user explicitly says not to use it.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
- A committed graphify-out/graph.json is rewritten whole on every build, so a large diff to it in a pull request is expected. Do not flag it as churn or suggest skipping the graph rebuild or its hooks.

# VoClone engineering instructions

## Goal and architecture
Native offline voice cloning on ARM64, especially POCO F6 / 12 GB / Android 16. One `app` module (Compose + persistent worker + local storage), `native` pinned JNI runtimes. Read PRODUCT.md, DESIGN.md, docs/ARCHITECTURE.md and docs/VERIFICATION.md before changing behavior. No WebView, paid server, analytics or hidden audio upload.

## Workflow
Asset-First: audit existing components and official licensing before adding dependencies. Verification-First: no fake audio, profiles, progress or success. Keep tested, untested and failed outcomes distinct. Kotlin: structured coroutines, IO/CPU off main, cancellation propagated, immutable UI state, private persistent files and atomic writes. Compose: Material 3 semantic colors, 48dp targets, scalable type, native Back and insets, no bottom navigation.

Ponytail is already provided by Engineering Suite in the current environment; do not reinstall a duplicate. Default full; ultra for architecture/audits/optimization. Official portable instructions: https://github.com/DietrichGebert/ponytail/blob/main/skills/ponytail/SKILL.md . If missing in a future environment use official INSTALL.md. Do not trade validation/security/tests for brevity.
UI UX Pro Max is installed project-scoped at `.agents/skills/ui-ux-pro-max` and owns design direction; Impeccable at `.agents/skills/impeccable` owns critique/a11y/polish. User's explicit reference and autonomous-work instruction prevail over generic interviews/approval gates. Read local skills before using. Hooks are installed but runtime activation is environment-dependent; run the detector explicitly when hooks are unavailable. Graphify is installed in `.codex/skills/graphify`; see its section above. Rebuild after architectural changes; never trust a stale graph. Pinned tool provenance: docs/SKILLS.md.

## Safety, licensing and completion
Application and adapted VoiceStudio JNI code remain AGPL-3.0; maintain notices and provide corresponding source with APK distribution. Runtime source licences differ from weight/tokenizer licences; model acceptance is fingerprinted, no weights bundled. Voice consent required. Never log voice/transcripts or commit signing keys/passwords/tokens. Never regenerate the release key: use GitHub Secrets or owner's original backup; public fingerprint in docs/signing-sha256.txt. Increment versionCode for all updates. Release gate: build/tests/lint, real E2E WAV, hardware playback/quality/background/cancel/download/upgrade evidence. Alpha releases must disclose missing device evidence. No hardware results may be inferred from host execution.

## Build and debug
JDK21, SDK36, NDK27.1.12297006, CMake3.22.1. `./gradlew testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest`. Native sources are fetched with verified SHA-256 via scripts; no floating revisions. Debug root causes with smallest reproducible input, worker status and sanitized error; no blanket exception-to-success handling. See docs/DEVICE_TEST.md for actual-device evidence. Never report compile success as cloning success.

## v0.2 contracts
Generate Speech has no artificial total-character cap. Full input is an immutable private text file, never a large WorkManager Data string. SpeechChunks streams <=280 Unicode code points per native request; keep the native per-request resource guard. Stream WAV metadata/PCM/assembly/waveforms/MP3 for long outputs; never restore whole-file FloatArray loading on the result path. Respect RIFF's real unsigned32-bit size limit and Android/storage quotas. Verified per-worker parts may be reused for system retry; explicit cancellation/terminal failures and stale work storage must be reclaimed. Propagate cancellation through scans, hashing and queued preparation as well as native inference.

Progress percentages are **stage** percentages from real completed native iterations, not a wall-clock ETA or simulated overall completion. Show text part counts separately. Model loading stays indeterminate until an actual callback exists. Preserve numeric-only JNI progress and private typed diagnostics; never add speech/transcripts/names/private paths or hidden upload. Optional instrumentation accepts model terms only through explicit QA flags and must restore connectivity/cancel/clean its own resources in finally.

The public release/feed destination is only WynnDev-rill/VoClone. Owner cancelled Wynn-Store publication. UPDATE_URL, allowlist and generated release metadata must stay consistent; publish updates/alpha.json only after its exact APK/source assets are anonymously downloadable. v0.1 needs one manual same-signer bootstrap. Preserve the automatic-check toggle and perform a new-build/feed check despite the old cooldown. Original owner signer remains docs/signing-sha256.txt; never regenerate it or publish private backup. If Secrets API access is unavailable, an already signed public APK may be transported for runner-side publication without transporting the private key; document that Actions did not sign/rebuild that owner APK and remove temporary transport after success.

Final owner provenance: commit source before the release build; refresh `:app:extractReleaseVersionControlInfo` after the commit, then rebuild/sign. package-release.py requires the APK's embedded Git revision to equal clean HEAD, alongside its generated manifest/source commit. Keep final artifact hashes in generated release identity/manifest to avoid a self-referential hash inside its own source commit. QA-only `-I qa/android-x86.init.gradle` can run the real pipeline natively on an x86 Android emulator; it forbids release tasks. Production ARM64 ABI/build remains unchanged. Clearly distinguish QA x86 E2E, translated ARM64 partial/timeout evidence, QEMU math tests and physical ARM64 certification.

Never publish an owner-signed instrumentation APK. Debug instrumentation belongs on a disposable emulator/spare device; never delete owner app data to bypass signer mismatch. Owner upgrade rehearsal uses a disposable rooted emulator shell, not a root permission/test backdoor in the production APK.
