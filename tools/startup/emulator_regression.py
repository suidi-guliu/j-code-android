#!/usr/bin/env python3
"""Actual shipping APK startup regression on an OWNED, initially empty Android Emulator only.
Never runs on a physical device, never uninstalls/clears app data, never recreates installed Ubuntu.
Use a separate fresh AVD for --mode fresh. ARM64 shipping APK runs using Google's native bridge.
"""
from pathlib import Path
import argparse,subprocess,time,hashlib,json,shlex,sys,traceback,xml.etree.ElementTree as ET
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--mode',choices=['upgrade','fresh'],required=True);p.add_argument('--fixtures',type=Path,required=True);p.add_argument('--fixed',type=Path,required=True);p.add_argument('--tests',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True)
a=p.parse_args();a.evidence.mkdir(parents=True,exist_ok=True)
repo=Path(__file__).resolve().parents[2];pkg='dev.suicode.ide.debug';activity=pkg+'/dev.blamspot.jcode.MainActivity';log=[]
def failure_hook(kind,error,tb):
 text=''.join(traceback.format_exception(kind,error,tb))
 (a.evidence/'failure.txt').write_text(text)
 try:
  r=subprocess.run(['adb','-s',a.serial,'logcat','-b','crash','-d','-v','brief'],text=True,capture_output=True,timeout=30)
  (a.evidence/'failure-crash.log').write_text(r.stdout+r.stderr)
 except Exception: pass
 try:
  xmls=sorted(a.evidence.glob('*.xml'),key=lambda p:p.stat().st_mtime)
  if xmls:
   nodes=[{k:v for k,v in n.attrib.items() if k in ['text','content-desc','class','bounds','package']} for n in ET.fromstring(xmls[-1].read_text()).iter('node') if n.attrib.get('text') or n.attrib.get('content-desc')]
   text+='\nLATEST_UI_NODES '+json.dumps(nodes,ensure_ascii=False)[-5000:]
 except Exception: pass
 print('::error title=Android startup regression::'+text[-6500:].replace('%','%25').replace('\r','%0D').replace('\n','%0A'),flush=True)
 traceback.print_exception(kind,error,tb)
sys.excepthook=failure_hook

def note(s):
 print(s,flush=True);log.append(s);(a.evidence/'steps.log').write_text('\n'.join(log)+'\n')
def adb(*args,check=True,timeout=120,binary=False,input=None):
 r=subprocess.run(['adb','-s',a.serial,*map(str,args)],capture_output=True,text=not binary,input=input,timeout=timeout)
 if check and r.returncode:raise RuntimeError(f'adb {args}: {r.stdout!r} {r.stderr!r}')
 return r
assert a.serial.startswith('emulator-') and adb('shell','getprop ro.kernel.qemu').stdout.strip()=='1','Refuse to modify a physical device'
assert not adb('shell','pm path '+pkg,check=False).stdout.strip(),'Use a new owned AVD; target app already installed'
platform=adb('shell','getprop').stdout;(a.evidence/'platform.txt').write_text(platform)
note('Platform: Android emulator; not ARM64 physical acceptance')
adb('shell','wm size 1280x800');adb('shell','wm density 160')
def install(path):
 result=adb('install','--no-streaming','-r',path,timeout=240);note('INSTALL '+str(path)+': '+result.stdout.strip());assert 'Success' in result.stdout
 adb('shell',f'pm grant {pkg} android.permission.POST_NOTIFICATIONS',check=False)
def shellrun(command,**kwargs):return adb('shell','run-as '+pkg+' sh -c '+shlex.quote(command),**kwargs)
def snapshot(name):
 (a.evidence/(name+'.png')).write_bytes(adb('exec-out','screencap -p',binary=True).stdout)
 adb('shell','uiautomator dump --compressed /data/local/tmp/sui-startup-ui.xml',timeout=90)
 xml=adb('shell','cat /data/local/tmp/sui-startup-ui.xml').stdout;(a.evidence/(name+'.xml')).write_text(xml)
 return xml
def alive(name,project=False):
 adb('shell','am force-stop '+pkg)
 result=adb('shell','am start -W -n '+activity,timeout=180);(a.evidence/(name+'-start.txt')).write_text(result.stdout+result.stderr)
 deadline=time.monotonic()+90
 while True:
  assert adb('shell','pidof '+pkg).stdout.strip(),'Activity process did not survive'
  xml=snapshot(name)
  nodes=list(ET.fromstring(xml).iter('node'))
  ready=any(n.attrib.get('package')==pkg and n.attrib.get('text') in ['Run file','Internal Files','Environment setup'] for n in nodes)
  if ready:break
  assert time.monotonic()<deadline,'Neither the normal workbench nor existing first-run environment screen is visible'
  time.sleep(2)
 note('FIRST_FRAME_PASS '+name+(' (existing environment onboarding: no installed Linux runtime)' if 'Environment setup' in xml else ' (workbench)'))
 return xml
def hashes(label):
 expected=json.loads((a.fixtures/'payload-sha256.json').read_text())
 for path,digest in expected.items():
  result=adb('shell','run-as '+pkg+' sha256sum '+shlex.quote(path)).stdout
  assert result.split()[0]==digest,(label,path,result,digest)
 note('DATA_HASH_PASS '+label+' '+str(len(expected))+' files')
def click(xml,predicate):
 for node in ET.fromstring(xml).iter('node'):
  if predicate(node.attrib):
   import re
   n=list(map(int,re.findall(r'\d+',node.attrib['bounds'])));adb('shell',f'input tap {(n[0]+n[2])//2} {(n[1]+n[3])//2}');time.sleep(1);return
 raise AssertionError('UI target missing')
if a.mode=='upgrade':
 install(repo/'.delivery/sui-code-v0.5/sui-code-v0.5-debug.apk')
 alive('v050-fresh')
 adb('shell','am force-stop '+pkg)
 adb('push',a.fixtures/'payload.tar','/data/local/tmp/sui-startup-payload.tar')
 shellrun('tar -xf /data/local/tmp/sui-startup-payload.tar')
 hashes('v050-before-open')
 xml=alive('v050-with-data')
 # Register a real optional Project through the unchanged v0.5 ViewModel API.
 # Its legitimate environment onboarding is kept: this does not fake an installed Ubuntu.
 install(a.tests)
 created=adb('shell','am instrument -w -r -e class dev.blamspot.jcode.BaselineDataTest#createRegisteredProject dev.suicode.ide.debug.test/androidx.test.runner.AndroidJUnitRunner',timeout=180)
 (a.evidence/'v050-project-create.txt').write_text(created.stdout+created.stderr)
 assert 'OK (1 test)' in created.stdout,created.stdout
 note('ORIGINAL_PRODUCTION_PROJECT_CREATED')
 hashes('v050-registered')
 adb('shell','am force-stop '+pkg)
 install(repo/'.delivery/sui-code-v0.5-ui-polish/sui-code-v0.5.1-ui-polish-debug.apk')
 adb('shell','am start -W -n '+activity,check=False,timeout=180);time.sleep(7)
 crash=adb('logcat','-b','crash','-d','-v','threadtime').stdout;(a.evidence/'v051-original-crash.log').write_text(crash)
 assert 'PatternSyntaxException' in crash and 'UiTranslations.text' in crash,'Original Activity did not reproduce the expected shipping crash'
 note('ORIGINAL_V051_ACTIVITY_CRASH_REPRODUCED')
 hashes('v051-after-crash')
 adb('shell','am force-stop '+pkg)
install(a.fixed)
for i in range(3):alive(a.mode+'-fixed-'+str(i+1),project=a.mode=='upgrade')
if a.mode=='upgrade':
 hashes('fixed-after-three-starts')
 verified=adb('shell','am instrument -w -r -e class dev.blamspot.jcode.BaselineDataTest#verifyRegisteredProjectSurvives dev.suicode.ide.debug.test/androidx.test.runner.AndroidJUnitRunner',timeout=180)
 (a.evidence/'registered-project-retained.txt').write_text(verified.stdout+verified.stderr)
 assert 'OK (1 test)' in verified.stdout,verified.stdout
 note('REGISTERED_PROJECT_METADATA_RETAINED')
# A real Android preference type mismatch must not block composition; original inputs are retained.
adb('shell','am force-stop '+pkg)
for name,xml in [('sui-native-ui-language','<?xml version="1.0" encoding="utf-8"?><map><int name="selection-v1" value="7"/><string name="untouched">keep</string></map>'),('sui-workbench-layout','<?xml version="1.0" encoding="utf-8"?><map><string name="content-width-v1">broken</string><string name="untouched">keep</string></map>')]:
 shellrun('mkdir -p shared_prefs; printf %s '+shlex.quote(xml)+' > shared_prefs/'+name+'.xml')
alive(a.mode+'-bad-ui-preferences',project=a.mode=='upgrade')
assert 'broken' in shellrun('cat shared_prefs/sui-workbench-layout.xml').stdout
assert 'untouched' in shellrun('cat shared_prefs/sui-native-ui-language.xml').stdout
note('BAD_UI_PREFERENCES_FALLBACK_PASS (files retained)')
install(a.tests)
result=adb('shell','am instrument -w -r -e class dev.blamspot.jcode.StartupRuntimeTest dev.suicode.ide.debug.test/androidx.test.runner.AndroidJUnitRunner',timeout=1200)
(a.evidence/'instrumentation.txt').write_text(result.stdout+result.stderr)
assert 'OK (4 tests)' in result.stdout,result.stdout
note('ANDROID_INSTRUMENTATION_PASS 4 tests')
if a.mode=='upgrade':hashes('after-instrumentation')
(a.evidence/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime','-s','AndroidRuntime','SuiStartup','SuiStartupTest').stdout)
for name in ['startup.log','startup.previous.log']:
 r=shellrun('cat files/sui-code/diagnostics/'+name,check=False)
 if r.returncode==0:(a.evidence/name).write_text(r.stdout)
(a.evidence/'result.json').write_text(json.dumps({'mode':a.mode,'result':'PASS','physical_arm64':False,'instrumentation_tests':4},indent=2)+'\n')
note('COMPLETE '+a.mode)
