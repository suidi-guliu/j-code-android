#!/usr/bin/env python3
"""Four real Basic providers with Unicode stdin over an existing Linux PTY and PRoot."""
import json,os,pathlib,shlex,subprocess
root=pathlib.Path('/evidence/task-projects/standalone 中文 : no Project')
plans=json.loads(pathlib.Path('/evidence/task-path-plans.json').read_text());guest=plans[str(root)]['guest'];mounts={b['guest']:b['host'] for v in plans.values() for b in v['binds']}
os.environ.update(PATH='/home/ubuntu/bin:'+os.environ['PATH'],LANG='C.UTF-8',LC_ALL='C.UTF-8')
results=[]
for language,provider,name,text in [
 ('Java','java','Input.java','import java.util.Scanner; public class Input { public static void main(String[] a) { System.out.println("INPUT="+new Scanner(System.in).nextLine()); }}'),
 ('C_CPP','c','input.c','#include <stdio.h>\nint main(){char input[200];if(!fgets(input,sizeof input,stdin))return 1;printf("INPUT=%s",input);return 0;}'),
 ('C_CPP','cpp','input.cpp','#include <iostream>\n#include <string>\nint main(){std::string input;std::getline(std::cin,input);std::cout<<"INPUT="<<input<<std::endl;}'),
 ('Python','python','input.py','print("INPUT="+input())\n')]:
 (root/name).write_text(text)
 argv=['proot','--kill-on-exit','--link2symlink','-r','/']
 for target,host in sorted(mounts.items(),key=lambda x:x[0].count('/')):argv+=['-b',host+':'+target]
 argv+=['-w',guest,'/bin/bash','/library/'+language+'/suic/resources/run-'+provider+'.sh',guest+'/'+name]
 p=subprocess.run(['script','-qefc',shlex.join(argv),'/dev/null'],input='你好 42\n',capture_output=True,text=True,timeout=60)
 pathlib.Path('/evidence/interactive-'+provider+'.log').write_text(p.stdout+p.stderr)
 assert p.returncode==0 and 'INPUT=你好 42' in p.stdout,(provider,p.returncode,p.stdout,p.stderr)
 results.append({'provider':provider,'result':'PASS','stdin':'你好 42','actual_pty':True});print(json.dumps(results[-1],ensure_ascii=False),flush=True)
pathlib.Path('/evidence/interactive-results.json').write_text(json.dumps(results,ensure_ascii=False,indent=2)+'\n')
