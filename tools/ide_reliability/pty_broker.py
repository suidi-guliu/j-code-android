#!/usr/bin/env python3
"""Owned Ubuntu test PTY broker; never shipped as an Android terminal implementation.
stdin/stdout is a bounded JSON control channel; program I/O uses openpty, not a DAP pipe.
"""
import json,os,pathlib,pty,select,signal,subprocess,sys,threading,time
assert os.environ.get('SUI_OWNED_INTEGRATION')=='1','Owned test environment required'
request=json.loads(pathlib.Path(sys.argv[1]).read_text());output=pathlib.Path(request['output'])
assert str(output).startswith('/tmp/sui-v055-'),output
master,slave=pty.openpty();tty=os.ttyname(slave);child=None;closed=False
if request.get('args'):
 env=os.environ.copy()
 for k,v in request.get('env',{}).items():
  if v is None:env.pop(k,None)
  else:env[k]=v
 argv=request.get('prefix',[])+request['args']
 child=subprocess.Popen(argv,cwd=request['cwd'],env=env,stdin=slave,stdout=slave,stderr=slave,start_new_session=True)
print(json.dumps({'tty':tty,'pid':child.pid if child else None,'shellPid':os.getpid()}),flush=True)
def reader():
 with output.open('wb',buffering=0) as sink:
  while not closed:
   if select.select([master],[],[],.1)[0]:
    try:data=os.read(master,32768)
    except OSError:break
    if not data:break
    sink.write(data)
thread=threading.Thread(target=reader,daemon=True);thread.start()
try:
 for line in sys.stdin:
  message=json.loads(line)
  if 'input' in message:os.write(master,message['input'].encode())
  elif message.get('eof'):os.write(master,b'\x04')
  elif message.get('close'):break
finally:
 if child and child.poll() is None:
  try:os.killpg(child.pid,signal.SIGTERM);child.wait(timeout=4)
  except subprocess.TimeoutExpired:os.killpg(child.pid,signal.SIGKILL);child.wait(timeout=3)
  except ProcessLookupError:pass
 time.sleep(.1);closed=True;thread.join(timeout=1);os.close(master);os.close(slave)
