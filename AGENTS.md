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
