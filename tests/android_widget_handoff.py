"""Capture widget handoff against a slow test destination on a disposable emulator.

The stand-in uses an absent supported package so the production APK needs no
test hook. Refuses to overwrite an existing Meituan or widget test host. Only
fixed synthetic text is submitted; probe logs contain no URI or query data.
"""
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import time
import xml.etree.ElementTree as ET

p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--adb',type=Path,required=True)
p.add_argument('--serial',required=True)
p.add_argument('--baseline',type=Path,required=True)
p.add_argument('--optimized',type=Path,required=True)
p.add_argument('--disposable-emulator',action='store_true')
p.add_argument('--trials',type=int,default=2)
p.add_argument('--checks-only',action='store_true',help='Run failure, draft and storage checks without re-recording captures')
a=p.parse_args()
if not a.disposable_emulator or not a.serial.startswith(('127.0.0.1:','emulator-')):
    raise SystemExit('An explicitly disposable local emulator is required')
root=Path(__file__).resolve().parents[1]
out=root/'build/handoff-capture';out.mkdir(parents=True,exist_ok=True)
host='org.nofeed.widgetfixture';target='com.sankuai.meituan'

def adb(*v):
    return subprocess.check_output([str(a.adb),'-s',a.serial,*map(str,v)],encoding='utf-8',errors='replace',timeout=60)
def sh(*v):return adb('shell',shlex.join(map(str,v))).strip()
def installed(package):return 'package:'+package in sh('pm','list','packages',package).splitlines()
def nodes():
    for _ in range(5):
        sh('rm','-f','/sdcard/yisou-handoff.xml')
        if 'dumped' in sh('uiautomator','dump','/sdcard/yisou-handoff.xml'):
            return list(ET.fromstring(sh('cat','/sdcard/yisou-handoff.xml')).iter('node'))
        time.sleep(.3)
    raise AssertionError('No fresh UI tree')
def bounds(n):return tuple(map(int,re.findall(r'\d+',n.get('bounds'))))
def find(text=None,desc=None):
    return next(n for n in nodes() if (text is None or n.get('text')==text) and (desc is None or n.get('content-desc')==desc) and bounds(n)[2]>bounds(n)[0])
def tap_node(n):
    x,y,x2,y2=bounds(n);sh('input','tap',(x+x2)//2,(y+y2)//2);time.sleep(.3)
def tap(text=None,desc=None):tap_node(find(text,desc))
def launch_host():
    sh('am','start','-W','-f','0x24000000','-n',host+'/fixture.MainActivity','--ei','widget_width','300')
    time.sleep(.6)
def open_input(label='美团'):
    launch_host()
    for _ in range(12):
        current=nodes()
        item=next((n for n in current if n.get('content-desc')==label+'，输入并搜索' and bounds(n)[2]-bounds(n)[0]>50),None)
        if item is not None:
            tap_node(item);break
        arrow=next(n for n in current if n.get('resource-id')=='org.nofeed.android:id/widget_next')
        tap_node(arrow)
    else:raise AssertionError('Cannot reach test destination through the real widget')
    find(text=label+' ▾');find(desc='快捷搜索关键词')
    sh('input','text','HandoffProbe')
    find(text='HandoffProbe',desc='快捷搜索关键词')
def capture_trial(label,cold):
    if cold:sh('am','force-stop',target)
    open_input()
    before=nodes()
    x,y,x2,y2=bounds(next(n for n in before if n.get('content-desc')=='提交快捷搜索'))
    submit_x,submit_y=(x+x2)//2,(y+y2)//2
    name=label.replace('-','_')
    sh('screencap','-p','/sdcard/yisou-handoff-before.png')
    adb('pull','/sdcard/yisou-handoff-before.png',out/(label+'-before.png'))
    device='/sdcard/yisou-handoff-'+label+'.mp4'
    recorder=subprocess.Popen([str(a.adb),'-s',a.serial,'shell','screenrecord','--bugreport',
        '--size','720x1280','--bit-rate','4000000','--time-limit','6',device],
        stdout=subprocess.PIPE,stderr=subprocess.STDOUT,
        creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
    try:
        time.sleep(1)
        # Same-shell markers bound a real submit tap without logging its text.
        # Hardware Enter in some emulator IMEs only dismisses the keyboard;
        # do not mistake that for the phone IME_ACTION_SEARCH callback.
        sh('sh','-c','log -t YisouHandoffMeasure '+name+'_PRE; input tap '+str(submit_x)+' '+str(submit_y)+'; log -t YisouHandoffMeasure '+name+'_POST')
        output,_=recorder.communicate(timeout=12)
        assert recorder.returncode==0,output.decode('utf-8',errors='replace')
    finally:
        if recorder.poll() is None:
            recorder.terminate();recorder.wait(timeout=5)
    adb('pull',device,out/(label+'.mp4'))
    current=nodes()
    assert any(n.get('text','').startswith('HANDOFF PROBE') for n in current),'Destination did not render'
    assert not any(n.get('content-desc')=='快捷搜索关键词' for n in current),'Input window remained foreground'
    return {'label':label,'cold':cold,'before_query_bounds':bounds(next(n for n in before if n.get('content-desc')=='快捷搜索关键词'))}

assert not installed(target),'Real Meituan is installed; refusing to replace it'
assert not installed(host),'Existing widget host must not be overwritten'
assert (root/'build/handoff-fixture/handoff-probe.apk').is_file()
assert (root/'build/widget-fixture/fixture.apk').is_file()
keyboard=sh('settings','get','secure','show_ime_with_hard_keyboard')
probe_added=host_added=False
results=[]
try:
    adb('install',root/'build/handoff-fixture/handoff-probe.apk');probe_added=True
    adb('install',root/'build/widget-fixture/fixture.apk');host_added=True
    sh('appwidget','grantbind','--package',host,'--user','0')
    sh('settings','put','secure','show_ime_with_hard_keyboard','1')
    if not a.checks_only:
        for variant,apk in [('baseline',a.baseline),('optimized',a.optimized)]:
            adb('install','-r',apk.resolve())
            for i in range(a.trials):
                results.append(capture_trial(variant+'-cold-'+str(i+1),True))
                print('PASS '+variant+' cold handoff '+str(i+1),flush=True)
            results.append(capture_trial(variant+'-warm',False))
            print('PASS '+variant+' warm handoff',flush=True)
    else:adb('install','-r',a.optimized.resolve())
    # Failed native resolution must preserve the editor and allow another try.
    adb('uninstall',target);probe_added=False
    open_input();tap(desc='提交快捷搜索')
    find(text='无法直达美团');tap(text='知道了')
    find(text='HandoffProbe',desc='快捷搜索关键词')
    tap(desc='提交快捷搜索');find(text='无法直达美团');tap(text='知道了')
    find(text='HandoffProbe',desc='快捷搜索关键词');tap(desc='关闭快捷搜索')
    print('PASS failed launches preserve the draft and release the submit guard',flush=True)
    if not installed('com.tencent.mm'):
        open_input('微信');tap(desc='提交快捷搜索');find(text='请先安装微信。');tap(text='知道了')
        find(text='HandoffProbe',desc='快捷搜索关键词');tap(desc='关闭快捷搜索')
        print('PASS missing-WeChat prompt preserves the draft',flush=True)
    denied=subprocess.run([str(a.adb),'-s',a.serial,'shell','am','start','-n','org.nofeed.android/.WidgetSearchActivity'],capture_output=True,text=True,encoding='utf-8',errors='replace')
    assert 'Permission Denial' in denied.stdout+denied.stderr
    print('PASS widget input stays private',flush=True)
    sh('am','start','-W','-n',host+'/org.nofeed.android.LocalStoreProbeActivity')
    find(desc='LOCAL_STORE_PASS')
    print('PASS production history, count, cap and disabled-history storage checks',flush=True)
finally:
    logs=sh('logcat','-d','-v','epoch','-s','YisouHandoffMeasure:I','YisouHandoffProbe:I','*:S')
    log_name='checks-timing.log' if a.checks_only else 'timing.log'
    (out/log_name).write_text(logs,encoding='utf-8')
    if results:(out/'trials.json').write_text(json.dumps(results,ensure_ascii=False,indent=2),encoding='utf-8')
    if keyboard=='null':sh('settings','delete','secure','show_ime_with_hard_keyboard')
    else:sh('settings','put','secure','show_ime_with_hard_keyboard',keyboard)
    if host_added:
        sh('appwidget','revokebind','--package',host,'--user','0');adb('uninstall',host)
    if probe_added:adb('uninstall',target)
    adb('install','-r',a.optimized.resolve())
print('Handoff captures complete; timing and window disappearance require video analysis.',flush=True)
