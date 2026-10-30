# Verifikasi VoClone — 30 Oktober 2026

Status: alpha, bukan sertifikasi perangkat fisik. Build lokal snapshot akhir berhasil; status CI/emulator dicatat terpisah setelah workflow dijalankan. Tidak ada mock AI dalam jalur produksi.

## Bukti yang sudah tersedia

- 34 tes JVM lulus pada source aplikasi: 5 audio/WAV/resampling, 6 unduhan model (SHA rusak, resume, restart, pembatalan, wrong Range, traversal), 3 profil (valid preview, persist/rename/delete, process-death staging recovery), 14 metadata updater dan 6 transfer APK. Recovery test folder staging gagal sebelum perbaikan, lalu lulus. Tes decoder native Android terpisah dari tes WAV JVM.
- Build akhir `testDebugUnitTest lintDebug assembleDebug assembleRelease assembleDebugAndroidTest` berhasil: 133 task, 6m16s pada run terakhir. Tes34/34; lint0 error dan34 warning (terutama versi dependency yang lebih baru tersedia, serta atribut/commit prefs). APK debug, instrumentation dan owner release dibangun. Lint tidak diklaim bebas warning. ZIP dan seluruh ELF native, termasuk AndroidX graphics path, lolos alignment16KiB; signer cocok dengan backup owner.
- Sumber Omni/GGML/Whisper dipatok dan SHA diverifikasi. Ketiga bobot benar-benar diunduh di host dan cocok ukuran/SHA, terpisah dari APK.
- Inferensi C API native asli memakai ref ucapan publik 3,2 detik yang ditranskripsikan Whisper, OmniVoice Q4, default 32 MaskGIT steps, codec sebenarnya, lalu WAV baru. English: input “Hello, friend.”, hasil ASR “Hello friend!”; Indonesia: “Halo, dunia.” -> “Halo, dunia!”; Jepang: “こんにちは。” -> “こんにちは!”. Ketiga WAV 24 kHz mono, finite/non-silent. Metadata/hashes/log di `qa/evidence/host-native-results.json` dan `host-native.log`.
- Waktu host masing-masing 661,098 / 654,620 / 739,090 detik untuk frasa singkat, dalam cloud dengan kuota dua CPU berbagi build/emulator. Ini bukti bahwa jalur CPU bisa sangat lambat, bukan estimasi ponsel. Tidak mengklaim VoClone sudah cepat atau speaker similarity lolos.
- Pembatalan native nyata: status OV_STATUS_CANCELLED (-5), 2,05954 detik, nol sampel; context yang sama kemudian berhasil menyintesis. Patch abort RAII tidak mengubah ABI publik 2.
- Percobaan awal ref11s dihentikan manual setelah 783,84 detik karena kontensi CPU; tidak dihitung lulus dan tidak menghasilkan audio.
- Review kode independen menemukan lalu memeriksa perbaikan: wrong updater path, stale worker output replay/queued recovery, stale profile stage, MP3 partial cache/race, full licence fingerprint, JNI OOM/null allocation, cancellation, dan GGML allocation double-free. Ini review kode, bukan pengganti test perangkat.
- Graphify resmi menghasilkan graph.html, graph.json dan GRAPH_REPORT.md. Graph sekarang menyorot aplikasi; parser Kotlin versi ini punya lima file dengan ekstraksi parsial. Build compiler adalah otoritas syntax, bukan warning parser graph.

## Ukuran dan identitas yang dipatok

| Berkas | Byte |
|---|---:|
| OmniVoice base Q4_K_M | 407.485.216 |
| OmniVoice tokenizer Q4_K_M | 252.474.112 |
| Whisper Tiny GGML | 77.691.713 |
| Total model | **737.651.041 (703,5 MiB)** |

Package id.wynn.voclone, minSdk29, targetSdk36, ARM64, versionCode1 / 0.1.0-alpha. APK owner final **7112183 byte (6.78 MiB)**, SHA-256 `6e8b56c76c1ba6ce824f7feeb703d9216faec68c62ef38dc1809b6238d7aca67`. Metadata lengkap: qa/evidence/apk-release.json. Public signer di docs/signing-sha256.txt; private key hanya backup owner. Model tidak dibundel.

## Belum terbukti / batasan

- Belum ada POCO F6/Android16 fisik: kualitas identitas/timbre/intonasi, bahasa panjang dan multi-speaker/noisy samples, peak RAM, kebocoran memori, suhu, baterai, accelerators, latency dan stabilitas panjang belum dibuktikan. ASR benar pada satu frasa bukan penilaian kualitas lengkap atau jaminan multilingual.
- Cloud host menggunakan core C API Linux; Android JNI/ART, microphone, MediaCodec, foreground quotas, low-memory process death dan installer berbeda. Emulator lokal API36 tanpa KVM tidak mencapai Package Manager; aplikasi tidak dinyatakan lulus di emulator tersebut. Job CI dengan KVM menyiapkan tes native/UI/audio; status aktual ditambahkan sesudah job selesai. Expensive Android real-AI test default **skip** sampai pemilik menerima model terms, mengunduh model dan mengaktifkan fullInference.
- CPU only. Tidak ada klaim GPU/NPU. Referensi 3–30s, transkrip <=2.000 karakter, teks generasi <=600. Profil adalah reference conditioning + preview, bukan fine-tuning model baru. MP3 mono24kHz64kbit/s via LAME.
- Model lisensi memiliki metadata bertentangan dan ketentuan nonkomersial/tokenizer; persetujuan dan atribusi ditampilkan. Tidak mengklaim izin komersial.
- Actions Secrets gagal disetel: GitHub API403 Resource not accessible by integration. Local owner signing tersedia; setup release Secrets perlu izin pemilik. Tidak ada key/password/token di git.
- Source repo privat. Kanal updater publik menunggu keputusan pemilik untuk membagikan APK dan corresponding source pada Wynn-Store. Feed404 ditangani sebagai belum ada rilis. Tidak ada token private GitHub dalam APK. Same-signer version upgrade di perangkat masih perlu diuji.

Lihat DEVICE_TEST.md untuk langkah penerimaan dan bukti yang harus disimpan. Stable release tidak boleh disimpulkan hanya dari compile, tes unit, graph atau host WAV.
