#!/usr/bin/env python3
"""Actual Ubuntu/PRoot task acceptance, run as the owned container user, never on user data.
Library resources are mounted read-only. This is Linux x64 evidence, not Android ARM64 acceptance.
"""
import argparse,base64,json,os,pathlib,shlex,shutil,signal,subprocess,time,traceback
p=argparse.ArgumentParser();p.add_argument('--library',type=pathlib.Path,default=pathlib.Path('/library'));p.add_argument('--evidence',type=pathlib.Path,default=pathlib.Path('/evidence'));a=p.parse_args()
root=a.evidence/'task-projects';root.mkdir(exist_ok=True);results=[]
env=dict(os.environ,JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/run/java-cacerts',LANG='C.UTF-8',LC_ALL='C.UTF-8',PATH='/home/ubuntu/bin:'+os.environ['PATH'])
mounts={'/workspace':str(root)};plans={}

def record(name,fn):
 start=time.monotonic()
 try:details=fn();row={'name':name,'result':'PASS','details':details}
 except Exception:row={'name':name,'result':'FAIL','error':traceback.format_exc()}
 row['elapsed_seconds']=round(time.monotonic()-start,3);results.append(row);print(json.dumps(row,ensure_ascii=False),flush=True)
 (a.evidence/'task-results.json').write_text(json.dumps({'platform':'Ubuntu24.04 amd64, real PRoot, not ARM64 Android','cases':results},ensure_ascii=False,indent=2)+'\n')

def planned(host):
 # Execute the APK's actual compiled Kotlin planner; do not reimplement its mapping rules here.
 cp='/opt/v054-tests:/opt/v054-tests/distro-classes.jar:/opt/v054-tests/kotlin-stdlib.jar'
 r=subprocess.run(['/jdk/bin/java','-cp',cp,'RuntimePathCli',str(host),str(root),str(a.evidence/'mount-aliases')],capture_output=True,text=True,env=env,check=True)
 directory=None;bindings=[]
 for line in r.stdout.splitlines():
  fields=line.split('\t')
  if fields[0]=='cwd':directory=base64.b64decode(fields[1]).decode()
  elif fields[0]=='bind':
   source,target=[base64.b64decode(x).decode() for x in fields[1:]];mounts[target]=source;bindings.append({'host':source,'guest':target})
 assert directory
 plans[str(host)]={'guest':directory,'binds':bindings}
 (a.evidence/'task-path-plans.json').write_text(json.dumps(plans,ensure_ascii=False,indent=2)+'\n')
 return directory

def run(argv,**kwargs):
 proc=subprocess.Popen(argv,start_new_session=True,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=subprocess.PIPE,text=True,env=env)
 try:
  stdout,stderr=proc.communicate(kwargs.get('input'),timeout=kwargs.get('timeout',240))
  return subprocess.CompletedProcess(argv,proc.returncode,stdout,stderr)
 except subprocess.TimeoutExpired:
  os.killpg(proc.pid,signal.SIGTERM)
  try:proc.communicate(timeout=5)
  except subprocess.TimeoutExpired:os.killpg(proc.pid,signal.SIGKILL);proc.communicate()
  raise

def guest(project,argv,*,input=None,bind=True,timeout=240):
 cmd=['proot','--kill-on-exit','--link2symlink','-r','/']
 if bind:
  for target,host in sorted(mounts.items(),key=lambda item:item[0].count('/')):cmd+=['-b',host+':'+target]
 cmd+=['-w',project,*argv]
 return run(cmd,input=input,timeout=timeout)

def check(proc,name,expected=None):
 output=proc.stdout+proc.stderr;(a.evidence/(name+'.log')).write_text(output)
 assert proc.returncode==0,(name,proc.returncode,output[-8000:])
 if expected is not None:assert expected in output,(name,output[-5000:])
 return {'exit':proc.returncode,'output':output[-1500:]}

projects={}
for language in ['Java','C_CPP','Python']:
 for variant in ['plain','中文 空格 + dollar$ quote\' semi; colon:']:
  name=language+' '+variant;host=root/name
  if not host.exists():shutil.copytree(a.library/language/'suik/resources/templates',host)
  guest_path=planned(host)
  projects[(language,variant)]=(host,guest_path)
  def task(action,language=language,guest_path=guest_path,name=name):
   return check(guest(guest_path,['/bin/bash',str(a.library/language/'suik/resources/project-task.sh'),action,guest_path]),name+'-'+action,'Hello from sui-code! 42' if action=='run' else None)
  for action in ['build','test','run']:record(name+' '+action,lambda action=action:task(action))
  record(name+' repeat run',lambda:task('run'))
  if language=='C_CPP':
   def compilation_database(host=host,guest_path=guest_path,name=name):
    entries=json.loads((host/'.sui-code/build/compile_commands.json').read_text());assert entries
    for item in entries:
     assert item['directory'].startswith(guest_path+'/'),item
     assert '/sui-quickrun' not in item['directory'],item
     check(guest('/tmp',['/bin/bash','-c','test -d '+shlex.quote(item['directory'])]),name+'-database-access')
    return {'entries':len(entries),'directory':entries[0]['directory']}
   record(name+' CMake/clangd database directories',compilation_database)

# Reproduce the original empty-bind failure independently of any compiler.
def missing_mount():
 path=projects[('Java','plain')][1]
 proc=guest('/tmp',['/bin/bash','-c','test -d '+shlex.quote(path)],bind=False)
 assert proc.returncode!=0,'The owned container must not expose /workspace without its bind'
 return {'old_empty_bind_exit':proc.returncode,'restored_bind_exit':guest('/tmp',['/bin/bash','-c','test -d '+shlex.quote(path)]).returncode}
record('missing /workspace mount reproduced; restored bind resolves project',missing_mount)

sources=root/'standalone 中文 : no Project';sources.mkdir(exist_ok=True)
items=[('Java','Solo.java','public class Solo { public static void main(String[] a) { System.out.println("SUI_SINGLE_JAVA_中文"); }}','java','SUI_SINGLE_JAVA_中文'),('C_CPP','solo.c','#include <stdio.h>\nint main(void){puts("SUI_SINGLE_C_中文");}\n','c','SUI_SINGLE_C_中文'),('C_CPP','solo.cpp','#include <iostream>\nint main(){std::cout << "SUI_SINGLE_CPP_中文" << std::endl;}\n','cpp','SUI_SINGLE_CPP_中文'),('Python','solo.py','print("SUI_SINGLE_PYTHON_中文")\n','python','SUI_SINGLE_PYTHON_中文')]
for language,name,text,adapter,expected in items:
 (sources/name).write_text(text)
 def run_single(language=language,name=name,adapter=adapter,expected=expected):
  directory=planned(sources)
  return check(guest(directory,['/bin/bash',str(a.library/language/'suic/resources'/('run-'+adapter+'.sh')),directory+'/'+name]),'single-'+adapter,expected)
 record('no Project '+adapter+' single file',run_single)
 record('no Project '+adapter+' repeat run',run_single)

# Check ordinary interactive stdin on the existing PTY mechanism (util-linux script allocates a PTY).
def interactive_python():
 file=sources/'input.py';file.write_text('print("STDIN="+input())\n');directory=planned(sources)
 argv=['/bin/bash',str(a.library/'Python/suic/resources/run-python.sh'),directory+'/input.py']
 command=shlex.join(['proot','--kill-on-exit','--link2symlink','-r','/',*[v for target,host in sorted(mounts.items(),key=lambda item:item[0].count('/')) for v in ['-b',host+':'+target]],'-w',directory,*argv])
 proc=run(['script','-qefc',command,'/dev/null'],input='42\n',timeout=30)
 return check(proc,'pty-stdin','STDIN=42')
record('interactive PTY stdin',interactive_python)

def preserve_configuration():
 assert pathlib.Path('/opt/sui-code/java.env').read_bytes()==(a.evidence/'java.env.before').read_bytes()
 assert not any(p.suffix in ['.class','.o'] or p.name=='a.out' for p in sources.iterdir())
 return 'java.env byte-identical; standalone source directory has no compilation garbage'
record('environment and standalone source safety',preserve_configuration)
assert all(r['result']=='PASS' for r in results),str(sum(r['result']=='FAIL' for r in results))+' actual task cases failed'
print('PASS',len(results),'actual task acceptance cases',flush=True)
