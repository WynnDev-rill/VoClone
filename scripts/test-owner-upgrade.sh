#!/usr/bin/env bash
# Rooted disposable Google APIs emulator only; no root permission is added to VoClone.
set -euo pipefail
cd "${VOCLONE_SOURCE_ROOT:-$(dirname "$0")/..}"
old="$1";new="$2";preview="$3"
mkdir -p qa/upgrade
adb root
adb wait-for-device
test "$(adb shell id -u | tr -d '\r')" = 0
adb install -r "$old"
adb shell am start -W -n id.wynn.voclone/.MainActivity > qa/upgrade/start-v1.txt
adb shell am force-stop id.wynn.voclone
fixture="${RUNNER_TEMP:-/tmp}/voclone-upgrade-fixture"
mkdir -p "$fixture"
python3 - "$fixture" <<'PY'
import pathlib,sys,json
p=pathlib.Path(sys.argv[1]);voice='12345678-1234-4234-8234-123456789abc'
(p/'profile.json').write_text(json.dumps(dict(id=voice,name='QA Upgrade Voice',transcript='And so my fellow Americans.',duration=3.2,createdAt=1793491200000)))
(p/'studio.xml').write_text(f'<?xml version="1.0" encoding="utf-8" standalone="yes" ?>\n<map><string name="voice">{voice}</string><string name="theme">dark</string><string name="qaUpgradeSentinel">owner-v1-preserved</string></map>')
(p/'model-sentinel.txt').write_text('Upgrade storage sentinel; not a model, never used for inference.\n')
PY
voice_dir=/data/user/0/id.wynn.voclone/files/voices/12345678-1234-4234-8234-123456789abc
prefs=/data/user/0/id.wynn.voclone/shared_prefs/studio.xml
sentinel=/data/user/0/id.wynn.voclone/no_backup/models/qa-upgrade-sentinel.txt
adb shell mkdir -p "$voice_dir" /data/user/0/id.wynn.voclone/shared_prefs /data/user/0/id.wynn.voclone/no_backup/models
adb push "$fixture/profile.json" "$voice_dir/profile.json"
adb push app/src/androidTest/assets/reference.wav "$voice_dir/reference.wav"
adb push "$preview" "$voice_dir/preview.wav"
adb push "$fixture/studio.xml" "$prefs"
adb push "$fixture/model-sentinel.txt" "$sentinel"
app_uid="$(adb shell stat -c %u /data/user/0/id.wynn.voclone | tr -d '\r')"
adb shell chown -R "$app_uid:$app_uid" /data/user/0/id.wynn.voclone/files /data/user/0/id.wynn.voclone/shared_prefs /data/user/0/id.wynn.voclone/no_backup
adb shell restorecon -RF /data/user/0/id.wynn.voclone
adb shell sha256sum "$voice_dir/profile.json" "$voice_dir/reference.wav" "$voice_dir/preview.wav" "$sentinel" > qa/upgrade/before.sha256
show_profile() {
  adb shell am start -W -n id.wynn.voclone/.MainActivity
  for attempt in 1 2 3 4; do
    adb shell uiautomator dump /sdcard/voclone-upgrade.xml >/dev/null
    adb pull /sdcard/voclone-upgrade.xml "qa/upgrade/ui-$1.xml" >/dev/null
    if grep -q 'QA Upgrade Voice' "qa/upgrade/ui-$1.xml"; then return; fi
    adb shell input swipe 450 1500 450 400 600
  done
  echo 'Profile missing from actual app UI';return 1
}
show_profile v1
adb shell am force-stop id.wynn.voclone
adb install -r "$new" | tee qa/upgrade/install-v2.txt
adb shell dumpsys package id.wynn.voclone > qa/upgrade/package-v2.txt
grep -q 'versionCode=2' qa/upgrade/package-v2.txt
adb shell sha256sum "$voice_dir/profile.json" "$voice_dir/reference.wav" "$voice_dir/preview.wav" "$sentinel" > qa/upgrade/after.sha256
diff -u qa/upgrade/before.sha256 qa/upgrade/after.sha256
adb shell cat "$prefs" > qa/upgrade/prefs-v2.xml
grep -q 'owner-v1-preserved' qa/upgrade/prefs-v2.xml
show_profile v2
adb shell cat "$prefs" | grep -q 'name="theme">dark'
printf '%s\n' 'PASS original-owner v1 -> v2 install -r, actual UI profile visible in both versions, profile/reference/real preview/storage-sentinel SHA preserved, preferences retained. Disposable rooted emulator; not physical hardware or a real model download test.' > qa/upgrade/result.txt
