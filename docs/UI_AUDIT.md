# Impeccable native audit

Mengikuti reference/android.md, craft-floor.md dan audit.native.md. Audit source, pengukuran warna dan screenshot Android16 CI dicatat terpisah dari pengujian POCO fisik.

Perbaikan yang diterapkan: satu halaman scroll tanpa dock bawah; native Back/insets/IME; hierarchy reference -> real preview/profile -> text generation; light/dark semantic colors; Plus Jakarta Sans lokal; 48dp control targets; labels/roles native; consent rows digabungkan untuk TalkBack; tombol utama disabled berdasarkan state nyata; waveform berasal dari PCM; current-chunk/phase progress tidak berupa timer palsu. Tidak ada profil contoh atau avatar orang fiktif.

Ikon aplikasi adalah JPEG pilihan pengguna yang sama. Header memakai target decode128px dan drawable-nodpi supaya sumber1280px tidak di-upscale Android menjadi bitmap puluhan MiB untuk thumbnail36dp. Hanya 15 vector Material round dipaketkan; dependensi seluruh katalog ikon dihapus. Aset/header/domain tetap sesuai referensi pemilik, tidak diganti template dashboard web.

Warna teks utama/sekunder dan tombol diperiksa melalui script `qa/contrast.py`; threshold 4,5 untuk teks normal. Gambar dan state disabled bukan klaim contrast teks. Native Material motion mengikuti sistem; tidak ada animasi buatan yang mengabaikan Remove Animations.

Screenshot awal API36 dari run41099863307 diperiksa langsung: ditemukan warna secondaryContainer bawaan ungu pada Record dan area consent terlalu rapat; keduanya diperbaiki (blue role eksplisit, row48dp dan jarak label8dp). Run41107870881 attempt2 lulus UI light/dark, font_scale1.3 dan320dp;10 screenshot disimpan di qa/evidence/android16/device/screenshots, termasuk layar generasi setelah scroll. Empat tampilan final diperiksa langsung; label terbaca, tombol compact ditumpuk dan tidak ada bottom navigation. Tes juga memeriksa tinggi consent48dp. Masih perlu hasil fisik: landscape/tablet, TalkBack focus order, keyboard Back, actual frame timing dan pointer seek. Audit source tidak memberi skor performance/accessibility perangkat yang belum diukur. Lihat VERIFICATION.md dan artifact emulator untuk status terbaru.
