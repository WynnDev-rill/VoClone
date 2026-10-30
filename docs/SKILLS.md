# Skill dan tooling proyek

Diperiksa sebelum memasang. Ponytail sudah tersedia melalui Engineering Suite (`c22/ponytail`) di sesi ini, jadi tidak dipasang duplikat. Sumber resmi yang diperiksa: DietrichGebert/ponytail `96608e078b2d5ea607c5c3b8b5a54d1c1fba6846`. Default full; ultra pada audit arsitektur dan optimasi. Pada environment AI lain yang belum punya Ponytail, ikuti INSTALL.md resmi atau pasang `skills/ponytail/SKILL.md` project-scoped. Tidak menganggap plugin sesi sekarang otomatis ada pada AI lain.

UI UX Pro Max terpasang project-scoped dengan prosedur resmi terbaru: `npx --yes ui-ux-pro-max-cli init --ai codex`. CLI yang benar-benar dipakai 2.15.0, sumber upstream yang diperiksa `28b3b8dee8c976df4fa3e94aed4e092ad43fcb03`. Skill/data di `.agents/skills/`, design-system di `design-system/voclone/MASTER.md`. Rekomendasi layout web dari generator disesuaikan ke batasan native pengguna dalam DESIGN.md.

Impeccable terpasang dengan `npx --yes impeccable install -y --providers=codex --scope=project`. CLI 4.8.0, engine v0.1.18, sumber yang diperiksa `df3a4abe7d0840d5f60324812c432e00b66a4e75`. Context loader dijalankan; PRODUCT.md dan DESIGN.md ditulis berdasarkan brief eksplisit pengguna yang mendelegasikan keputusan. Native Android/craft-floor/audit references dibaca. Hook manifest terpasang, tetapi aktivasi hook oleh harness sesi ini tidak diklaim. Audit native dilakukan langsung; audit.native.md menyatakan detector web tidak berlaku untuk Compose. Binary engine diabaikan git; launcher resmi dapat mengunduh ulang dengan verifikasi checksum. Peran: UI UX Pro Max untuk sistem desain; Impeccable untuk audit/polish; Ponytail untuk efisiensi dan kualitas.

Graphify resmi diinstal menggunakan `uv tool install graphifyy`, versi yang terpasang **0.9.97**. Integrasi Codex: `graphify install --project --platform codex`; `.codex/skills/graphify`, hook manifest dan bagian AGENTS.md dibuat installer resmi. Tidak memakai layanan Graphify Cloud/API berbayar. Sumber diperiksa `0a8cd4e1e342235fe53e881fb5d591bf0460071f`.

## Perbarui graph

```sh
uv tool install graphifyy==0.9.97   # jika belum tersedia
# Setelah perubahan kode/arsitektur:
graphify update . --force
# Pertanyaan dependensi lebih dulu:
graphify query "NativeBridge VoiceEngine CloneWorker" --budget 2000
graphify path "VoiceEngine" "NativeBridge"
```

`graphify-out/graph.html`, `graph.json`, `GRAPH_REPORT.md` dihasilkan CLI resmi. `.graphifyignore` mengecualikan skill/tooling dan detail internal LAME agar graph menyorot aplikasi. `--force` diperlukan setelah pengurangan graph yang disengaja; tidak menghapus kode. Simpan output baru setelah perubahan. AST Kotlin/Compose dari versi ini melaporkan beberapa region parsial; ini keterbatasan parser Graphify, bukan hasil compiler. Gunakan graph sebagai peta, cek source dan build untuk callback/delegated state yang belum diekstrak. Jangan menyatakan graph lengkap atau mengganti bukti debugging dengan graph yang sudah kedaluwarsa.

Skill instructions tunduk pada brief pengguna dan instruksi keselamatan/privasi. Tidak menambah approval gate untuk keputusan teknis rutin. Review kode independen dilakukan melalui skill requesting-code-review; temuan penting diperbaiki dan dicek ulang. Jangan memasukkan password/key/token ke graph, log, screenshot atau source archive.
