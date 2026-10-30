# VoClone

Android native untuk voice cloning lokal: Kotlin, Jetpack Compose, Material 3, OmniVoice GGUF + Whisper. Tema putih-biru, dark mode, Plus Jakarta Sans, dan satu halaman cloning tanpa bottom navigation.

**Status: alpha.** Engine asli terintegrasi; hasil build, inferensi nyata, dan batasan pengujian ada di [laporan verifikasi](docs/VERIFICATION.md). Jangan menyamakan host Linux dengan bukti kualitas/stabilitas POCO F6. Bobot model diunduh terpisah setelah persetujuan; OmniVoice nonkomersial dan tokenizer memiliki syarat tersendiri.

Impor MP3/WAV atau rekam -> validasi/resample -> transkrip Whisper yang bisa dikoreksi -> preview sintetis nyata -> profil lokal -> teks baru -> playback, save/share WAV/MP3. Worker latar belakang, pembatalan native, unduhan resume + SHA-256 dan updater dengan pemeriksaan signer. Tidak ada server inferensi, analytics atau unggahan suara.

- [Build, signing dan GitHub Actions](docs/BUILD.md)
- [Arsitektur](docs/ARCHITECTURE.md) dan [riset engine/produk](docs/RESEARCH.md)
- [Prosedur pengujian perangkat](docs/DEVICE_TEST.md)
- [Sumber aset dan lisensi](THIRD_PARTY_NOTICES.md)
- [Skill dan pembaruan Graphify](docs/SKILLS.md)
- [Graph interaktif](graphify-out/graph.html), [JSON](graphify-out/graph.json), [laporan graph](graphify-out/GRAPH_REPORT.md)
- [Instruksi AI berikutnya](AGENTS.md)

APK debug di artifact workflow **Android APK**. Owner release menggunakan kunci asli yang fingerprint publiknya tersimpan di docs/signing-sha256.txt; private key hanya dalam backup pemilik/Secrets, tidak pernah di repository. Tidak ada jaminan memori lintas sesi AI: AGENTS.md, dokumentasi dan backup privat adalah handoff yang persisten.

AGPL-3.0; atribusi VoiceStudio dan semua lisensi komponen dipertahankan. Gunakan suara yang dimiliki atau dengan izin pemiliknya.
