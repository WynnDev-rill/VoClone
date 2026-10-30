# Graph Report - VoClone  (2026-10-30)

## Corpus Check
- 68 files · ~51,926 words
- Verdict: corpus is large enough that graph structure adds value.
- Unclassified: 29 file(s) not represented in the graph (top: .xml 20, (none) 4, .pro 1)

## Summary
- 575 nodes · 883 edges · 40 communities (25 shown, 15 thin omitted)
- Extraction: 93% EXTRACTED · 7% INFERRED · 0% AMBIGUOUS · INFERRED: 60 edges (avg confidence: 0.85)
- Token cost: 0 input · 0 output

## Graph Freshness
- Built from commit: `75a8f101`
- Run `git rev-parse HEAD` and compare to check if the graph is stale.
- Run `graphify update .` after code changes (no API cost).

## Community Hubs (Navigation)
- release
- Theme.kt
- host_api.cpp
- with
- Attached
- Stop
- emulator
- release
- host_test.py
- steps
- env
- StudioScreen.kt
- UI_AUDIT.md
- NativeControl
- emulator
- Models.kt
- AndroidAudio.kt
- CloneWorker.kt
- .copy
- ProfileStore
- Control
- UpdateManifestTest
- NativeControl
- Design System Master File
- Mp3Encoder.c
- VoClone
- control.h
- Java_id_wynn_voclone_NativeBridge_transcribe
- README.md
- OmniVoice GGUF
- OmniVoice GGUF
- Java_id_wynn_voclone_NativeBridge_cloneAudio
- build-native.sh
- DESIGN.md
- 2026-10-30-voclone.md

## God Nodes (most connected - your core abstractions)
1. `PcmAudio` - 21 edges
2. `with` - 19 edges
3. `Control` - 17 edges
4. `UpdateManifestTest` - 16 edges
5. `ProfileStore` - 13 edges
6. `StudioScreen()` - 11 edges
7. `check()` - 10 edges
8. `VoClone` - 10 edges
9. `AppIcon()` - 9 edges
10. `NativeControl` - 9 edges

## Surprising Connections (you probably didn't know these)
- `Kepemilikan pekerjaan` --references--> `CloneWorker`  [INFERRED]
  docs/ARCHITECTURE.md → app/src/main/java/id/wynn/voclone/CloneWorker.kt
- `Kepemilikan pekerjaan` --references--> `TaskLease`  [INFERRED]
  docs/ARCHITECTURE.md → app/src/main/java/id/wynn/voclone/CloneWorker.kt
- `encodeMp3()` --calls--> `check()`  [INFERRED]
  app/src/main/java/id/wynn/voclone/AndroidAudio.kt → app/src/main/java/id/wynn/voclone/AppUpdater.kt
- `Java_id_wynn_voclone_NativeBridge_cloneAudio()` --calls--> `fail()`  [INFERRED]
  native/omni.cpp → native/control.h
- `Java_id_wynn_voclone_NativeBridge_transcribe()` --calls--> `fail()`  [INFERRED]
  native/whisper.cpp → native/control.h

## Import Cycles
- None detected.

## Communities (40 total, 15 thin omitted)

### Community 0 - "release"
Cohesion: 0.18
Nodes (11): release, on, pull_request, push, workflow_dispatch, branches, tags, default (+3 more)

### Community 2 - "host_api.cpp"
Cohesion: 0.19
Nodes (3): main(), read(), wav()

### Community 3 - "with"
Cohesion: 0.12
Nodes (17): with, api-level, arch, cores, disable-animations, distribution, emulator-options, if-no-files-found (+9 more)

### Community 4 - "Attached"
Cohesion: 0.40
Nodes (4): Attached, detached, env, vm

### Community 5 - "Stop"
Cohesion: 0.50
Nodes (3): Stop, start, triggered

### Community 6 - "emulator"
Cohesion: 0.22
Nodes (8): concurrency, cancel-in-progress, group, jobs, validate, name, runs-on, timeout-minutes

### Community 7 - "release"
Cohesion: 0.25
Nodes (8): release, permissions, contents, if, needs, permissions, runs-on, timeout-minutes

### Community 9 - "steps"
Cohesion: 0.52
Nodes (7): steps, steps, if, name, run, uses, steps

### Community 10 - "env"
Cohesion: 0.33
Nodes (6): GH_TOKEN, KEYSTORE_BASE64, VOCLONE_KEY_ALIAS, VOCLONE_KEY_PASSWORD, VOCLONE_STORE_PASSWORD, env

### Community 12 - "StudioScreen.kt"
Cohesion: 0.07
Nodes (9): AppIcon(), LegalPage(), SectionHeading(), SettingsScreen(), AudioPlayer(), ButtonIcon(), RecordButton(), StudioScreen() (+1 more)

### Community 14 - "NativeControl"
Cohesion: 0.22
Nodes (3): NativeBridge, NativeControl, VoiceEngine

### Community 15 - "emulator"
Cohesion: 0.33
Nodes (6): needs, permissions, runs-on, timeout-minutes, emulator, actions

### Community 45 - "Models.kt"
Cohesion: 0.18
Nodes (5): ModelAsset, ModelCatalog, sha256(), VerifiedDownload, ModelStoreTest

### Community 67 - "AndroidAudio.kt"
Cohesion: 0.07
Nodes (5): AndroidAudio, encodeMp3(), NativeMp3Bridge, VoiceRecorder, MainActivity

### Community 74 - "CloneWorker.kt"
Cohesion: 0.05
Nodes (8): check(), State, CloneWorker, privateReference(), VoCloneApp, ProfileStore, VoiceProfile, UpdateManifest

### Community 82 - "ProfileStore"
Cohesion: 0.16
Nodes (3): PcmAudio, AudioTest, ProfileStoreTest

### Community 83 - "Control"
Cohesion: 0.19
Nodes (7): Control, check, failed, owner, report, value, vm

### Community 93 - "NativeControl"
Cohesion: 0.12
Nodes (3): DeviceTest, StudioUiTest, fileSha256()

### Community 94 - "Design System Master File"
Cohesion: 0.12
Nodes (16): Additional Forbidden Patterns, Anti-Patterns (Do NOT Use), Buttons, Cards, Color Palette, Component Specs, Design System Master File, Global Rules (+8 more)

### Community 145 - "Mp3Encoder.c"
Cohesion: 0.12
Nodes (10): VSMp3, Java_id_wynn_voclone_NativeMp3Bridge_close(), Java_id_wynn_voclone_NativeMp3Bridge_create(), Java_id_wynn_voclone_NativeMp3Bridge_finish(), Java_id_wynn_voclone_NativeMp3Bridge_write(), quiet(), vs_mp3_create(), vs_mp3_finish() (+2 more)

### Community 158 - "VoClone"
Cohesion: 0.18
Nodes (10): Accessibility & Inclusion, Brand Commitments, Capabilities and Constraints, Evidence on Hand, Platform, Product Principles, Product Purpose, Stack (+2 more)

### Community 172 - "control.h"
Cohesion: 0.21
Nodes (5): fail(), oom(), Utf8, chars, storage

### Community 192 - "README.md"
Cohesion: 0.05
Nodes (32): Build and debug, Goal and architecture, graphify, Safety, licensing and completion, VoClone engineering instructions, Workflow, TaskLease, Arsitektur VoClone (+24 more)

### Community 204 - "OmniVoice GGUF"
Cohesion: 0.29
Nodes (6): Backends, Files, License, OmniVoice GGUF, Quantization policy, Quick start

### Community 205 - "OmniVoice GGUF"
Cohesion: 0.29
Nodes (6): Backends, Files, License, OmniVoice GGUF, Quantization policy, Quick start

## Knowledge Gaps
- **14 isolated node(s):** `storage`, `chars`, `owner`, `vm`, `value` (+9 more)
  These have ≤1 connection - possible missing edges or undocumented components. (Counts symbols only; 254 node(s) total have ≤1 connection when file, concept and rationale nodes are included.)
- **15 thin communities (<3 nodes) omitted from report** — run `graphify query` to explore isolated nodes.

## Suggested Questions
_Questions this graph is uniquely positioned to answer:_

- **Why does `Kepemilikan pekerjaan` connect `README.md` to `CloneWorker.kt`?**
  _High betweenness centrality (0.097) - this node is a cross-community bridge._
- **Are the 8 inferred relationships involving `PcmAudio` (e.g. with `.decodesRealWavAndEncodedMp3()` and `.rejectsNonFiniteOutput()`) actually correct?**
  _`PcmAudio` has 8 INFERRED edges - model-reasoned connections that need verification._
- **What connects `storage`, `chars`, `owner` to the rest of the system?**
  _14 weakly-connected nodes found - possible documentation gaps or missing edges._
- **Should `with` be split into smaller, more focused modules?**
  _Cohesion score 0.11764705882352941 - nodes in this community are weakly interconnected._
- **Why does `CloneWorker` connect `CloneWorker.kt` to `README.md`?**
  _High betweenness centrality (0.072) - this node is a cross-community bridge._
- **Should `host_test.py` be split into smaller, more focused modules?**
  _Cohesion score 0.1028225806451613 - nodes in this community are weakly interconnected._
- **Should `StudioScreen.kt` be split into smaller, more focused modules?**
  _Cohesion score 0.06636500754147813 - nodes in this community are weakly interconnected._