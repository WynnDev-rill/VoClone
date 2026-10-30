#!/usr/bin/env python3
"""Generate owner release assets; never includes signing keys or model weights."""
import hashlib, json, os, pathlib, re, shutil, subprocess, tarfile, tempfile
root=pathlib.Path(__file__).resolve().parents[1];os.chdir(root)
subprocess.run(['git','diff','--quiet','HEAD','--'],check=True)
assert not subprocess.check_output(['git','ls-files','--others','--exclude-standard']).strip(), 'Commit all source files before packaging'
commit=subprocess.check_output(['git','rev-parse','HEAD'],text=True).strip()
assert (root/'app/src/main/AndroidManifest.xml').is_file()

build=(root/'app/build.gradle.kts').read_text();version=re.search(r'versionName = "([^"]+)"',build).group(1);code=int(re.search(r'versionCode = (\d+)',build).group(1))
out=root/'distribution';out.mkdir(exist_ok=True)
apk=out/f'VoClone-{version}-arm64.apk';shutil.copy2(root/'app/build/outputs/apk/release/app-release.apk',apk)
sha=hashlib.file_digest(apk.open('rb'),'sha256').hexdigest()
manifest=dict(versionCode=code,versionName=version,packageName='id.wynn.voclone',minSdk=29,apkUrl=f'https://github.com/WynnDev-rill/Wynn-Store/releases/download/voclone-v{version}/{apk.name}',sha256=sha,signerSha256=(root/'docs/signing-sha256.txt').read_text().strip(),sizeBytes=apk.stat().st_size,sourceCommit=commit,notes='VoClone alpha: cloning lokal OmniVoice + Whisper. Baca laporan verifikasi; kualitas dan stabilitas POCO F6 memerlukan uji perangkat fisik.')
(out/'alpha.json').write_text(json.dumps(manifest,indent=2)+'\n')
(out/(apk.name+'.sha256')).write_text(sha+'  '+apk.name+'\n')
source=out/f'VoClone-{version}-source.tar.gz'
with tempfile.TemporaryDirectory() as d:
 stage=pathlib.Path(d)/'VoClone';stage.mkdir()
 archive=pathlib.Path(d)/'tracked.tar'
 with archive.open('wb') as f:subprocess.run(['git','archive','HEAD'],stdout=f,check=True)
 with tarfile.open(archive) as t:t.extractall(stage,filter='data')
 (stage/'SOURCE_COMMIT.txt').write_text(commit+'\n')
 for folder in ('sources','downloads'):
  # Prepared runtime source and original checksum-verified archives: full corresponding source.
  shutil.copytree(root/'native/build'/folder,stage/'native/build'/folder,symlinks=True)
 with tarfile.open(source,'w:gz') as t:t.add(stage,arcname='VoClone')
print(apk.name,apk.stat().st_size,source.name,source.stat().st_size)
