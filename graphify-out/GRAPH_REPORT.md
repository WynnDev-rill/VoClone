# Graph Report - VoClone  (2026-11-01)

## Corpus Check
- 108 files · ~64,283 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 32 file(s) not represented in the graph (top: .xml 20, (none) 4, .sha256 2)

## Summary
- 802 nodes · 1312 edges · 61 communities (38 shown, 23 thin omitted)
- Extraction: 91% EXTRACTED · 9% INFERRED · 0% AMBIGUOUS · INFERRED: 112 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `0f72acd5`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- release
- Theme.kt
- host_api.cpp
- with
- Diagnostics.kt
- Stop
- emulator
- release
- host_test.py
- steps
- env
- UI_AUDIT.md
- NativeControl
- jobs
- speechChunks
- uuid
- progress.h
- VoClone performance and observable inference implementation plan
- TaskProgress.kt
- real-inference
- 2026-10-31-performance-ledger.md
- release-inputs/README.md
- test-arm-kernels.sh
- StudioScreen
- AppIcon
- host_progress.cpp
- README.md
- Verifikasi VoClone — 30 Oktober 2026
- Arsitektur VoClone
- test-owner-upgrade.sh
- Build yang dapat direproduksi
- POCO F6 / Android 16 / ARM64 acceptance
- VERIFICATION.md
- THIRD_PARTY_NOTICES.md
- Models.kt
- MainActivity.kt
- AppUpdater.kt
- .copy
- PcmAudio
- Control
- UpdateManifestTest
- StudioUiTest.kt
- Design System Master File
- Mp3Encoder.c
- VoClone
- VC_JNI
- VoClone engineering instructions
- OmniVoice GGUF
- OmniVoice GGUF
- build-native.sh
- DESIGN.md
- 2026-10-30-voclone.md

## God Nodes (most connected - your core abstractions)
1. `PcmAudio` - 25 edges
2. `with` - 20 edges
3. `Control` - 19 edges
4. `UpdateManifestTest` - 16 edges
5. `check()` - 13 edges
6. `ProfileStore` - 13 edges
7. `NativeControl` - 11 edges
8. `StreamingWavWriter` - 11 edges
9. `StudioScreen()` - 11 edges
10. `InferenceProgress` - 10 edges

## Surprising Connections (you probably didn't know these)
- `Kepemilikan pekerjaan` --references--> `TaskLease`  [INFERRED]
  docs/ARCHITECTURE.md → app/src/main/java/id/wynn/voclone/CloneWorker.kt
- `encodeMp3()` --calls--> `check()`  [INFERRED]
  app/src/main/java/id/wynn/voclone/AndroidAudio.kt → app/src/main/java/id/wynn/voclone/AppUpdater.kt
- `encodeMp3()` --calls--> `forEachWaveBlock()`  [INFERRED]
  app/src/main/java/id/wynn/voclone/AndroidAudio.kt → app/src/main/java/id/wynn/voclone/StreamingWav.kt
- `AudioPlayer()` --calls--> `AppIcon()`  [INFERRED]
  app/src/main/java/id/wynn/voclone/StudioScreen.kt → app/src/main/java/id/wynn/voclone/AppIcon.kt
- `ButtonIcon()` --calls--> `AppIcon()`  [INFERRED]
  app/src/main/java/id/wynn/voclone/StudioScreen.kt → app/src/main/java/id/wynn/voclone/AppIcon.kt

## Import Cycles
- None detected.

## Communities (61 total, 23 thin omitted)

### Community 0 - "release"
Cohesion: 0.13
Nodes (15): default, description, type, full_inference, release, on, pull_request, push (+7 more)

### Community 1 - "Theme.kt"
Cohesion: 0.09
Nodes (4): ProgressUiTest, cloneInput(), VoCloneTheme(), SpeechRequestTest

### Community 2 - "host_api.cpp"
Cohesion: 0.31
Nodes (3): main(), read(), wav()

### Community 3 - "with"
Cohesion: 0.12
Nodes (17): with, api-level, arch, cores, disable-animations, distribution, emulator-options, if-no-files-found (+9 more)

### Community 4 - "Diagnostics.kt"
Cohesion: 0.11
Nodes (9): DiagnosticStore, SupportReport, TaskDiagnostic, InferenceProgress, InferenceSnapshot, InferenceTrace, useI8mm(), DiagnosticStoreTest (+1 more)

### Community 5 - "Stop"
Cohesion: 0.50
Nodes (3): Stop, start, triggered

### Community 6 - "emulator"
Cohesion: 0.33
Nodes (5): concurrency, cancel-in-progress, group, name, permissions

### Community 7 - "release"
Cohesion: 0.33
Nodes (6): release, if, needs, permissions, runs-on, timeout-minutes

### Community 8 - "host_test.py"
Cohesion: 0.08
Nodes (6): fetch_sources(), patch(), main(), next_feed(), verify_public(), FeedTest

### Community 9 - "steps"
Cohesion: 0.32
Nodes (12): steps, steps, validate, steps, steps, if, name, run (+4 more)

### Community 10 - "env"
Cohesion: 0.33
Nodes (6): GH_TOKEN, KEYSTORE_BASE64, VOCLONE_KEY_ALIAS, VOCLONE_KEY_PASSWORD, VOCLONE_STORE_PASSWORD, env

### Community 14 - "NativeControl"
Cohesion: 0.13
Nodes (6): CpuChoice, CpuRuntime, FastNativeBridge, NativeBridge, NativeControl, VoiceEngine

### Community 15 - "jobs"
Cohesion: 0.25
Nodes (8): runs-on, timeout-minutes, needs, runs-on, timeout-minutes, jobs, arm-kernels, emulator

### Community 17 - "uuid"
Cohesion: 0.16
Nodes (6): prepareSpeechInput(), speechTextFile(), pruneSpeechFiles(), pruneSpeechStorage(), SpeechInputTest, SpeechStorageTest

### Community 20 - "progress.h"
Cohesion: 0.25
Nodes (5): parse_progress(), Progress, done, stage, total

### Community 21 - "VoClone performance and observable inference implementation plan"
Cohesion: 0.25
Nodes (7): Constraints, Review focus, Task1: bounded preview request (red/green), Task2: real native progress and local diagnostics (red/green), Task3: guarded ARM kernel variant, Task4: verification, update and distribution, VoClone performance and observable inference implementation plan

### Community 24 - "real-inference"
Cohesion: 0.29
Nodes (8): permissions, real-inference, actions, contents, if, permissions, runs-on, timeout-minutes

### Community 28 - "StudioScreen"
Cohesion: 0.44
Nodes (5): AudioPlayer(), ButtonIcon(), RecordButton(), StudioScreen(), VoiceRow()

### Community 31 - "host_progress.cpp"
Cohesion: 0.20
Nodes (4): Events, decoded, reference, steps

### Community 32 - "README.md"
Cohesion: 0.20
Nodes (7): Keputusan engine, Konflik lisensi yang benar-benar ditemukan, Riset Asset-First — 30 Oktober 2026, Standar pengalaman pengguna, Perbarui graph, Skill dan tooling proyek, VoClone

### Community 33 - "Verifikasi VoClone — 30 Oktober 2026"
Cohesion: 0.25
Nodes (8): Belum terbukti / batasan, Bukti yang sudah tersedia, Distribusi alpha yang tersedia, Final artifact provenance and publication, Final real Android pipeline evidence, GitHub Actions, Ukuran dan identitas yang dipatok, Verifikasi VoClone — 30 Oktober 2026

### Community 36 - "Arsitektur VoClone"
Cohesion: 0.29
Nodes (6): TaskLease, Arsitektur VoClone, Audio, Kepemilikan pekerjaan, Penyimpanan dan privasi, Updater

### Community 39 - "Build yang dapat direproduksi"
Cohesion: 0.29
Nodes (6): Build yang dapat direproduksi, Environment-specific failures seen, GitHub Actions / Secrets, Owner release signing, Public feed and feed-only recovery, Published owner alpha

### Community 41 - "POCO F6 / Android 16 / ARM64 acceptance"
Cohesion: 0.50
Nodes (3): Instrumentation commands, POCO F6 / Android 16 / ARM64 acceptance, Update 0.2 regression cases

### Community 42 - "VERIFICATION.md"
Cohesion: 0.25
Nodes (6): Real progress and CPU execution, Update distribution and security, Verification and reproduction, VoClone 0.2.0-alpha, Published 0.2.0-alpha — final artifact, Update 0.2.0-alpha — 1 November 2026

### Community 45 - "Models.kt"
Cohesion: 0.19
Nodes (5): ModelAsset, ModelCatalog, sha256(), VerifiedDownload, ModelStoreTest

### Community 67 - "MainActivity.kt"
Cohesion: 0.07
Nodes (7): DeviceTest, AndroidAudio, encodeMp3(), NativeMp3Bridge, VoiceRecorder, MainActivity, fileSha256()

### Community 74 - "AppUpdater.kt"
Cohesion: 0.07
Nodes (6): WorkerInferenceTest, inference(), privateReference(), publish(), report(), VoCloneApp

### Community 82 - "PcmAudio"
Cohesion: 0.07
Nodes (11): check(), PcmAudio, ProfileStore, VoiceProfile, forEachWaveBlock(), StreamingWavWriter, WaveInfo, WaveSummary (+3 more)

### Community 83 - "Control"
Cohesion: 0.07
Nodes (18): Attached, detached, env, vm, Control, check, failed, owner (+10 more)

### Community 89 - "UpdateManifestTest"
Cohesion: 0.16
Nodes (3): State, UpdateManifest, UpdateManifestTest

### Community 94 - "Design System Master File"
Cohesion: 0.12
Nodes (16): Additional Forbidden Patterns, Anti-Patterns (Do NOT Use), Buttons, Cards, Color Palette, Component Specs, Design System Master File, Global Rules (+8 more)

### Community 145 - "Mp3Encoder.c"
Cohesion: 0.11
Nodes (11): VSMp3, Java_id_wynn_voclone_NativeMp3Bridge_close(), Java_id_wynn_voclone_NativeMp3Bridge_create(), Java_id_wynn_voclone_NativeMp3Bridge_finish(), Java_id_wynn_voclone_NativeMp3Bridge_write(), quiet(), vs_mp3_create(), vs_mp3_finish() (+3 more)

### Community 158 - "VoClone"
Cohesion: 0.18
Nodes (10): Accessibility & Inclusion, Brand Commitments, Capabilities and Constraints, Evidence on Hand, Platform, Product Principles, Product Purpose, Stack (+2 more)

### Community 180 - "VC_JNI"
Cohesion: 0.22
Nodes (5): Java_id_wynn_voclone_NativeBridge_hasI8mm(), VC_JNI(), quant_matmul_case(), quant_matmul_selftest(), main()

### Community 192 - "VoClone engineering instructions"
Cohesion: 0.25
Nodes (7): Build and debug, Goal and architecture, graphify, Safety, licensing and completion, v0.2 contracts, VoClone engineering instructions, Workflow

### Community 204 - "OmniVoice GGUF"
Cohesion: 0.29
Nodes (6): Backends, Files, License, OmniVoice GGUF, Quantization policy, Quick start

### Community 205 - "OmniVoice GGUF"
Cohesion: 0.29
Nodes (6): Backends, Files, License, OmniVoice GGUF, Quantization policy, Quick start

## Knowledge Gaps
- **21 isolated node(s):** `storage`, `chars`, `owner`, `vm`, `value` (+16 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 312 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **23 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `TaskLease` connect `Arsitektur VoClone` to `AppUpdater.kt`?**
  _High betweenness centrality (0.097) - this node is a cross-community bridge._
- **Are the 11 inferred relationships involving `PcmAudio` (e.g. with `.decodesRealWavAndEncodedMp3()` and `.rejectsNonFiniteOutput()`) actually correct?**
  _`PcmAudio` has 11 INFERRED edges - model-reasoned connections that need verification._
- **What connects `storage`, `chars`, `owner` to the rest of the system?**
  _21 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `release` be split into smaller, more focused modules?**
  _Cohesion score 0.13333333333333333 - nodes in this community are weakly interconnected._
- **Should `Theme.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.08901515151515152 - nodes in this community are weakly interconnected._
- **Should `with` be split into smaller, more focused modules?**
  _Cohesion score 0.11764705882352941 - nodes in this community are weakly interconnected._
- **Should `Diagnostics.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.1111111111111111 - nodes in this community are weakly interconnected._