# VoClone 0.2.0-alpha

The 600-character overall Generate Speech limit is removed. The complete text is written atomically in private storage, because WorkManager Data is limited to 10 KiB. A Unicode-safe sentence/whitespace chunker sends at most 280 code points (560 UTF-16 units) to each native request. Each part uses the same complete reference/transcript. Changes in intonation across part boundaries remain possible and require listening tests. No new model or lower synthesis iteration count is introduced.

Long results use bounded PCM reads/writes for assembly, waveform/RMS validation and MP3 export. RIFF WAV has a real unsigned 32-bit size limit (approximately 4 GiB); disk space, memory needed for model contexts, thermal throttling, and Android foreground-service quotas still apply. Removing an arbitrary text cap is not an infinite storage or execution guarantee. Parts and final output temporarily require additional disk space. Valid per-worker part WAVs have SHA-256 commit receipts and a signature covering build/chunking, text, voice reference, transcript, language and model hashes. System retry of the same worker can reuse them. Terminal failed/cancelled tasks are cleaned; stale storage is also pruned on refresh. A newly submitted task is a new generation.

Clone preview now synthesizes a short phrase instead of re-speaking a potentially long reference transcript. Complete conditioning audio and transcript remain unchanged, and a profile is committed only after real output is validated. A 16-second reference can still cost considerably more than a clean 3–10-second reference. No POCO speed multiplier is claimed.

## Real progress and CPU execution

Native patched callbacks expose reference encoding, completed MaskGIT iterations, and decoding through a numeric-only protocol. The UI labels percentages as **stage synthesis progress**: e.g. 8/32 is 25%. These are not an ETA or a percentage of total wall-clock work. Loading models/reference/decode stays indeterminate until the runtime reports completion. Text part i/N, elapsed time and age of the last changed native event help distinguish slow work. A stale counter is diagnostic evidence, not proof of a deadlock.

A separate ARM64 library enables only dot-product and I8MM compiler capabilities. It is selected only for a primary native ARM64 process whose actual auxvec supports both; generic execution is retained. Each backend runs real Q4_K/Q6_K mathematical checks against a scalar reference before selection. JNI symbols are isolated; an inference failure never silently restarts with another backend. No GPU/NPU acceleration is claimed. QEMU numerical tests exercise ARM kernels but do not certify phone performance.

Private diagnostics record build/device metadata, stages, counters, times, thermal status, observed PSS and failure class. They exclude reference audio, speech text/transcripts, profile names and private paths. Export is an explicit Settings share action; no analytics or upload runs automatically. Observed PSS is sampled, not a guarantee of peak memory or absence of leaks.

## Update distribution and security

Only `WynnDev-rill/VoClone` is a publication destination. Wynn-Store publication was cancelled by the owner. v0.2 checks `https://raw.githubusercontent.com/WynnDev-rill/VoClone/main/updates/alpha.json`, and accepts only allowlisted VoClone GitHub Release APK paths. Metadata validates package, higher versionCode, API compatibility, size, hash and the installed owner signing certificate. APK archive identity and signer are checked again before Android's package installer. Checks run when opened (default 12-hour cooldown; the new build/feed is checked once immediately). User-disabled automatic checks remain disabled. Android still requires the user's installation confirmation; updates are not silently installed.

v0.1 hardcoded the previous Wynn-Store feed. Because publication there was cancelled, install v0.2 manually once, over the existing app with the same owner signer. Do not uninstall or clear data. Future higher-version releases can be discovered by v0.2 in VoClone's own feed after publication. No GitHub token is embedded in the APK.

A read-only public-source audit examined all historical tracked blobs and expanded the earlier APK/crash archive. No recognizable real private signing key, token, password or personal voice was found. This is a bounded audit, not a blanket security certification. The only tracked test voice is the documented public JFK fixture. Signing keys/passwords stay outside git; owner backup must remain private. Release source must contain the exact corresponding AGPL source and native dependency sources/archives. Model weights are not bundled; their distinct noncommercial/restricted terms still apply.

## Verification and reproduction

Run the build/test commands in AGENTS.md, `python3 scripts/verify-apk.py <apk>`, and `bash scripts/test-arm-kernels.sh` on an Ubuntu host with the tools listed in that script. The ARM numerical checks run under QEMU; they do not measure POCO speed. `qa/host_progress.cpp` runs the exact patched host core with actual models and verifies reference/32-step/decode events, cancellation, and a valid non-silent synthesized WAV.

GitHub Actions defaults to build, JVM/lint, Android 16 UI/JNI/codec tests and ARM kernel math tests. `workflow_dispatch full_inference=true` is an explicit QA operator acceptance of the terms in ModelCatalog and bundled model licences; it downloads the three verified models, enables airplane mode, transcribes a real reference and runs actual clone/generate Workers while Home is pressed. Model consent remains mandatory in the production app; opt-in test setup is confined to the instrumentation APK. Source contains the test, not the user's consent or models.

Physical POCO F6 listening/speaker similarity (especially Indonesian/Japanese), 16-second reference timing, thermals/battery, prolonged background execution and installed owner-signer upgrade remain device gates until recorded. Alpha labelling is retained while those gates lack evidence. Read docs/DEVICE_TEST.md; do not describe a compile or visual fixture as successful cloning.
