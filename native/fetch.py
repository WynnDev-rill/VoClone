#!/usr/bin/env python3
"""Verified pinned runtime sources only; never downloads model weights."""
import hashlib, json, pathlib, shutil, tarfile, urllib.request
ROOT = pathlib.Path(__file__).resolve().parent
PINS = json.loads((ROOT / 'pins.json').read_text())

def fetch_sources():
    build = ROOT / 'build'
    for name, pin in PINS.items():
        archive = build / 'downloads' / (name + '.tar.gz')
        archive.parent.mkdir(parents=True, exist_ok=True)
        if not archive.exists():
            temp = archive.with_suffix('.part')
            with urllib.request.urlopen(pin['url'], timeout=120) as source, temp.open('wb') as out:
                shutil.copyfileobj(source, out)
            temp.replace(archive)
        if hashlib.file_digest(archive.open('rb'), 'sha256').hexdigest() != pin['sha256']:
            archive.unlink()
            raise ValueError('Runtime checksum mismatch: ' + name)
        destination = build / 'sources' / name
        if (destination / '.voclone-revision').exists() and (destination / '.voclone-revision').read_text() == pin['revision']:
            continue
        temporary = build / ('extract-' + name)
        shutil.rmtree(temporary, ignore_errors=True)
        temporary.mkdir(parents=True)
        with tarfile.open(archive) as source:
            source.extractall(temporary, filter='data')
        entries = list(temporary.iterdir())
        if len(entries) != 1:
            raise ValueError('Unexpected archive layout')
        shutil.rmtree(destination, ignore_errors=True)
        destination.parent.mkdir(exist_ok=True)
        entries[0].rename(destination)
        temporary.rmdir()
        (destination / '.voclone-revision').write_text(pin['revision'])
    ggml = build / 'sources/omnivoice/ggml'
    if ggml.exists() and not ggml.is_symlink():
        shutil.rmtree(ggml)
    if not ggml.exists():
        ggml.symlink_to('../ggml', target_is_directory=True)
    # VoClone patch: at most four CPU workers. Prevents oversubscription on hosts
    # and excessive concurrent big/LITTLE work on mobile. Upstream otherwise uses logical/2.
    backend = build / 'sources/omnivoice/src/backend.h'
    text = backend.read_text()
    text = text.replace('return n > 0 ? n : 1;', 'return n > 4 ? 4 : (n > 0 ? n : 1); // VoClone: bounded CPU workers')
    if backend.read_text()!=text:backend.write_text(text)
    # VoClone: preserve old CPU work buffer if the replacement allocation fails.
    for source in (build/'sources/ggml/src/ggml-cpu/ggml-cpu.cpp', build/'sources/whisper/ggml/src/ggml-cpu/ggml-cpu.cpp'):
        text=source.read_text()
        old='        delete[] cpu_ctx->work_data;\n        cpu_ctx->work_data = new uint8_t[cplan.work_size];'
        new='        // VoClone OOM-safe replacement; retain the old buffer on failure.\n        auto* replacement = new(std::nothrow) uint8_t[cplan.work_size];\n        if (!replacement) return GGML_STATUS_ALLOC_FAILED;\n        delete[] cpu_ctx->work_data;\n        cpu_ctx->work_data = replacement;'
        if old in text:
            text=text.replace('#include <vector>', '#include <vector>\n#include <new>').replace(old,new)
            source.write_text(text)
        else:
            assert '// VoClone OOM-safe replacement' in text
    from patch_omni import patch
    patch(build / 'sources/omnivoice')
    # Stable stamp for archives without .git; never report the enclosing app commit.
    (build / 'sources/omnivoice/tools/version.cmake').write_text('file(WRITE "${OUTPUT}" "#pragma once\\n#define OMNIVOICE_VERSION \\"'+PINS['omnivoice']['revision']+'\\"\\n")\n')

if __name__ == '__main__':
    fetch_sources()
