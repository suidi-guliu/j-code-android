#!/usr/bin/env python3
"""Real production DebugSession + Ubuntu24.04 PRoot + owned PTYs, not ARM64 acceptance.
Requires compiled DebugInputCli and the production debug/distro jars on --classpath.
"""
import argparse,importlib.util,json,os,pathlib,shlex,signal,socket,subprocess,tempfile,time,traceback,shutil
p=argparse.ArgumentParser();p.add_argument('--library',type=pathlib.Path,required=True);p.add_argument('--classpath',required=True);p.add_argument('--broker',type=pathlib.Path,required=True);p.add_argument('--protocol',type=pathlib.Path,required=True);p.add_argument('--output',type=pathlib.Path,required=True);p.add_argument('--proot',default='/home/ubuntu/bin/proot');p.add_argument('--jdk',default='/jdk');p.add_argument('--only',choices=['Python','Java','C','C_CPP']);a=p.parse_args()
assert os.environ.get('SUI_OWNED_INTEGRATION')=='1','Owned Ubuntu environment required'
spec=importlib.util.spec_from_file_location('framed',a.protocol);mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);Framed=mod.Framed
root=pathlib.Path(tempfile.mkdtemp(prefix='sui-v055-debug-'));results=[];a.output.mkdir(parents=True,exist_ok=True)
python='''before = 1 # BEFORE
name = input("INPUT:")
after = "你好 " + name # AFTER
print("RESULT:" + after, flush=True)
more = input()
print("SECOND:" + more, flush=True)
'''
java='''import java.util.Scanner;
public class Hello {
 public static void main(String[] args) {
  int before = 1; // BEFORE
  Scanner scanner = new Scanner(System.in);
  System.out.print("INPUT:");
  String name = scanner.nextLine();
  String after = "你好 " + name; // AFTER
  System.out.println("RESULT:" + after);
  String more = scanner.nextLine();
  System.out.println("SECOND:" + more);
 }
}
'''
cpp='''#include <iostream>
#include <string>
int main(){
 int before = 1; // BEFORE
 std::string name;
 std::cout<<"INPUT:"<<std::flush;
 if(!std::getline(std::cin,name)) return 19;
 std::string after="你好 "+name; // AFTER
 std::cout<<"RESULT:"<<after<<std::endl;
 std::string more;
 if(!std::getline(std::cin,more)) return 20;
 std::cout<<"SECOND:"<<more<<std::endl;
 return 0;
}
'''
c='''#include <stdio.h>
#include <string.h>
int main(){
 int before=1; // BEFORE
 char name[1024],more[2048];
 printf("INPUT:");fflush(stdout);
 if(scanf("%1023[^\\n]%*c",name)!=1)return 19;
 int length=strlen(name); // AFTER
 printf("RESULT:你好 %s\\n",name);fflush(stdout);
 if(scanf("%2047[^\\n]%*c",more)!=1)return 20;
 printf("SECOND:%s\\n",more);fflush(stdout);
 return 0;
}
'''
for label in ['plain','中文 space']:
 for language,text,filename in [('Python',python,'main.py'),('C_CPP',cpp,'main.cpp'),('C',c,'main.c'),('Java',java,'Hello.java')]:
  if a.only and language!=a.only:continue
  project=root/(language+' '+label);project.mkdir();source=project/filename;source.write_text(text)
  descriptor=json.loads((a.library/('C_CPP' if language=='C' else language)/'suik/adapters/dap.json').read_text());workflow=json.loads(descriptor['workflow']);jdt=None;lsp=None
  try:
   if language in ['C','C_CPP']:
    prepared=subprocess.run(['/bin/bash',str(a.library/'C_CPP/suik/resources/prepare-debug.sh'),str(source),str(project)],capture_output=True,text=True,timeout=120)
    (a.output/f'{language}-{label}-prepare.log').write_text(prepared.stdout+prepared.stderr);assert prepared.returncode==0,prepared.stderr;program=json.loads(prepared.stdout)['program']
   if language=='Java':
    server=pathlib.Path('/opt/sui-code/ide/java/current');configuration=root/('jdt-configuration-'+label);shutil.copytree(server/'config_linux',configuration)
    launcher=next((server/'plugins').glob('org.eclipse.equinox.launcher_*.jar'))
    command=[a.proot,'--kill-on-exit','--link2symlink','-r','/','-w',str(project),a.jdk+'/bin/java','-Xmx640m','-Declipse.application=org.eclipse.jdt.ls.core.id1','-Declipse.product=org.eclipse.jdt.ls.core.product','-Dosgi.bundles.defaultStartLevel=4','--add-modules=ALL-SYSTEM','--add-opens','java.base/java.util=ALL-UNNAMED','--add-opens','java.base/java.lang=ALL-UNNAMED','-jar',str(launcher),'-configuration',str(configuration),'-data',str(root/('jdt-workspace-'+label))]
    jdt=subprocess.Popen(command,stdin=subprocess.PIPE,stdout=subprocess.PIPE,stderr=(a.output/f'Java-{label}-jdt.stderr').open('wb'),start_new_session=True,env={**os.environ,'JAVA_HOME':a.jdk})
    lsp=Framed(jdt.stdout,jdt.stdin)
    lsp.rpc('initialize',{'processId':os.getpid(),'rootUri':project.as_uri(),'workspaceFolders':[{'uri':project.as_uri(),'name':'integration'}],'capabilities':{'workspace':{'configuration':True,'workspaceFolders':True}},'initializationOptions':{'bundles':[str(server/'java-debug.jar')],'extendedClientCapabilities':{'classFileContentsSupport':True}}})
    lsp.send({'jsonrpc':'2.0','method':'initialized','params':{}});lsp.send({'jsonrpc':'2.0','method':'textDocument/didOpen','params':{'textDocument':{'uri':source.as_uri(),'languageId':'java','version':1,'text':text}}})
    mains=[]
    for _ in range(30):
     mains=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.resolveMainClass','arguments':[project.as_uri()]})
     (a.output/f'Java-{label}-main-candidates.json').write_text(json.dumps(mains,ensure_ascii=False,indent=2))
     if any(x.get('mainClass')=='Hello' and x.get('projectName') for x in mains):break
     time.sleep(1)
    selected=next(x for x in mains if x['mainClass']=='Hello' and x.get('projectName'))
    built=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.buildWorkspace','arguments':[json.dumps({'projectName':selected['projectName'],'mainClass':'Hello','isFullBuild':False})]});assert built==1,built
    paths=lsp.rpc('workspace/executeCommand',{'command':'vscode.java.resolveClasspath','arguments':['Hello',selected['projectName']]})
   for repeat,mode in [(1,'normal'),(2,'normal'),(1,'cancel'),(1,'eof')]:
    case=f'{language}-{label}-{mode}-{repeat}';directory=project/case;directory.mkdir()
    try:
     launch=json.loads(json.dumps(workflow['launch']).replace('${source}',str(source)).replace('${project}',str(project)).replace('${prepared.program}',program if language in ['C','C_CPP'] else '')) if language!='Java' else {'type':'java','request':'launch','mainClass':'Hello','projectName':selected['projectName'],'modulePaths':paths[0],'classPaths':paths[1],'cwd':str(project),'console':workflow['launch']['console'],'suiTerminalExit':workflow['launch'].get('suiTerminalExit'),'stopOnEntry':False,'javaExec':a.jdk+'/bin/java'}
     config={'language':descriptor['debugType'],'project':str(project),'source':str(source),'launch':launch,'before':next(i+1 for i,line in enumerate(text.splitlines()) if 'BEFORE' in line),'after':next(i+1 for i,line in enumerate(text.splitlines()) if 'AFTER' in line),'mode':mode,'evidence':str(directory),'proot':[a.proot,'--kill-on-exit','--link2symlink','-r','/','-w',str(project)],'broker':str(a.broker),'adapter':descriptor['adapterCommand']}
     if language=='Java':config['port']=int(lsp.rpc('workspace/executeCommand',{'command':'vscode.java.startDebugSession','arguments':[]}))
     file=directory/'config.json';file.write_text(json.dumps(config,ensure_ascii=False))
     result=subprocess.run([a.jdk+'/bin/java','-cp',a.classpath,'DebugInputCli',str(file)],capture_output=True,text=True,timeout=150)
     (directory/'cli.log').write_text(result.stdout+result.stderr);assert result.returncode==0,result.stdout+result.stderr
     record=json.loads((directory/'result.json').read_text());record.update(case=case,language=language,repeat=repeat,platform='Ubuntu24.04 amd64 PRoot; production Kotlin DebugSession; not Android ARM64');results.append(record)
    except Exception:results.append({'case':case,'result':'FAIL','error':traceback.format_exc()})
    print(json.dumps(results[-1],ensure_ascii=False),flush=True)
    shutil.copytree(directory,a.output/case,dirs_exist_ok=True)
    (a.output/'debug-input-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
  except Exception:
   results.append({'case':language+' '+label+' setup','result':'FAIL','error':traceback.format_exc()});print(json.dumps(results[-1]),flush=True)
  finally:
   if lsp:
    try:lsp.rpc('shutdown',None);lsp.send({'jsonrpc':'2.0','method':'exit','params':None})
    except Exception:pass
   if jdt:
    try:os.killpg(jdt.pid,signal.SIGTERM)
    except ProcessLookupError:pass
    try:jdt.wait(timeout=5)
    except subprocess.TimeoutExpired:os.killpg(jdt.pid,signal.SIGKILL);jdt.wait()
(a.output/'debug-input-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
print('Owned source/evidence root:',root)
assert results and all(x['result']=='PASS' for x in results),results
