#!/usr/bin/env python3
"""Actual three-language success/failure/cancellation, compiled planner/launcher/history, owned PRoot."""
import argparse,base64,json,os,pathlib,shutil,subprocess,tempfile,time,traceback
p=argparse.ArgumentParser();p.add_argument('--classpath',required=True);p.add_argument('--library',type=pathlib.Path,required=True);p.add_argument('--broker',required=True);p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
assert os.environ.get('SUI_OWNED_INTEGRATION')=='1'
root=pathlib.Path(tempfile.mkdtemp(prefix='sui-v055-tasks-'));projects=root/'projects';projects.mkdir();a.output.mkdir(parents=True,exist_ok=True);results=[]
env=dict(os.environ,LANG='C.UTF-8',LC_ALL='C.UTF-8',JAVA_TOOL_OPTIONS='-Djavax.net.ssl.trustStore=/run/java-cacerts',PATH='/home/ubuntu/bin:'+os.environ['PATH'])
def plan(host):
 value=subprocess.run(['/jdk/bin/java','-cp',a.classpath,'RuntimePathCli',str(host),str(projects),str(root/'aliases')],capture_output=True,text=True,env=env,check=True)
 binds=[];cwd=None
 for line in value.stdout.splitlines():
  fields=line.split('\t')
  if fields[0]=='cwd':cwd=base64.b64decode(fields[1]).decode()
  elif fields[0]=='bind':binds+=['-b',base64.b64decode(fields[1]).decode()+':'+base64.b64decode(fields[2]).decode()]
 assert cwd
 return cwd,['/home/ubuntu/bin/proot','--kill-on-exit','--link2symlink','-r','/',*binds,'-w',cwd]
def case(name,language,host,argv,success=True,source=None,mode='normal',input=None,contains=None):
 directory=root/('result-'+str(len(results)));directory.mkdir();guest,prefix=plan(host)
 config={'name':name,'package':'dev.suicode.'+language.lower()+'.ide','component':name.split()[-1],'source':str(source or host),'project':str(host),'cwd':guest,'hostCwd':str(host),'argv':[v.replace('${project}',guest) for v in argv],'success':success,'mode':mode,'broker':a.broker,'evidence':str(directory),'proot':prefix,'version':'v0.5.5 source'}
 if input is not None:config['input']=input
 if contains is not None:config['contains']=contains
 file=directory/'config.json';file.write_text(json.dumps(config,ensure_ascii=False));started=time.monotonic()
 try:
  value=subprocess.run(['/jdk/bin/java','-cp',a.classpath,'TaskExitCli',str(file)],capture_output=True,text=True,timeout=240,env=env)
  (directory/'cli.log').write_text(value.stdout+value.stderr);assert value.returncode==0,value.stdout+value.stderr
  row=json.loads((directory/'result.json').read_text())
 except Exception:row={'name':name,'result':'FAIL','error':traceback.format_exc()}
 row.update(seconds=round(time.monotonic()-started,3),platform='Ubuntu24.04 amd64 PRoot; production launcher/history/path planner, not Android ARM64')
 results.append(row);shutil.copytree(directory,a.output/directory.name,dirs_exist_ok=True)
 (a.output/'task-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n');print(json.dumps(row,ensure_ascii=False),flush=True)
for label in ['plain','中文 space']:
 for language,relative in [('Java','src/main/java/app/Main.java'),('C_CPP','src/main.cpp'),('Python','main.py')]:
  host=projects/(language+' '+label);shutil.copytree(a.library/language/'suik/resources/templates',host)
  script=str(a.library/language/'suik/resources/project-task.sh');source=host/relative;original=source.read_text()
  for action in ['build','test','run']:
   case(language+' '+label+' '+action,language,host,['/bin/bash',script,action,'${project}'],source=source,contains='Hello from sui-code! 42' if action=='run' else None)
   if action=='build':
    source.write_text(original+'\nTHIS IS NOT VALID SOURCE !!!\n')
    case(language+' '+label+' build failure',language,host,['/bin/bash',script,'build','${project}'],False,source=source)
    source.write_text(original)
   elif action=='test':
    test=host/({'Java':'src/test/java/app/MainTest.java','C_CPP':'tests/math_test.cpp','Python':'tests/test_main.py'}[language]);old=test.read_text()
    replacements={'Java':('assertEquals(42,','assertEquals(999,'),'C_CPP':('== 42','== 999'),'Python':('self.assertEqual(42,','self.assertEqual(999,')}
    before,after=replacements[language];assert before in old,(test,old);test.write_text(old.replace(before,after))
    case(language+' '+label+' test failure',language,host,['/bin/bash',script,'test','${project}'],False,source=source);test.write_text(old)
   else:
    bad={'Java':'package app; public class Main { public static void main(String[] args){System.err.println("EXPECTED_RUN_FAILURE"); System.exit(23);} }','C_CPP':'#include <stdio.h>\nint main(){fputs("EXPECTED_RUN_FAILURE\\n",stderr);return 23;}\n','Python':'import sys\nprint("EXPECTED_RUN_FAILURE",file=sys.stderr)\nsys.exit(23)\n'}[language]
    source.write_text(bad);case(language+' '+label+' run failure',language,host,['/bin/bash',script,'run','${project}'],False,source=source,contains='EXPECTED_RUN_FAILURE');source.write_text(original)
  case(language+' '+label+' repeated run',language,host,['/bin/bash',script,'run','${project}'],source=source,contains='Hello from sui-code! 42')
  if label=='plain':
   long={'Java':'package app; public class Main { public static void main(String[] args)throws Exception {System.out.println("RUNNING:");while(true)Thread.sleep(100);} }','C_CPP':'#include <stdio.h>\n#include <unistd.h>\nint main(){puts("RUNNING:");fflush(stdout);while(1)usleep(100000);}\n','Python':'import time\nprint("RUNNING:",flush=True)\nwhile True:time.sleep(.1)\n'}[language]
   source.write_text(long);case(language+' stop run',language,host,['/bin/bash',script,'run','${project}'],False,source=source,mode='cancel');source.write_text(original)
standalone=projects/'普通文件 中文 space';standalone.mkdir()
items=[('Java','java','Input.java','import java.util.Scanner; public class Input {public static void main(String[] a){System.out.print("INPUT:");System.out.println("ECHO:"+new Scanner(System.in).nextLine());}}'),('C_CPP','c','input.c','#include <stdio.h>\nint main(){char s[200];printf("INPUT:");fflush(stdout);if(!fgets(s,200,stdin))return 19;printf("ECHO:%s",s);return 0;}'),('C_CPP','cpp','input.cpp','#include <iostream>\n#include <string>\nint main(){std::string s;std::cout<<"INPUT:"<<std::flush;if(!std::getline(std::cin,s))return 19;std::cout<<"ECHO:"<<s<<std::endl;}'),('Python','python','input.py','print("ECHO:"+input("INPUT:"))\n')]
for language,provider,name,text in items:
 source=standalone/name;source.write_text(text)
 case(provider+' no Project QuickRun input',language,standalone,['/bin/bash',str(a.library/language/'suic/resources'/('run-'+provider+'.sh')),'${project}/'+name],source=source,input='你好 42\n',contains='ECHO:你好 42')
assert len(results)==49,len(results)
assert all(r['result']=='PASS' for r in results),[r for r in results if r['result']!='PASS']
print('PASS',len(results),'real task scenarios',flush=True)
