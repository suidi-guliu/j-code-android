#!/usr/bin/env python3
"""Shipping APK + standalone Kit UI regression. OWNED new AVDs only; never uninstall/clear target app.
No real Ubuntu/toolchain is installed: fixtures prove byte retention, not runtime execution.
"""
import argparse,subprocess,shlex,json,time,re,traceback,sys,xml.etree.ElementTree as ET
from pathlib import Path
p=argparse.ArgumentParser();p.add_argument('--serial',required=True);p.add_argument('--mode',choices=['upgrade','fresh'],required=True);p.add_argument('--inputs',type=Path,required=True);p.add_argument('--apk',type=Path,required=True);p.add_argument('--kit',type=Path,required=True);p.add_argument('--evidence',type=Path,required=True);a=p.parse_args()
root=Path(__file__).resolve().parents[2];a.evidence.mkdir(parents=True,exist_ok=True);pkg='dev.suicode.ide.debug';activity=pkg+'/dev.blamspot.jcode.MainActivity';kitid='dev.suicode.localization.zh-hans';serial=a.serial
steps=[];last_xml='';WIDTH=1280;HEIGHT=800

def adb(*args,check=True,timeout=120,binary=False):
 r=subprocess.run(['adb','-s',serial,*map(str,args)],capture_output=True,text=not binary,timeout=timeout)
 if check and r.returncode:raise RuntimeError(f'adb {args}: {r.stdout!r} {r.stderr!r}')
 return r

def note(message):
 print(message,flush=True);steps.append(message);(a.evidence/'steps.log').write_text('\n'.join(steps)+'\n')
def fail_hook(kind,error,tb):
 text=''.join(traceback.format_exception(kind,error,tb))
 (a.evidence/'failure.txt').write_text(text)
 try:
  log=adb('logcat','-d','-v','brief',timeout=30).stdout
  (a.evidence/'failure-logcat.txt').write_text(log)
  lines=log.splitlines();fatal=[]
  for i,line in enumerate(lines):
   if 'FATAL EXCEPTION' in line or ('Fatal signal' in line and pkg in line):fatal.extend(lines[max(0,i-2):i+45])
  text+='\nANDROID_FATAL '+('\n'.join(fatal)[-7500:] or 'No fatal exception found in captured logcat')
  launches=sorted(a.evidence.glob('*-launch.txt'),key=lambda p:p.stat().st_mtime)
  if launches:text+='\nLATEST_LAUNCH '+launches[-1].read_text()
 except Exception as capture_error:text+='\nDIAGNOSTIC_CAPTURE '+str(capture_error)
 if last_xml:
  try:text+='\nLATEST_UI '+json.dumps([{k:v for k,v in n.attrib.items() if k in ['text','content-desc','bounds','enabled','checked','checkable']} for n in ET.fromstring(last_xml).iter('node') if n.attrib.get('text') or n.attrib.get('content-desc')],ensure_ascii=False)[-9500:]
  except Exception:pass
 print('::error title=Chinese Kit Android UI regression::'+text[-22000:].replace('%','%25').replace('\r','%0D').replace('\n','%0A'),flush=True)
 traceback.print_exception(kind,error,tb)
sys.excepthook=fail_hook
assert serial.startswith('emulator-') and adb('shell','getprop ro.kernel.qemu').stdout.strip()=='1','Refuse physical device'
assert not adb('shell','pm path '+pkg,check=False).stdout.strip(),'Use a new owned AVD'
def install(path):
 r=adb('install','--no-streaming','-r',path,timeout=240);assert 'Success' in r.stdout,r.stdout;note('INSTALL '+str(path))
 adb('shell','pm grant '+pkg+' android.permission.POST_NOTIFICATIONS',check=False)
def runas(command,check=True):return adb('shell','run-as '+pkg+' sh -c '+shlex.quote(command),check=check)
def xml():
 global last_xml
 # Android may report a successful dump without creating a file during Activity recreation.
 # Retry only capture, always delete the prior file, and never accept a stale hierarchy.
 attempts=[]
 for attempt in range(5):
  adb('shell','rm -f /data/local/tmp/sui-zh-ui.xml')
  dump=adb('shell','uiautomator dump --compressed /data/local/tmp/sui-zh-ui.xml',timeout=90,check=False)
  captured=adb('shell','cat /data/local/tmp/sui-zh-ui.xml',check=False)
  if dump.returncode==0 and captured.returncode==0:
   tree=ET.fromstring(captured.stdout);last_xml=captured.stdout;return tree
  attempts.append({'attempt':attempt+1,'dump':dump.stdout+dump.stderr,'capture':captured.stderr})
  time.sleep(1)
 raise RuntimeError('Fresh Android UI hierarchy capture failed: '+json.dumps(attempts))
def matches(tree,label,contains=False):
 return [n for n in tree.iter('node') if (label in n.attrib.get('text','') or label in n.attrib.get('content-desc',''))] if contains else [n for n in tree.iter('node') if label in [n.attrib.get('text'),n.attrib.get('content-desc')]]
def bounds(n):return list(map(int,re.findall(r'\d+',n.attrib['bounds'])))
def tap_node(n):
 x1,y1,x2,y2=bounds(n);assert x2>x1 and y2>y1,n.attrib
 adb('shell',f'input tap {(x1+x2)//2} {(y1+y2)//2}');time.sleep(.7)
def swipe(pane=False):
 x=220 if pane else WIDTH//2
 top,bottom=100,HEIGHT-50
 if last_xml:
  candidates=[]
  for n in ET.fromstring(last_xml).iter('node'):
   if n.attrib.get('scrollable')=='true':
    x1,y1,x2,y2=bounds(n)
    if x1<x<x2 and y2-y1>90:candidates.append(((x2-x1)*(y2-y1),y1,y2))
  if candidates:_,top,bottom=max(candidates)
 start=min(bottom-24,int(HEIGHT*.84));end=max(top+20,start-min(220,int((bottom-top)*.40)))
 adb('shell',f'input swipe {x} {start} {x} {end} 350');time.sleep(.4)
def wait_text(label,contains=False,scroll=False,pane=False,timeout=50):
 deadline=time.monotonic()+timeout
 while True:
  tree=xml();found=matches(tree,label,contains)
  if found:return tree,found
  assert time.monotonic()<deadline,'Visible UI text missing: '+label
  if scroll:swipe(pane)
  else:time.sleep(1)
def tap(label,contains=False,scroll=False,pane=False,nav=False):
 tree,found=wait_text(label,contains,scroll,pane)
 if nav:found=sorted(found,key=lambda n:bounds(n)[0])
 tap_node(found[0]);return tree
def checkpoint(name,expected=()):
 tree=xml();texts=[n.attrib.get('text','') for n in tree.iter('node')]
 for value in expected:assert any(value in t for t in texts),(name,value,texts)
 (a.evidence/(name+'.xml')).write_text(last_xml)
 png=adb('exec-out','screencap -p',binary=True).stdout;assert png.startswith(b'\x89PNG\r\n\x1a\n')
 (a.evidence/(name+'.png')).write_bytes(png);note('SCREENSHOT_PASS '+name+' '+str(list(expected)))
 return tree
def alive(name,chinese=False):
 adb('shell','am force-stop '+pkg);r=adb('shell','am start -W -n '+activity,timeout=180)
 (a.evidence/(name+'-launch.txt')).write_text(r.stdout+r.stderr)
 deadline=time.monotonic()+60
 while True:
  assert adb('shell','pidof '+pkg).stdout.strip(),'Activity did not survive'
  tree=xml();labels=['运行文件','环境配置'] if chinese else ['Run file','Environment setup']
  if any(matches(tree,t) for t in labels):break
  assert time.monotonic()<deadline,'No actual app first frame';time.sleep(1)
 checkpoint(name)
def instrument(selector,name,expected):
 r=adb('shell',f'am instrument -w -r -e class {selector} dev.suicode.ide.debug.test/androidx.test.runner.AndroidJUnitRunner',timeout=1200)
 (a.evidence/(name+'.txt')).write_text(r.stdout+r.stderr)
 assert f'OK ({expected} test'+('s)' if expected!=1 else ')') in r.stdout,r.stdout
 assert 'INSTRUMENTATION_STATUS_CODE: -3' not in r.stdout,'No skipped cases permitted for owned fixtures'
 note('INSTRUMENTATION_PASS '+name+' '+str(expected))
def hashes(label):
 expected=json.loads((a.inputs/'payload-sha256.json').read_text())
 for name,digest in expected.items():assert runas('sha256sum '+shlex.quote(name)).stdout.split()[0]==digest,(label,name)
 note('DATA_HASH_PASS '+label+' '+str(len(expected)))
def ensure_workbench():
 tree=xml()
 if matches(tree,'Environment setup'):
  tap('Continue without environment',scroll=True)
 wait_text('Run file')
 checkpoint('english-workbench',['Run file','Overview','Packages'])
def open_packages(chinese=False):tap('扩展包' if chinese else 'Packages',nav=True);time.sleep(1)
def review():
 # The consent UI intentionally remains protected English, including in Chinese mode.
 wait_text('Unverified publisher',scroll=True)
 tree,_=wait_text('I trust this unverified publisher',scroll=True)
 checkboxes=[n for n in tree.iter('node') if n.attrib.get('checkable')=='true' and n.attrib.get('checked')=='false']
 assert checkboxes,'Expected an actual unchecked source-consent control'
 checkpoint('review-before-consent')
 # The source checkbox is the first one, followed by the independent capability control.
 tap_node(checkboxes[0])
 tap('Approve all listed permissions',scroll=True)
 tree,_=wait_text('ui.localization',contains=True,scroll=True)
 checkpoint('review-permissions-listed',['ui.localization'])
 tap('Confirm plan and install',scroll=True)
 deadline=time.monotonic()+60
 while True:
  tree=xml()
  if matches(tree,'Interface language') or matches(tree,'界面语言'):break
  assert time.monotonic()<deadline,'Package review did not return to the language selector';time.sleep(1)
 note('USER_SOURCE_AND_PERMISSION_CONSENT_COMPLETED')
def import_kit(chinese=False):
 open_packages(chinese)
 tap('导入 .suic / .suik' if chinese else 'Import .suic / .suik')
 time.sleep(1)
 # A test-APK-only read-only DocumentsProvider supplies the exact shipped bytes through real SAF.
 tree=xml()
 if not matches(tree,'sui-code test packages'):
  for label in ['Show roots','Show navigation drawer','Open navigation drawer']:
   if matches(tree,label):tap(label);break
 tap('sui-code test packages',scroll=True,pane=True)
 tap(a.kit.name,scroll=True)
 review()
 note('REAL_SAF_IMPORT_AND_REVIEW_PASS (read-only test DocumentsProvider; shipped Kit bytes)')
def select_language(chinese):
 current_zh=bool(matches(xml(),'界面语言'))
 tap('界面语言' if current_zh else 'Interface language')
 # Current selection button, separate from the heading. Its text identifies the selected locale.
 tree=xml()
 selected='简体中文 (zh-Hans)' if matches(tree,'简体中文 (zh-Hans)',True) else ('English（默认）' if current_zh else 'English (default)')
 tap(selected,contains=selected.startswith('简体'))
 if chinese:tap('简体中文 (zh-Hans)\n'+kitid)
 else:tap('English（默认）' if current_zh else 'English (default)')
 tap('关闭' if chinese else 'Close')
 wait_text('运行文件' if chinese else 'Run file')
 checkpoint('language-selected-'+('zh' if chinese else 'en'),['运行文件','概览','文件','扩展包'] if chinese else ['Run file','Overview','Files','Packages'])
def card_action(label,chinese):
 open_packages(chinese);tap('已安装' if chinese else 'Installed')
 deadline=time.monotonic()+70
 while True:
  tree=xml();owners=[]
  for n in tree.iter('node'):
   subtree=list(n.iter('node'))
   records=[c for c in subtree if c.attrib.get('text','').startswith(kitid+'\n')]
   if records:
    record_bottom=min(bounds(c)[3] for c in records)
    # A partially visible card must not borrow Disable/Uninstall from the preceding Kit.
    actions=[c for c in subtree if c.attrib.get('text')==label and bounds(c)[1]>=record_bottom]
    if actions:owners.append((len(subtree),actions))
  if owners:
   actions=min(owners,key=lambda t:t[0])[1];tap_node(min(actions,key=lambda n:bounds(n)[1]));return
  assert time.monotonic()<deadline,'Package action not visible for '+kitid+': '+label
  swipe(False)

def ui_lifecycle():
 import_kit();select_language(True)
 assert kitid+'/zh-Hans' in runas('cat shared_prefs/sui-native-ui-language.xml').stdout
 alive('chinese-cold-restart',True)
 checkpoint('chinese-navigation',['文件','查找','语言','扩展包','设置','运行文件','调试','概览'])
 tap('文件',nav=True);checkpoint('chinese-files',['文件名筛选'])
 tap('新建文件夹',scroll=True,pane=True);checkpoint('chinese-create-folder',['新建文件夹','取消']);tap('取消')
 tap('查找',nav=True);checkpoint('chinese-find',['文件内容'])
 tap('语言',nav=True);wait_text('刷新就绪状态',scroll=True);checkpoint('chinese-languages',['刷新就绪状态'])
 tap('设置',nav=True);tree,_=wait_text('外观')
 assert any(n.attrib.get('class')=='android.widget.EditText' for n in tree.iter('node')),'Expected the actual Settings search editor'
 # The visual placeholder is behind BasicTextField and Android omits it from accessibility XML.
 checkpoint('chinese-settings',['全局','外观'])
 # Real portrait/font scale recreation, still no environment installation or data reset.
 adb('shell','wm size 800x1280');adb('shell','settings put system font_scale 1.3');time.sleep(2)
 checkpoint('chinese-portrait-font130',['设置','外观'])
 adb('shell','wm size 1280x800');adb('shell','settings put system font_scale 1.0');time.sleep(2)
 card_action('禁用',True);wait_text('Run file');checkpoint('disabled-english-fallback',['Run file'])
 card_action('Enable',False);review();wait_text('运行文件');checkpoint('reenabled-chinese',['运行文件'])
 open_packages(True);select_language(False);alive('explicit-english-restart')
 open_packages();select_language(True)
 card_action('卸载',True);wait_text('卸载扩展包？');tap('卸载')
 wait_text('Run file');alive('uninstalled-english-restart');checkpoint('uninstalled-english-fallback',['Run file','Packages'])
 note('UI_INSTALL_SELECT_RESTART_DISABLE_ENABLE_ENGLISH_UNINSTALL_PASS')

(a.evidence/'platform.txt').write_text(adb('shell','getprop').stdout)
adb('shell','wm size 1280x800');adb('shell','wm density 160');adb('shell','settings put system font_scale 1.0')
# The test DocumentsProvider avoids adb/shell writes into Android16's FUSE Downloads.
adb('shell','input keyevent KEYCODE_WAKEUP');adb('shell','wm dismiss-keyguard',check=False);adb('shell','input keyevent 82')
if a.mode=='upgrade':
 install(root/'.delivery/sui-code-v0.5.2-startup-recovery/sui-code-v0.5.2-startup-debug.apk');alive('v052-original-first-frame')
 adb('shell','am force-stop '+pkg);adb('push',a.inputs/'payload.tar','/data/local/tmp/sui-zh-payload.tar');runas('tar -xf /data/local/tmp/sui-zh-payload.tar');hashes('v052-seeded')
 install(a.inputs/'startup-androidTest.apk')
 instrument('dev.blamspot.jcode.BaselineDataTest#createRegisteredProject','v052-create-project',1)
 adb('shell','am force-stop '+pkg)
install(a.apk);install(a.inputs/'startup-androidTest.apk');alive('v053-first-frame');ensure_workbench()
if a.mode=='upgrade':
 hashes('v053-before-localization');instrument('dev.blamspot.jcode.BaselineDataTest#verifyRegisteredProjectSurvives','v053-project-retained',1);alive('v053-project-restart');ensure_workbench()
ui_lifecycle()
for i in range(3):alive('final-english-cold-'+str(i+1))
if a.mode=='upgrade':hashes('after-ui-lifecycle')
# Real on-disk wrong-type optional preferences. Preserve the bad input and verify English fallback.
adb('shell','am force-stop '+pkg)
for name,value in [('sui-native-ui-language','<map><int name="selection-v1" value="7"/><string name="untouched">keep</string></map>'),('sui-workbench-layout','<map><string name="content-width-v1">broken</string><string name="untouched">keep</string></map>')]:runas('mkdir -p shared_prefs; printf %s '+shlex.quote(value)+' > shared_prefs/'+name+'.xml')
alive('wrong-preference-english-fallback');assert 'broken' in runas('cat shared_prefs/sui-workbench-layout.xml').stdout
install(a.inputs/'startup-androidTest.apk')
instrument('dev.blamspot.jcode.StartupRuntimeTest','startup-android',4)
instrument('dev.blamspot.jcode.ZhHansRuntimeTest','full-kit-android',4)
if a.mode=='upgrade':hashes('after-all-android-tests')
(a.evidence/'logcat.txt').write_text(adb('logcat','-d','-v','threadtime','-s','AndroidRuntime','SuiStartup','SuiStartupTest','SuiPerformance').stdout)
for name in ['startup.log','startup.previous.log']:
 r=runas('cat files/sui-code/diagnostics/'+name,check=False)
 if r.returncode==0:(a.evidence/name).write_text(r.stdout)
(a.evidence/'result.json').write_text(json.dumps({'mode':a.mode,'result':'PASS','android_instrumentation_tests':8,'physical_arm64':False,'ui_lifecycle':'real SAF import, consent, selection, restart, disable, reenable, English selection, uninstall, portrait/font130'},indent=2)+'\n')
note('COMPLETE '+a.mode)
