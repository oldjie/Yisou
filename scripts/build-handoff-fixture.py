"""Build a test-only slow destination. This script never installs an APK.

The stand-in deliberately uses com.xunmeng.pinduoduo so an unmodified production
APK can exercise its normal Pinduoduo dispatch path. Before any installation,
confirm that `pm list packages com.xunmeng.pinduoduo` contains NO exact package on
an explicitly disposable emulator. Refuse to proceed if real Pinduoduo is present;
never replace it, never use install -r for this probe, and never uninstall an
existing original application to make room. Remove only the probe installed by
that test run. Do not publish this APK to a Release or distribute it to users.

Uses the existing local SDK, JAVA_HOME and .tools/development.p12. Resources are
generated inside build/handoff-fixture so no production source needs a test hook.
"""
import os
from pathlib import Path
import shutil
import subprocess
import zipfile

root = Path(__file__).resolve().parents[1]
sdk = root / '.tools/sdk'
tools = sdk / 'build-tools/35.0.0'
if not tools.is_dir():
    tools = sdk / 'android-15'
platform = sdk / 'platforms/android-35/android.jar'
if not platform.is_file():
    platform = sdk / 'android-35/android.jar'
java_home = os.environ.get('JAVA_HOME')
if not java_home:
    raise SystemExit('Set JAVA_HOME to an existing JDK 17 or newer.')
jdk = Path(java_home) / 'bin'
key = root / '.tools/development.p12'
for required in (tools / 'aapt2.exe', platform, jdk / 'javac.exe', key):
    if not required.is_file():
        raise SystemExit('Missing existing build input: ' + str(required))

out = root / 'build/handoff-fixture'
if out.resolve() != out.absolute():
    raise SystemExit('Refusing an output directory redirected through a symlink.')
out.mkdir(parents=True, exist_ok=True)
for name in ('classes', 'dex', 'generated', 'res'):
    folder = out / name
    if folder.is_symlink() or not folder.resolve().is_relative_to(out.resolve()):
        raise SystemExit('Build cleanup escaped its output directory.')
    if folder.exists():
        shutil.rmtree(folder)
    folder.mkdir()

styles = out / 'res/values/styles.xml'
styles.parent.mkdir()
styles.write_text('''<resources>
    <style name="HandoffProbeTheme" parent="android:style/Theme.Material.Light.NoActionBar">
        <item name="android:windowDisablePreview">true</item>
        <item name="android:windowBackground">#0000FF</item>
        <item name="android:statusBarColor">#0000FF</item>
        <item name="android:navigationBarColor">#0000FF</item>
        <item name="android:windowLightStatusBar">false</item>
        <item name="android:windowLightNavigationBar">false</item>
        <item name="android:fontFamily">sans-serif</item>
    </style>
</resources>
''', encoding='utf-8')

def run(*args):
    subprocess.run([str(arg) for arg in args], check=True)

run(tools / 'aapt2.exe', 'compile', '--dir', out / 'res', '-o', out / 'resources.zip')
run(tools / 'aapt2.exe', 'link', '-o', out / 'base.apk', '-I', platform,
    '--manifest', root / 'tests/handoff_fixture/AndroidManifest.xml',
    '--java', out / 'generated', '--min-sdk-version', '26', '--target-sdk-version', '35',
    '--version-code', '1', '--version-name', 'test-only', out / 'resources.zip')
run(jdk / 'javac.exe', '-encoding', 'UTF-8', '-source', '8', '-target', '8',
    '-bootclasspath', str(platform) + ';' + str(tools / 'core-lambda-stubs.jar'),
    '-d', out / 'classes', root / 'tests/handoff_fixture/SlowTargetActivity.java')
run(jdk / 'java.exe', '-cp', tools / 'lib/d8.jar', 'com.android.tools.r8.D8',
    '--min-api', '26', '--lib', platform, '--output', out / 'dex',
    *sorted((out / 'classes').rglob('*.class')))
shutil.copyfile(out / 'base.apk', out / 'unsigned.apk')
with zipfile.ZipFile(out / 'unsigned.apk', 'a') as archive:
    archive.write(out / 'dex/classes.dex', 'classes.dex')
run(tools / 'zipalign.exe', '-f', '4', out / 'unsigned.apk', out / 'aligned.apk')
run(jdk / 'java.exe', '-jar', tools / 'lib/apksigner.jar', 'sign', '--ks', key,
    '--ks-pass', 'pass:android', '--ks-key-alias', 'development',
    '--out', out / 'handoff-probe.apk', out / 'aligned.apk')
run(jdk / 'java.exe', '-jar', tools / 'lib/apksigner.jar', 'verify', out / 'handoff-probe.apk')
print('TEST ONLY: ' + str(out / 'handoff-probe.apk'))
print('No installation performed. Never replace real com.xunmeng.pinduoduo.')
