#!/usr/bin/env python3
"""Real compiled Core cleanup + Linux syscall binding. PTY session/output must survive Stop.
The isolated Ubuntu user/container owns every test process. Never run on user application data.
"""
import json,os,pathlib,pty,select,shlex,signal,subprocess,time,uuid,termios
os.environ["PATH"]="/home/ubuntu/bin:"+os.environ["PATH"]
root=pathlib.Path('/evidence/stop-tests');root.mkdir(exist_ok=True)
java=root/'LongRun.java';java.write_text('public class LongRun { public static void main(String[] a) throws Exception { System.out.println("READY_JAVA"); while(true) Thread.sleep(1000); }}')
c=root/'long.c';c.write_text('#include <stdio.h>\n#include <unistd.h>\nint main(){puts("READY_C");fflush(stdout);for(;;)sleep(1);}')
bin=root/'program';subprocess.run(['gcc',str(c),'-o',str(bin)],check=True)
results=[]
def read_until(fd,needle,timeout=30):
 text='';end=time.monotonic()+timeout
 while time.monotonic()<end:
  if select.select([fd],[],[],0.2)[0]:
   try:text+=os.read(fd,65536).decode(errors='replace')
   except OSError:break
   if needle in text:return text
 raise AssertionError((needle,text))
for name,command,ready in [('Java',['/jdk/bin/java',str(java)],'READY_JAVA'),('C',[str(bin)],'READY_C'),('Python',['python3','-u','-c','import time;print("READY_PYTHON");time.sleep(600)'],'READY_PYTHON')]:
 owner='sui-quickrun:'+uuid.uuid4().hex
 master,slave=pty.openpty()
 attrs=termios.tcgetattr(slave);attrs[3]&=~termios.ECHO;termios.tcsetattr(slave,termios.TCSANOW,attrs)
 # The persistent shell has no task tag. Its child run owns the stop boundary.
 shell=subprocess.Popen(['/bin/bash','--noprofile','--norc'],stdin=slave,stdout=slave,stderr=slave,start_new_session=True);os.close(slave)
 unrelated=subprocess.Popen(['sleep','600'],env={**os.environ,'JCODE_EXT_ID':owner+'-other'},start_new_session=True)
 try:
  args=['proot','--kill-on-exit','--link2symlink','-r','/','-w',str(root),'/bin/bash','-c','export JCODE_EXT_ID='+shlex.quote(owner)+'; '+shlex.join(command)]
  os.write(master,(shlex.join(args)+'\n').encode());transcript=read_until(master,ready)
  r=subprocess.run(['/jdk/bin/java','-cp','/opt/v054-tests:/opt/v054-tests/distro-classes.jar:/opt/v054-tests/kotlin-stdlib.jar','StopOwnerCli',owner],capture_output=True,text=True,timeout=30)
  assert r.returncode==0,(r.stdout,r.stderr)
  assert shell.poll() is None,'Stopping the run closed its terminal shell'
  assert unrelated.poll() is None,'Stopping one owner killed an unrelated owner'
  os.write(master,b'printf "TERMINAL_RETAINED\\n"\n');transcript+=read_until(master,'TERMINAL_RETAINED')
  (root/(name+'.log')).write_text(transcript+'\n'+r.stdout+r.stderr)
  results.append({'language':name,'result':'PASS','core_cleanup':r.stdout.strip(),'terminal_retained':True,'unrelated_retained':True});print(json.dumps(results[-1]),flush=True)
 finally:
  os.close(master)
  for p in [shell,unrelated]:
   if p.poll() is None:
    os.killpg(p.pid,signal.SIGTERM)
    try:p.wait(timeout=3)
    except subprocess.TimeoutExpired:os.killpg(p.pid,signal.SIGKILL);p.wait()
pathlib.Path('/evidence/stop-results.json').write_text(json.dumps(results,indent=2)+'\n')
