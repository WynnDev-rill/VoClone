#!/usr/bin/env python3
"""Publish only an explicitly requested signed alpha in the source repository."""
import json,pathlib,subprocess
root=pathlib.Path(__file__).resolve().parents[1]
commit=subprocess.check_output(['git','rev-parse','HEAD'],cwd=root,text=True).strip()
metadata=json.loads((root/'distribution/alpha.json').read_text());version=metadata['versionName']
assert metadata.get('sourceCommit')==commit, 'Release assets must match this exact committed source'
assert 'alpha' in version, 'Stable release needs explicit physical-device gate implementation/review'
# An existing lightweight/annotated tag must resolve to the exact built commit.
repo='WynnDev-rill/VoClone'
tag='v'+version
check=subprocess.run(['gh','api',f'repos/{repo}/git/ref/tags/{tag}'],capture_output=True,text=True)
if check.returncode==0:
    obj=json.loads(check.stdout)['object']
    while obj['type']=='tag':obj=json.loads(subprocess.check_output(['gh','api',f'repos/{repo}/git/tags/'+obj['sha']]))['object']
    assert obj['sha']==commit, 'Existing release tag points at a different source commit'
elif 'HTTP 404' not in check.stderr:
    raise SystemExit(check.stderr)
notes=root/'distribution/RELEASE_NOTES.md'
notes.write_text(f'''VoClone {version} — alpha Android ARM64\n\nNative offline OmniVoice + Whisper; model files downloaded separately after licence acceptance.\n\nThis alpha is not a physical-device quality certification. Read docs/VERIFICATION.md and docs/DEVICE_TEST.md in the corresponding source archive. Indonesian/Japanese speaker similarity, POCO F6 speed/thermals, Android 16 background execution and physical-device upgrade rehearsal remain device gates unless that report contains actual evidence.\n\nAPK signer SHA-256: `{metadata['signerSha256']}`\nAPK SHA-256: `{metadata['sha256']}`\n\nSources and bundled dependency licences are supplied with this release. No signing keys or model weights are included.\n''')
subprocess.run(['gh','release','create','v'+version,'--repo','WynnDev-rill/VoClone','--target',commit,'--prerelease','--title','VoClone '+version,'--notes-file',str(notes),*map(str,[root/'distribution'/name for name in (f'VoClone-{version}-arm64.apk',f'VoClone-{version}-arm64.apk.sha256',f'VoClone-{version}-source.tar.gz',f'VoClone-{version}-verification.md','alpha.json','RELEASE_NOTES.md')])],check=True)

# Feed is the last mutation: APK/source must already be publicly downloadable and identical.
subprocess.run(['python3',str(root/'scripts/publish-update-feed.py')],check=True)
