# Riset Asset-First — 30 Oktober 2026

Evaluasi ini berdasarkan kode, kartu model dan dokumentasi primer yang dibaca dalam sesi, bukan benchmark kualitas lintas produk yang dibuat-buat. Dokumen/README bergerak; keputusan runtime dan bobot dipatok terpisah.

| Produk / engine | Hal yang diadopsi | Keterbatasan untuk VoClone |
|---|---|---|
| [VoiceStudio](https://github.com/debpalash/VoiceStudio/tree/4d4cb0badf99c7518e3f4c69487df6e8bfeff43f/mobile/android) | Android local-first, native Omni JNI, source/model pins, licence gates, PCM pipeline, output validation, source attribution, mature MP3 encoder subset | Aplikasi jauh lebih luas: desktop pairing, dubbing, translation, ASR/TTS banyak engine dan UI banyak tab tidak dibutuhkan. Sherpa Whisper exports punya metadata lisensi konversi tidak jelas. Katalog bukan bukti kualitas Indonesia/Jepang atau RAM. |
| [ElevenLabs voice cloning](https://elevenlabs.io/voice-cloning) | UX reference -> voice -> text -> preview; reference yang bersih, status nyata dan persetujuan suara | Layanan terkelola/cloud dan API tidak memenuhi inferensi offline bebas biaya. Tidak dipakai sebagai backend. Tidak melakukan benchmark audio berbayar. |
| [Chatterbox](https://github.com/resemble-ai/chatterbox) | Reference-driven synthesis, perhatian pada speaker similarity dan stabilitas; model Nano menargetkan CPU/on-device | README saat audit menyebut Nano English dan multilingual V3 23+ bahasa; pipeline Python/PyTorch bukan drop-in JNI Android. Klaim kecepatan upstream tidak diadopsi sebagai hasil ponsel. Tidak mengganti native path yang sudah tersedia. |
| [F5-TTS](https://github.com/SWivid/F5-TTS) | Reference audio + transcript yang akurat, preview dan pemisahan lisensi kode/bobot | Kode MIT; bobot menyebut CC-BY-NC. Membutuhkan port/runtime/tokenizer/vocoder Android tersendiri. Tidak lebih matang daripada port Omni yang tersedia untuk lingkup ini. |
| [XTTS-v2](https://huggingface.co/coqui/XTTS-v2) | Profil referensi tersimpan, bahasa eksplisit dan conditioning ulang untuk text berikutnya | CPML bobot terpisah dari kode; implementasi Python dan runtime Android/fitur bahasa harus dibuktikan. Bukan pilihan yang otomatis menyelesaikan Indonesia/Jepang. |

Situs pemasaran ElevenLabs mengembalikan HTTP403 dari lingkungan ini. Riset API-nya diverifikasi melalui README SDK resmi https://github.com/elevenlabs/elevenlabs-python yang menyatakan cloning membutuhkan API key; tidak ada benchmark berbayar yang dijalankan.

## Keputusan engine

Pilih OmniVoice Q4_K_M pada commit `886fc079838ca7400cb2b42b36e2a65aa1daabe8`, dengan dua GGUF yang persis dipakai katalog Android VoiceStudio. Kode dan bobot/tokenizer dipisahkan. Whisper Tiny melalui whisper.cpp dipilih daripada Sherpa-ONNX supaya runtime berisi fungsi yang diperlukan dan rantai lisensi asli Whisper/MIT lebih jelas. Ini tetap Whisper + OmniVoice, tanpa server fallback. Model tidak dilatih/fine-tune di ponsel: profil adalah referensi + transkrip + preview yang benar-benar disintesis, bukan klaim bobot speaker baru.

Audit Android VoiceStudio meliputi `core/local/LocalEngine.kt`, `AudioFiles.kt`, `ModelCatalog.kt`, `ModelDownloads.kt`, `PhoneVoices.kt`, `PhoneAudioExport.kt`, `mobile/native/android/{omni.cpp,control.h,build.sh,CMakeLists.txt}`, source checksum/pins, model credits dan LAME subset. Diadaptasi hanya bridge/pola yang diperlukan dan encoder; UI, server, llama translation, Sherpa, PDF, espeak dan engine lain tidak dibundel.

Whisper dan Omni dimuat berurutan, tidak bersamaan. Referensi mono 24 kHz, input Whisper resampled ke 16 kHz, output Omni 24 kHz. Mapping bahasa runtime memiliki `id` dan `ja`; keberadaan mapping bukan bukti inteligibilitas/kemiripan. UI memberi label uji; cek status bukti terbaru di VERIFICATION.md. Tidak ada klaim GPU tersedia.

## Konflik lisensi yang benar-benar ditemukan

Kartu konversi pada `3616093` menyebut Apache-2.0, tetapi kartu sebelumnya `0170941` menyebut CC-BY-NC-4.0; [kartu model asli](https://huggingface.co/k2-fsa/OmniVoice) saat audit masih mengatakan kode Apache-2.0 dan bobot CC-BY-NC karena data training. [Tokenizer Higgs Audio 2](https://huggingface.co/bosonai/higgs-audio-v2-tokenizer/blob/main/LICENSE) memuat Community License Boson dan referensi Meta Llama 3. Metadata Apache pada konversi tidak membuktikan pelepasan kewajiban komponen. VoClone memakai pembatasan nonkomersial yang konservatif, menyertakan teks/atribusi dan meminta penerimaan sebelum unduhan. Tidak ada bobot dibundel atau izin komersial yang diklaim. Lihat THIRD_PARTY_NOTICES.md.

## Standar pengalaman pengguna

Buka langsung Voice Cloning, satu kolom scroll, settings di kanan atas, tidak ada bottom navigation. Tidak ada profil contoh atau waveform acak. Model manager menampilkan byte yang dipatok, partial resume dan integritas sebelum ready. Error menuntun pemulihan. Transkrip dapat diperiksa/dikoreksi. Tombol Clone baru aktif setelah referensi, transkrip, model dan persetujuan suara siap. Simpan profil hanya sesudah preview nyata. Semua aksi utama terhubung; keterbatasan perangkat tidak disamarkan sebagai keberhasilan.
