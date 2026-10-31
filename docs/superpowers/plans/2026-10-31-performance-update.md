# VoClone performance and observable inference implementation plan

> **For agentic workers:** Use superpowers:executing-plans task by task. User has explicitly authorized autonomous implementation; native execution in this session, independent code review before release.

**Goal:** Short, genuine clone previews; observable/cancellable inference; safely selected existing ARM quantized kernels; signed update with honest device evidence.
**Architecture:** Preserve one app module, model catalogue, complete references and profile schema. Reuse pinned OmniVoice/GGML, build isolated baseline and I8MM JNI variants, and add local privacy-safe task diagnostics.
**Tech Stack:** Kotlin/Compose/WorkManager; C++17 JNI; existing GGML Q4_K/Q6_K kernels; no new models/dependencies.
**Spec:** PRODUCT.md, DESIGN.md, AGENTS.md, user request31October2026 and observed 8-minute POCO F6 clone.

## Constraints
- Package id.wynn.voclone; min29/target36; ARM64; same original owner signing key; increment versionCode to2.
- Full reference/transcript must remain unchanged. Profile saved only after real valid synthesized preview.
- No fake percent, fabricated ETA, voice/transcript logging, cloud inference, or silent retry of cancelled/failed native synthesis.
- Do not claim community/stable hardware acceptance without actual POCO quality/thermal/background/upgrade results. Deliver a tested alpha update if those gates remain unavailable.

## Review focus
- Japanese/mixed Unicode and blank input: short valid preview, original reference preserved.
- Old ARM CPUs and translated emulators: no I8MM load without runtime capability AND native primary ARM64.
- Cancellation during model load/reference/step/decode: no committed partial result, no dangling global callback.
- Long step/background/process restart: phase/elapsed displayed truthfully; persisted diagnostics remain bounded and private.
- Existing voices and signed upgrade: retain package/key/schema; never regenerate keys or require model redownload unless files are invalid.

## Task1: bounded preview request (red/green)
Files: new SpeechRequest.kt and SpeechRequestTest.kt; StudioViewModel.kt, StudioScreen.kt.
- [ ] Characterize cloneInput(referenceId,transcript,name,language):Data preservation, then test long JP preview stays short while full transcript survives. Observe regression failure against old full-transcript behavior.
- [ ] Implement localized short preview for explicit en/id/ja, kana-aware auto preview, bounded word/codepoint excerpt for remaining auto input. Integrate factory at actual clone request boundary.
- [ ] Run JVM suite. Show concise long-reference advice without cropping audio or misaligning transcript.

## Task2: real native progress and local diagnostics (red/green)
Files: native/patch_omni.py, native/omni.cpp, native/control.h, Engine.kt, CloneWorker.kt; new InferenceProgress.kt, TaskProgress.kt and tests; SettingsScreen.kt, StudioScreen.kt.
- [ ] Add failing checks for progress parser rejecting untrusted/log text and invalid counts; task snapshot tracks real phase/step only.
- [ ] Adapt pinned runtime numeric-only stage/MaskGIT callbacks through scoped log lease; public upstream ABI2 structs unchanged. No raw log contents leave native.
- [ ] Emit bounded WorkManager updates, actual phase counters, monotonic elapsed and last update time; preserve cancellation and cleanup.
- [ ] Save bounded local task summaries without audio/text/names/paths; add shareable support JSON in settings using existing FileProvider exports.
- [ ] Test progress validation, completed/failed/cancelled report, native cancellation and actual native callback ordering.

## Task3: guarded ARM kernel variant
Files: build-native.sh, native/CMakeLists.txt, new cpu_features.h/tests; omni.cpp, Engine.kt; verify-apk.py, Android device tests.
- [ ] Test capability gate with missing/present bits and primary ARM64 requirement before production route changes.
- [ ] Preserve generic libvc_omni; separately compile libvc_omni_i8mm with armv8-a+dotprod+i8mm and distinct FastNativeBridge exports. Same model, four-thread baseline, no simultaneous contexts.
- [ ] Assert symbol isolation and16KiB alignment for all libs. Never catch inference failure and silently rerun.
- [ ] Run real Q4_K/Q6_K matmul checks against scalar dequantized weight/Q8 activation reference, including odd/even/single-column shapes, on supported ARM or documented emulation. Record skipped hardware cases.

## Task4: verification, update and distribution
- [ ] Real native inference generates valid new WAV with progress/cancel; record actual timings/limits without estimating POCO latency from host.
- [ ] JVM, lint, signed release/debug/instrumentation build, Android16 light/dark/narrow UI and same-owner version2 update fixture; independent review fixes important findings.
- [ ] Update Graphify, architecture, research/provenance, verification/device checklist and source archive.
- [ ] Publish signed alpha through Actions and provide direct APK/source/support-report links; preserve private backup. Public Wynn-Store publication remains separate from readiness, with no private source disclosure by assumption.
