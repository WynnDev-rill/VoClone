# POCO F6 / Android 16 / ARM64 acceptance

These steps are an unexecuted hardware checklist unless VERIFICATION.md explicitly links actual device evidence. Cloud Linux core inference is not Android JNI/ART, microphone permission, audio playback, thermal or upgrade evidence.

1. Install the owner-signed APK as a fresh install. Confirm package/version/certificate with `apksigner verify --print-certs` and compare docs/signing-sha256.txt. Open directly to cloning; no bottom navigation.
2. Settings -> read model/tokenizer terms -> accept -> download. Check **737,651,041 bytes total**. Pause network midway, cancel, reconnect and resume. Corrupt a test copy (debug device only) and confirm SHA rejection. Never replace model validation with a ready flag.
3. Enable airplane mode with Wi-Fi also off. Import a clean, permitted 3–10s WAV. Repeat MP3 stereo/44.1kHz; inspect mono24kHz result. Test corrupt, silent, too-short and >30s input; failures must not replace the existing valid profile.
4. Grant/deny microphone permission. Record, stop, background mid-recording and return; no continuing microphone capture. Observe duration and clipping indication.
5. Transcribe. Compare every word and correct manually if needed. Confirm voice ownership/permission. Clone. No percentage without a real callback. A profile appears only after a playable, nonempty synthesized preview exists.
6. Rename/restart/select/delete/recreate voice. Select saved profile, type genuinely different text, generate, play/seek, save and share WAV then MP3. Decode exported files with another audio app. Check synthetic provenance.
7. Test Indonesia and Japanese with native listeners. Use separate same-language and cross-language references. Record exact text, sample hash, duration, time, peak RAM, perceived intelligibility and speaker similarity. Valid WAV alone is not a quality pass.
8. During real generation press Cancel, then immediately run another generation. No partial result/profile may be presented as success. Background, rotate, lock/unlock, low-memory/process death and reopen. Completed work restores once; no stale transcript/profile selection replay.
9. Android Studio Memory Profiler or `adb shell dumpsys meminfo id.wynn.voclone`, plus CPU/battery/thermal evidence (`dumpsys thermalservice`). Repeat operations to detect retained contexts. Do not infer a RAM minimum from Linux RSS. Record thermal throttling and battery use. Android16 foreground-service/background-job quotas need actual testing.
10. UI: 320dp and normal POCO width; landscape; dark/system/manual appearance; font_scale1.3 and larger; TalkBack; reduced animation settings; keyboard + native Back. Capture real screenshots. Restore system settings after testing.
11. Updater: use a **same-owner-signer** older fixture, incremented versionCode release and the real feed. Confirm automatic check/toggle, download byte count, SHA, package/version/cert rejection fixtures, unknown-source consent and Android installer. Verify private voices survive upgrade. Never test production migration with an unrelated debug signer.

## Instrumentation commands

Use a disposable debug emulator/spare test device. Debug signer differs from the owner release: never uninstall the owner app/clear private voices to make these commands work, and never publish an owner-signed instrumentation APK. Test the owner installation on POCO through the UI checklist; the rooted upgrade script is only for disposable emulator shells and adds no root permission to the APK.

```sh
./gradlew assembleDebug assembleDebugAndroidTest
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb install -r app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
adb shell am instrument -w id.wynn.voclone.test/androidx.test.runner.AndroidJUnitRunner
# Opt in only after reading/accepting terms and installing models inside that debug installation:
adb shell am instrument -w -e fullInference true -e class id.wynn.voclone.DeviceTest id.wynn.voclone.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/id.wynn.voclone/files/qa ./qa/device-evidence
```

Default suite checks JNI dependency loading and real WAV/MP3 decoding, plus Compose launch/settings screenshots. The expensive offline test is explicitly skipped until fullInference is enabled; it creates its own temporary QA profile and deletes only that profile. It performs real Whisper -> Omni preview -> persisted profile -> different text -> valid WAV. The codec sine fixture is not a cloned voice and is never packaged in the production app. Keep actual logs and device/build identifiers alongside WAV hashes; redact personal voice/transcript data before sharing logs publicly.


## Update 0.2 regression cases

Install the v0.2 owner APK over v0.1 once (do not uninstall); v0.1's old Wynn-Store feed cannot discover the new VoClone destination. Capture package/cert, voices, model acceptance and file hashes before/after. v0.2 checks its own public feed when opened (12-hour cooldown; new build/feed gets one immediate check); test disabled-auto and manual check too. A future higher version must pass installer consent; no silent install claim.

Compare the same clean permitted reference at3–10seconds and16seconds. Record stage timings and CPU backend from the explicit Settings diagnostic export, plus total wall time, output duration and peak/thermal samples. Clone preview now has a short target; target duration should not repeat the entire reference. Do not compare unrelated text/reference/thermal conditions as a speed benchmark. Keep default32 iterations. Verify intelligibility and speaker identity by listening with the owner's permission.

Paste more than600characters, including Japanese, emoji, a long unbroken word and whitespace. Verify all content survives chunking; real text-part i/N and native iteration percentage are separate. Inspect transitions between parts; export WAV and MP3 and decode externally. Cancel during text preparation, reference encoding, synthesis and export; no partial success. On process death/system retry, SHA-verified completed parts of the same worker can be reused. Disk-full failure must reclaim terminal data. RIFF4GiB, available storage and Android service quotas are real limits.

The optional CI `full_inference=true` Worker test accepts the documented model terms explicitly for noncommercial QA via the instrumentation-only argument `acceptModelTerms=true`; it downloads and verifies models before airplane mode and actual Workers. It restores prior connectivity and cleans only its own requests/profile/reference in finally. Normal UI fixture tests and codec sine-wave tests do not claim AI synthesis.
