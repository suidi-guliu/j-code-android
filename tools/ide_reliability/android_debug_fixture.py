#!/usr/bin/env python3
"""Owned CI fixture: actual debugpy and PTY over adb reverse, never a fake DAP server.
Android tests use their real native PTY and production debug/terminal composables. The
debuggee runs on the runner, not in Android/ARM64 Ubuntu; report this distinction.
"""
import argparse,json,os,pathlib,pty,select,signal,socket,subprocess,tempfile,threading,time,sys
p=argparse.ArgumentParser();p.add_argument('--output',type=pathlib.Path,required=True);a=p.parse_args()
assert os.environ.get('SUI_OWNED_ANDROID_CI')=='1','Owned GitHub emulator runner only'
a.output.mkdir(parents=True,exist_ok=True);root=pathlib.Path(tempfile.mkdtemp(prefix='sui-v055-android-debug-'))
source=root/'main.py';source.write_text('before=1\nname=input("INPUT:")\nafter="你好 "+name\nprint("RESULT:"+after,flush=True)\nmore=input()\nprint("SECOND:"+more,flush=True)\nprint("RUNNING:",flush=True)\nimport time\nwhile True: time.sleep(.1)\n')
def listener():
 s=socket.socket();s.bind(('127.0.0.1',0));s.listen(4);return s
control=listener();tty_server=listener();unused=listener();dap_port=unused.getsockname()[1];unused.close()
adapter=subprocess.Popen([sys.executable,'-m','debugpy.adapter','--host','127.0.0.1','--port',str(dap_port)],stdout=(a.output/'adapter.out').open('wb'),stderr=(a.output/'adapter.err').open('wb'),start_new_session=True)
sentinel=subprocess.Popen(['/bin/sleep','1800']);master,slave=pty.openpty();child=None;connected=threading.Event();closed=threading.Event();raw=bytearray()
def terminal():
 client,_=tty_server.accept();client.setblocking(False);connected.set()
 try:
  while not closed.is_set():
   readable,_,_=select.select([client,master],[],[],.1)
   if client in readable:
    data=client.recv(32768)
    if not data:break
    os.write(master,data)
   if master in readable:
    try:data=os.read(master,32768)
    except OSError:break
    raw.extend(data);client.sendall(data);(a.output/'debuggee-pty.log').write_bytes(raw)
 finally:client.close()
def commands():
 global child
 while not closed.is_set():
  client,_=control.accept()
  try:
   stream=client.makefile('rwb');request=json.loads(stream.readline())
   if request['operation']=='launch':
    r=request['arguments'];assert r.get('cwd')==str(root) and r.get('kind','integrated')=='integrated'
    assert not r.get('argsCanBeInterpretedByShell',False)
    argv=r['args'];assert len(argv)>=3 and 'debugpy' in ' '.join(argv) and str(source) in argv
    assert child is None,'Only one reviewed debuggee per fixture'
    env=os.environ.copy()
    for key,value in r.get('env',{}).items():
     assert key.isidentifier()
     if value is None:env.pop(key,None)
     else:env[key]=value
    child=subprocess.Popen(argv,cwd=root,env=env,stdin=slave,stdout=slave,stderr=slave,start_new_session=True)
    response={'processId':child.pid,'shellProcessId':os.getpid()}
   elif request['operation']=='status':
    response={'alive':child is not None and child.poll() is None,'sentinelAlive':sentinel.poll() is None,'output':raw.decode(errors='replace')}
   else:raise ValueError('Unsupported owned fixture operation')
   stream.write((json.dumps(response,ensure_ascii=False)+'\n').encode());stream.flush()
  except Exception as error:
   try:client.sendall((json.dumps({'error':repr(error)})+'\n').encode())
   except OSError:pass
  finally:client.close()
threading.Thread(target=terminal,daemon=True).start();threading.Thread(target=commands,daemon=True).start()
metadata={'controlPort':control.getsockname()[1],'ttyPort':tty_server.getsockname()[1],'dapPort':dap_port,'source':str(source),'project':str(root),'before':1,'after':3}
(a.output/'fixture.json').write_text(json.dumps(metadata));print(json.dumps(metadata),flush=True)
try:
 while adapter.poll() is None:time.sleep(.2)
 # Keep the status/TTY fixture available after the real DAP adapter disconnects.
 while not closed.is_set():time.sleep(.2)
except KeyboardInterrupt:pass
finally:
 closed.set()
 for process in [child,adapter,sentinel]:
  if process is not None and process.poll() is None:
   try:os.killpg(process.pid,signal.SIGTERM) if process is not sentinel else process.terminate()
   except ProcessLookupError:pass
   try:process.wait(timeout=4)
   except subprocess.TimeoutExpired:process.kill();process.wait()
 os.close(master);os.close(slave);control.close();tty_server.close()
