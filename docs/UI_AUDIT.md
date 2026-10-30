# Impeccable native audit

Mengikuti reference/android.md, craft-floor.md dan audit.native.md. Ini audit source dan pengukuran warna, bukan klaim screenshot/UX perangkat sudah lulus.

Perbaikan yang diterapkan: satu halaman scroll tanpa dock bawah; native Back/insets/IME; hierarchy reference -> real preview/profile -> text generation; light/dark semantic colors; Plus Jakarta Sans lokal; 48dp control targets; labels/roles native; consent rows digabungkan untuk TalkBack; tombol utama disabled berdasarkan state nyata; waveform berasal dari PCM; current-chunk/phase progress tidak berupa timer palsu. Tidak ada profil contoh atau avatar orang fiktif.

Ikon aplikasi adalah JPEG pilihan pengguna yang sama. Header memakai target decode128px dan drawable-nodpi supaya sumber1280px tidak di-upscale Android menjadi bitmap puluhan MiB untuk thumbnail36dp. Hanya 15 vector Material round dipaketkan; dependensi seluruh katalog ikon dihapus. Aset/header/domain tetap sesuai referensi pemilik, tidak diganti template dashboard web.

Warna teks utama/sekunder dan tombol diperiksa melalui script `qa/contrast.py`; threshold 4,5 untuk teks normal. Gambar dan state disabled bukan klaim contrast teks. Native Material motion mengikuti sistem; tidak ada animasi buatan yang mengabaikan Remove Animations.

Masih perlu hasil emulator/fisik: screenshot light/dark, font_scale1.3+,320dp, landscape/tablet, TalkBack focus order, keyboard Back, actual frame timing dan pointer seek. Audit source tidak memberi skor performance/accessibility perangkat yang belum diukur. Lihat VERIFICATION.md dan artifact emulator untuk status terbaru.
