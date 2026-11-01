# Update 0.2.0-alpha — 1 November 2026

Current update details and limits: [UPDATE_0.2.md](UPDATE_0.2.md). Source versionCode2; original signer retained; distribution/update feed moved entirely to this now-public VoClone repository. No Wynn-Store publication. v0.1 requires one manual same-signer upgrade.

Already verified for this update: 57 JVM tests passed, actual patched host AI produced a new valid non-silent 24k mono WAV with reference/32 native iterations/decode callbacks, and actual native cancellation passed. Evidence: `qa/evidence/update-host-progress.json` and `.txt`. The 256.466-second host run includes load/cancel and concurrent Android build; it is not a POCO benchmark. The 240-second streaming WAV test and real LAME five-hour **frame-count initialization** test passed; neither claims five hours of AI output. First lint failure (getStopReason requires API31) was fixed with a guard for min29. Final local build `testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` passed in4m7s (133 tasks); lint0errors/34warnings. Pre-clean-source owner candidate APK8,799,889bytes (8.39MiB), SHA `4762f1ebe292567f1ca192d5c86f293c381ae1c6ffc7698b3fb83fe0085f16be`, signer matches original; ARM64 ZIP and all6nativeELFs16KiB aligned. No model weights or signing key in APK.

CI [41314858599](https://github.com/WynnDev-rill/VoClone/actions/runs/41314858599), app source `0641ca5dd1b1634f975bc3262f66e446962481de`: validate and ARM numerical jobs passed. Generic Q4_K/Q6_K executes under QEMU Cortex-A53, dotprod/I8MM variant under QEMU max; both match dequantized scalar reference across even/odd/single-column shapes. Numeric progress/privacy parser, capability gate and actual LAME initialization regression also passed. Android16 emulator passed default7JUnit cases (5executed,2opt-in AI assumption-skipped), plus1narrow/font1.3UI case. Two ProgressUiTest cases are presentation fixtures, not AI claims. Evidence summaries in qa/evidence/update-arm-kernels.txt and update-emulator-summary.txt; screenshots inspected for light and narrow dark layouts. [Emulator artifact](https://github.com/WynnDev-rill/VoClone/actions/runs/41314858599/artifacts/13086827513).

Actual original-owner APK upgrade rehearsal [41315642608](https://github.com/WynnDev-rill/VoClone/actions/runs/41315642608) passed on disposable rooted Google APIs Android16 emulator: v0.1 install, seeded documented public-reference/real-preview QA profile, actual UI reads it, install-r v0.2, same SHA for profile/reference/preview/storage sentinel, preferences retained and actual v0.2 UI shows profile. This is not a real model download or physical-device upgrade test. [Evidence artifact](https://github.com/WynnDev-rill/VoClone/actions/runs/41315642608/artifacts/13087292288). Initial rehearsal41315430688 failed before install at signer precheck without adequate verifier output; APK hashes matched. Pinning build-tools36 and capturing verifier output produced the passing run with unchanged APKs; do not infer an app data defect from that setup failure. Subsequent full real-Worker evidence is recorded separately below when complete.

Two read-only reviews found no remaining Critical/Important production/security issue after fixes to storage reclamation, queued-task cancellation, whitespace scans, and cancellation during cached-part validation. New real-Worker instrumentation cleanup was corrected to restore connectivity and cancel only its own requests. Build success, UI fixture tests, actual host AI, emulator AI and physical phone tests are separate evidence categories.

The report below is retained as **v0.1 historical evidence**, not a claim that v0.2 has already passed those checks.

# Verifikasi VoClone — 30 Oktober 2026

Status: alpha, bukan sertifikasi perangkat fisik. Build lokal snapshot akhir berhasil; status CI/emulator dicatat terpisah setelah workflow dijalankan. Tidak ada mock AI dalam jalur produksi.

## GitHub Actions

Run final [41107870881](https://github.com/WynnDev-rill/VoClone/actions/runs/41107870881), source aplikasi `ef605ddad91299383ad56181e7b712fab79ab2e3`, **attempt2 selesai sukses**:

- Build CI 4m34s; **34 tes JVM /0 gagal /0 skip**, 2,670s. Lint CI **0 error /35 warning**; satu warning tambahan terhadap host adalah OldTargetApi karena SDK terbaru lebih baru daripada target36 Android16 yang dipilih.
- [Artifact APK debug + instrumentation + laporan tes/lint](https://github.com/WynnDev-rill/VoClone/actions/runs/41107870881/artifacts/13001862231). APK debug 33.400.995 byte, SHA `c1aced1da68a8118c9b58609a6a5624d9b5d17d3c1904a8b9c3842f341d6b407`; ARM64, ZIP/ELF16KiB lulus. Signer debug berbeda dari owner.
- Emulator API36 dengan KVM dan ARM64 translation: JNI ABI2, WAV/MP3 dan UI berhasil. Default suite melaporkan OK (4 tests): **3 dijalankan, 1 full-inference dilewati** oleh syarat opt-in, 41,120s. UI 320dp/font1,3x melaporkan OK (1 test), 21,783s. Tema terang/gelap, scroll hingga Generate Audio dan native Back dijalankan.
- [Artifact 10 screenshot + log Android16](https://github.com/WynnDev-rill/VoClone/actions/runs/41107870881/artifacts/13021102820). Salinan tersedia di `qa/evidence/android16/`; empat tampilan final ditinjau langsung. Tidak mengklaim TalkBack atau perangkat fisik dari screenshot.
- Attempt1 berhenti dengan `INSTRUMENTATION_ABORTED: System has crashed` sebelum laporan kelas tes; penyebab spesifik crash sistem tidak terbukti. Pengulangan pada runner baru berhasil tanpa mengubah APK. Bukti asli disimpan di `qa/evidence/ci-emulator-system-crash-attempt1.zip` sebelum artefak gagal324byte dibersihkan agar uploader versi lama dapat memakai nama yang sama. Workflow berikutnya mengumpulkan logcat/build fingerprint dan screenshot sebelum validasi gagal, serta memakai nama artefak per-attempt; perubahan diagnostik ini divalidasi YAML, tidak diklaim telah dijalankan pada attempt2.

Run awal 41098645338 gagal sebelum build karena paket SDK `tools` yang sudah dihapus. Root cause diperbaiki dengan `packages: platform-tools`. Run41099863307 kemudian sukses sebelum polish UI. Semua kegagalan tersebut tetap dibedakan dari pass. Job release bertanda tangan tidak diminta pada push biasa; empat Secrets owner belum terpasang karena izin integrasi403. APK owner dibangun dan ditandatangani lokal.

## Bukti yang sudah tersedia

- 34 tes JVM lulus pada source aplikasi: 5 audio/WAV/resampling, 6 unduhan model (SHA rusak, resume, restart, pembatalan, wrong Range, traversal), 3 profil (valid preview, persist/rename/delete, process-death staging recovery), 14 metadata updater dan 6 transfer APK. Recovery test folder staging gagal sebelum perbaikan, lalu lulus. Tes decoder native Android terpisah dari tes WAV JVM.
- Build akhir `testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` berhasil: 133 task, 5m45s pada run polish terakhir (19 executed /114 cached; JVM34 lulus kembali/cache unchanged). Tes34/34; lint0 error dan34 warning (terutama versi dependency yang lebih baru tersedia, serta atribut/commit prefs). APK debug, instrumentation dan owner release dibangun. Lint tidak diklaim bebas warning. ZIP dan seluruh ELF native, termasuk AndroidX graphics path, lolos alignment16KiB; signer cocok dengan backup owner.
- Sumber Omni/GGML/Whisper dipatok dan SHA diverifikasi. Ketiga bobot benar-benar diunduh di host dan cocok ukuran/SHA, terpisah dari APK.
- Inferensi C API native asli memakai ref ucapan publik 3,2 detik yang ditranskripsikan Whisper, OmniVoice Q4, default 32 MaskGIT steps, codec sebenarnya, lalu WAV baru. English: input “Hello, friend.”, hasil ASR “Hello friend!”; Indonesia: “Halo, dunia.” -> “Halo, dunia!”; Jepang: “こんにちは。” -> “こんにちは!”. Ketiga WAV 24 kHz mono, finite/non-silent. Metadata/hashes/log di `qa/evidence/host-native-results.json` dan `host-native.log`.
- Waktu host masing-masing 661,098 / 654,620 / 739,090 detik untuk frasa singkat, dalam cloud dengan kuota dua CPU berbagi build/emulator. Ini bukti bahwa jalur CPU bisa sangat lambat, bukan estimasi ponsel. Tidak mengklaim VoClone sudah cepat atau speaker similarity lolos.
- Pembatalan native nyata: status OV_STATUS_CANCELLED (-5), 2,05954 detik, nol sampel; context yang sama kemudian berhasil menyintesis. Patch abort RAII tidak mengubah ABI publik 2.
- Percobaan awal ref11s dihentikan manual setelah 783,84 detik karena kontensi CPU; tidak dihitung lulus dan tidak menghasilkan audio.
- Review kode independen menemukan lalu memeriksa perbaikan: wrong updater path, stale worker output replay/queued recovery, stale profile stage, MP3 partial cache/race, full licence fingerprint, JNI OOM/null allocation, cancellation, dan GGML allocation double-free. Ini review kode, bukan pengganti test perangkat.
- Graphify resmi menghasilkan graph.html, graph.json dan GRAPH_REPORT.md. Graph sekarang menyorot aplikasi; parser Kotlin versi ini punya empat file dengan ekstraksi parsial. Build compiler adalah otoritas syntax, bukan warning parser graph.

## Ukuran dan identitas yang dipatok

| Berkas | Byte |
|---|---:|
| OmniVoice base Q4_K_M | 407.485.216 |
| OmniVoice tokenizer Q4_K_M | 252.474.112 |
| Whisper Tiny GGML | 77.691.713 |
| Total model | **737.651.041 (703,5 MiB)** |

Package id.wynn.voclone, minSdk29, targetSdk36, ARM64, versionCode1 / 0.1.0-alpha. APK owner final **7112183 byte (6.78 MiB)**, SHA-256 `ab46b56234467c07c596518f5a386f52eb1dcab2a1771f0dff8ec8dd64853447`. Metadata lengkap: qa/evidence/apk-release.json. Public signer di docs/signing-sha256.txt; private key hanya backup owner. Model tidak dibundel.

## Distribusi alpha yang tersedia

[Release v0.1.0-alpha](https://github.com/WynnDev-rill/VoClone/releases/tag/v0.1.0-alpha), source commit a06b3005bbc80ee86e248b9a48c5924d372d11e4. [Actions publikasi41148356729](https://github.com/WynnDev-rill/VoClone/actions/runs/41148356729) sukses setelah verifier memeriksa ulang APK owner/hash/signer/ARM64/alignment16KiB. [Artifact owner APK](https://github.com/WynnDev-rill/VoClone/actions/runs/41148356729/artifacts/13022330442). APK7112183 byte; corresponding source di Release31248825 byte. Arsip source lokal dapat berbeda byte gzip/timestamp, bukan klaim reproduksi byte-identik.

APK owner ditandatangani lokal, kemudian diunggah oleh Actions; private key tidak masuk repo maupun job tersebut. Direct upload cloud401 ditangani dengan transport sementara yang dijelaskan di BUILD.md. Source repo dan Release tetap privat; belum ada publikasi pada Wynn-Store.

## Belum terbukti / batasan

- Belum ada POCO F6/Android16 fisik: kualitas identitas/timbre/intonasi, bahasa panjang dan multi-speaker/noisy samples, peak RAM, kebocoran memori, suhu, baterai, accelerators, latency dan stabilitas panjang belum dibuktikan. ASR benar pada satu frasa bukan penilaian kualitas lengkap atau jaminan multilingual.
- Cloud host menggunakan core C API Linux; Android JNI/ART, microphone, MediaCodec, foreground quotas, low-memory process death dan installer berbeda. Emulator lokal API36 tanpa KVM tidak mencapai Package Manager; aplikasi tidak dinyatakan lulus di emulator tersebut. Job CI dengan KVM sudah menjalankan tes JNI/UI/audio pada sourceef605dd seperti dicatat di atas; ini ARM64 translation, bukan POCO F6 fisik. Expensive Android real-AI test default **skip** sampai pemilik menerima model terms, mengunduh model dan mengaktifkan fullInference.
- CPU only. Tidak ada klaim GPU/NPU. Referensi 3–30s, transkrip <=2.000 karakter, teks generasi <=600. Profil adalah reference conditioning + preview, bukan fine-tuning model baru. MP3 mono24kHz64kbit/s via LAME.
- Model lisensi memiliki metadata bertentangan dan ketentuan nonkomersial/tokenizer; persetujuan dan atribusi ditampilkan. Tidak mengklaim izin komersial.
- Actions Secrets gagal disetel: GitHub API403 Resource not accessible by integration. Local owner signing tersedia; setup release Secrets perlu izin pemilik. Tidak ada key/password/token di git.
- Source repo privat. Kanal updater publik menunggu keputusan pemilik untuk membagikan APK dan corresponding source pada Wynn-Store. Feed404 ditangani sebagai belum ada rilis. Tidak ada token private GitHub dalam APK. Same-signer version upgrade di perangkat masih perlu diuji.

Lihat DEVICE_TEST.md untuk langkah penerimaan dan bukti yang harus disimpan. Stable release tidak boleh disimpulkan hanya dari compile, tes unit, graph atau host WAV.


Final process-death storage regression: two assertions reproduced retained `results/UUID.part` and old `texts/UUID.part`. The fix preserves active retry stages/completed WAV/recent preparation/owned text while reclaiming terminal or old orphan stages. Unit suite now57passed; review found no Critical/Important issue. Final application source `37c1d388726b54ae86542b7574340514ecd2c24f`; native engine, Worker, JNI and model catalog are identical to the expensive full-inference source0641ca5 (only SpeechStorage and its new test differ). Standard CI [41317679233](https://github.com/WynnDev-rill/VoClone/actions/runs/41317679233) validates this final application code separately. Final owner APK upgrade repeated successfully in [41317897280](https://github.com/WynnDev-rill/VoClone/actions/runs/41317897280), using SHA4762f1… with unchanged original signer. The previous pre-pruner owner candidate was614281…; its standalone upgrade also passed. An intervening rehearsal with a mistyped source commit was cancelled before testing and is not evidence.


## Final real Android pipeline evidence

[Actions41319064053](https://github.com/WynnDev-rill/VoClone/actions/runs/41319064053), application code37c1d38, QA/workflow sourceca6524734d6f3c5c6f272f0962d33c4e24273ecf, completed **success**. ARM64 build/57JVM tests/lint, ARM QEMU mathematical tests and Android16 UI/JNI/WAV/MP3/default+large-font tests all passed. The real-Worker job also passed: **one actual native x86_64 Android E2E test**,798.96s including verified model download, Whisper, clone and new-text generation. The debug-only ABI overlay changes no production Kotlin/JNI/Worker/model code and explicitly rejects release tasks. Production owner APK stays ARM64.

Airplane mode was enabled before inference, Wi-Fi disabled, Home pressed during actual clone/generate Workers; both succeeded with real sampler progress observed. Profile persisted and a different text produced a different valid WAV. Preview1.99s/24kHz mono,95,608bytes,SHA `d6a9c0192a56b89719b28b10b1f355609a6018ce96cdcc149a44d33de5110783`; generated1.41s/24kHz mono,67,768bytes,SHA `86b319753243de466c5e7a8715682b67f914e772690fd7bd8c39d4f2d57e5230`. Independent host Whisper ASR of these exact generated files recognized “This is my voice” and “Hello!”. This supports English text intelligibility, not speaker similarity certification.

Typed diagnostic snapshots show clone363.092s (337.388s sampler) and generate318.562s (297.169s sampler) in this native x86 emulator; sampled peak PSS908,906/898,200KiB and final114,099/109,452KiB respectively. These are observed emulator values, not phone memory requirements or proof of no leaks. Source evidence is in `qa/evidence/update-android-worker/`; [actual WAV/log artifact](https://github.com/WynnDev-rill/VoClone/actions/runs/41319064053/artifacts/13088697828).

The earlier **ARM64 translated emulator E2E failed**, run41314858599: Whisper succeeded, reference encoded, sampler advanced0→12/32 at roughly151s/step, then the35-minute test deadline cancelled it. No completed ARM64 WAV is claimed from that run. Numeric progress and foreground execution were observed; source evidence `qa/evidence/update-arm-emulator-timeout.txt` and artifact13088421712 preserve the failure. Berberis ARM translation is unsuitable as a POCO timing benchmark. Native x86 QA succeeded while ARM64 kernel math/loading/codec and partial actual sampling were separately verified. Full physical ARM64 synthesis/listening/speed/thermal/background endurance remains a required device gate.

## Final artifact provenance and publication

Earlier locally signed candidate hashes above are retained as chronological evidence. The release owner APK is rebuilt **after** the final clean source commit so its embedded `META-INF/version-control-info.textproto` matches sourceCommit in alpha.json and the source archive. package-release.py enforces this equality; generated `VoClone-0.2.0-alpha-verification.md` begins with the authoritative final APK hash/size/source identity. Final packaging is followed by another same-owner upgrade rehearsal before publication; the publication workflow is the authoritative evidence for that exact final binary. No candidate with stale embedded provenance is published.

Both normal signed-release and constrained owner-transport routes require successful actual E2E evidence. Publication creates only immutable VoClone release assets, then verifies anonymous APK **and corresponding source** SHA/size before updating only VoClone/main/updates/alpha.json. Rollback, same-code replacement and concurrent Contents API SHA conflicts are rejected. Three publisher policy tests pass. The documented feed-only retry uses original release assets and never recreates/clobbers a published Release. Original signer backup remains private and valid; GitHub integration Secrets access still403, so the one-time owner APK is built/signed locally and transported without key material. Actions distributes/verifies it; it does not claim to have signed/rebuilt that owner binary. Debug APKs and all CI tests are built on Actions.
