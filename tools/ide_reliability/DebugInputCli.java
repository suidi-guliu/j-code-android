import dev.blamspot.jcode.core.debug.*;
import java.nio.file.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import kotlin.*;
import kotlin.coroutines.*;
import kotlin.jvm.functions.*;
import kotlinx.coroutines.*;
import org.json.*;

/** Executes the exact compiled production DebugSession, real adapters and an owned Linux PTY. */
public class DebugInputCli {
 @SuppressWarnings({"unchecked","rawtypes"}) static <T>T await(Function1<Continuation<T>,Object> action) throws Exception {
  return (T) BuildersKt.runBlocking(EmptyCoroutineContext.INSTANCE,(scope,continuation)->action.invoke((Continuation<T>)(Continuation)continuation));
 }
 static class Wire implements DapTransport {
  final InputStream input;final OutputStream output;final AutoCloseable close;final ByteArrayOutputStream sent=new ByteArrayOutputStream();
  Wire(InputStream i,OutputStream o,AutoCloseable c){input=i;output=o;close=c;}
  public int read(byte[] b){try{return input.read(b);}catch(IOException e){return -1;}}
  public synchronized void write(byte[] b){try{sent.write(b);output.write(b);output.flush();}catch(IOException e){throw new RuntimeException(e);}}
  public void close(){try{close.close();}catch(Exception e){throw new RuntimeException(e);}}
  public String terminationDetail(){return null;}
 }
 static class Broker implements AutoCloseable {
  final Process process;final Writer control;final Path output;final JSONObject ready;
  Broker(JSONObject request,Path root,String tag,JSONObject cfg) throws Exception {
   output=root.resolve(tag+"-pty.log");request.put("output",output.toString());
   request.put("prefix",cfg.getJSONArray("proot"));Path file=root.resolve(tag+"-request.json");Files.writeString(file,request.toString());
   process=new ProcessBuilder("/usr/bin/python3",cfg.getString("broker"),file.toString()).redirectError(root.resolve(tag+"-broker.err").toFile()).start();
   control=new OutputStreamWriter(process.getOutputStream(),java.nio.charset.StandardCharsets.UTF_8);
   ready=new JSONObject(new BufferedReader(new InputStreamReader(process.getInputStream())).readLine());
  }
  void send(JSONObject data)throws Exception{control.write(data+"\n");control.flush();}
  String text(){try{return Files.readString(output);}catch(IOException e){return "";}}
  void until(String token)throws Exception{long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(30);while(!text().contains(token)){if(System.nanoTime()>end)throw new AssertionError("PTY missing "+token+": "+text());Thread.sleep(20);}}
  public void close()throws Exception{if(process.isAlive()){send(new JSONObject().put("close",true));if(!process.waitFor(7,TimeUnit.SECONDS)){process.destroyForcibly();process.waitFor();}}}
 }
 static boolean running(int pid){try{return !Files.readString(Path.of("/proc/"+pid+"/stat")).substring(Files.readString(Path.of("/proc/"+pid+"/stat")).lastIndexOf(')')+2).startsWith("Z");}catch(IOException e){return false;}}
 static void waitFor(BooleanSupplier condition,String why)throws Exception {long end=System.nanoTime()+TimeUnit.SECONDS.toNanos(35);while(!condition.getAsBoolean()){if(System.nanoTime()>end)throw new AssertionError(why);Thread.sleep(20);}}
 public static void main(String[] args)throws Exception{
  if(!"1".equals(System.getenv("SUI_OWNED_INTEGRATION")))throw new IllegalStateException("Owned integration only");
  JSONObject cfg=new JSONObject(Files.readString(Path.of(args[0])));Path root=Path.of(cfg.getString("evidence"));Files.createDirectories(root);
  List<Broker> brokers=new CopyOnWriteArrayList<>();AtomicReference<Wire> wire=new AtomicReference<>();AtomicInteger exit=new AtomicInteger(Integer.MIN_VALUE),pid=new AtomicInteger();AtomicBoolean ended=new AtomicBoolean();List<String> adapterOutput=new CopyOnWriteArrayList<>();
  JSONObject launch=cfg.getJSONObject("launch");String language=cfg.getString("language");
  Process sentinel=new ProcessBuilder("/bin/sleep","120").start();
  DebugSession session=new DebugSession(language,cfg.getString("project"),command->{try{
   Wire w;
   if(cfg.has("port")){Socket socket=new Socket("127.0.0.1",cfg.getInt("port"));w=new Wire(socket.getInputStream(),socket.getOutputStream(),()->socket.close());}
   else {List<String> argv=new ArrayList<>();cfg.getJSONArray("proot").forEach(v->argv.add((String)v));Collections.addAll(argv,"/bin/bash","--noprofile","--norc","-c",command);Process process=new ProcessBuilder(argv).directory(Path.of(cfg.getString("project")).toFile()).redirectError(root.resolve("adapter.stderr").toFile()).start();w=new Wire(process.getInputStream(),process.getOutputStream(),()->{process.destroy();if(!process.waitFor(4,TimeUnit.SECONDS))process.destroyForcibly();});}
   wire.set(w);return w;
  }catch(Exception e){throw new RuntimeException(e);}});
  session.setOnRunInTerminal((request,continuation)->{try{
   DebugTerminalPaths paths=new DebugTerminalPaths(UUID.randomUUID().toString());Files.createDirectories(Path.of(paths.getDirectory()));
   Files.writeString(Path.of(paths.getScript()),DebugTerminalLaunch.INSTANCE.script(request,paths));
   JSONObject r=new JSONObject().put("cwd",request.getCwd()).put("args",new JSONArray(List.of("/bin/bash","--noprofile","--norc",paths.getScript()))).put("env",new JSONObject());
   Broker broker=new Broker(r,root,"reverse-"+brokers.size(),cfg);brokers.add(broker);
   Files.writeString(Path.of(paths.getReady()),"ready");
   waitFor(()->Files.isRegularFile(Path.of(paths.getPid())),"No production launcher PID acknowledgement");
   String[] ids=Files.readString(Path.of(paths.getPid())).trim().split(" ");
   if(ids.length!=2)throw new AssertionError("Invalid actual terminal PID metadata");
   Files.writeString(Path.of(paths.getGo()),"go");
   return new TerminalProcess(Integer.parseInt(ids[0]),Integer.parseInt(ids[1]));
  }catch(Exception e){throw new RuntimeException(e);}});
  session.setOnOutput((category,text)->{adapterOutput.add(category+": "+text);return Unit.INSTANCE;});
  session.setOnProcess(value->{pid.set(value);return Unit.INSTANCE;});session.setOnExit(value->{exit.set(value);return Unit.INSTANCE;});session.setOnTerminated(()->{ended.set(true);return Unit.INSTANCE;});
  try {
   if(launch.has("suiTerminal")){
    if(!launch.getString("suiTerminal").equals("lldb-stdio"))throw new AssertionError("Unexpected terminal policy");launch.remove("suiTerminal");
    Broker broker=new Broker(new JSONObject().put("cwd",cfg.getString("project")),root,"stdio",cfg);brokers.add(broker);String tty=broker.ready.getString("tty");if(!tty.matches("/dev/pts/[0-9]+"))throw new AssertionError(tty);
    JSONArray commands=launch.optJSONArray("preRunCommands");if(commands==null)commands=new JSONArray();for(String field:List.of("input","output","error"))commands.put("settings set target."+field+"-path "+tty);launch.put("preRunCommands",commands);
   }
   Map<String,List<Integer>> bps=Map.of(cfg.getString("source"),List.of(cfg.getInt("before"),cfg.getInt("after")));
   await(c->session.start(cfg.optString("adapter",""),"launch",launch,bps,c));
   waitFor(()->session.getState().getValue()==DebugState.STOPPED || session.getState().getValue()==DebugState.ERROR || ended.get(),"No input-before breakpoint");
   if(session.getState().getValue()!=DebugState.STOPPED)throw new AssertionError(adapterOutput);
   int thread=session.getStopped().getValue().getThreadId();List<DapStackFrame> stack=await(c->session.stackTrace(thread,c));if(stack.isEmpty()||stack.getFirst().getLine()!=cfg.getInt("before"))throw new AssertionError("Wrong before breakpoint "+stack);
   List<DapScope> scopes=await(c->session.scopes(stack.getFirst().getId(),c));if(scopes.isEmpty())throw new AssertionError("No scopes");
   await(c->session.continueThread(thread,c));waitFor(()->!brokers.isEmpty(),"No real debuggee PTY");Broker broker=brokers.getFirst();broker.until("INPUT:");
   String mode=cfg.optString("mode","normal");
   if(mode.equals("cancel")){session.close();waitFor(()->session.getState().getValue()==DebugState.DISCONNECTED,"Stop did not disconnect");}
   else {
    if(mode.equals("eof")) broker.send(new JSONObject().put("eof",true));else broker.send(new JSONObject().put("input","用户输入张三\n"));
    waitFor(()->session.getState().getValue()==DebugState.STOPPED || ended.get() || session.getState().getValue()==DebugState.ERROR,"No post-input breakpoint or real EOF exit");
    if(mode.equals("eof")){
     // Languages may explicitly handle EOF. This fixture must either terminate nonzero or reach its after-input breakpoint with a documented failure value.
     if(!ended.get())throw new AssertionError("EOF fixture did not terminate with a real error: "+adapterOutput);
     if(exit.get()==0 || exit.get()==Integer.MIN_VALUE)throw new AssertionError("EOF did not report its real nonzero exit");
    }else {
     if(session.getState().getValue()!=DebugState.STOPPED)throw new AssertionError("Did not stop after input: "+adapterOutput);
     int afterThread=session.getStopped().getValue().getThreadId();List<DapStackFrame> afterStack=await(c->session.stackTrace(afterThread,c));if(afterStack.isEmpty()||afterStack.getFirst().getLine()!=cfg.getInt("after"))throw new AssertionError("Wrong after breakpoint "+afterStack);
     List<DapScope> afterScopes=await(c->session.scopes(afterStack.getFirst().getId(),c));boolean sawName=false;
     for(DapScope scope:afterScopes)if(!scope.getExpensive()&&scope.getVariablesReference()!=0){List<DapVariable> variables=await(c->session.variables(scope.getVariablesReference(),c));if(variables.toString().contains("用户输入张三"))sawName=true;}
     if(!sawName)throw new AssertionError("Real variables did not contain typed text");
     String second="暂停时输入"+"长文本".repeat(128);broker.send(new JSONObject().put("input",second+"\n"));
     await(c->session.next(afterThread,c));waitFor(()->session.getState().getValue()==DebugState.STOPPED,"Step lost its stopped event");
     int resumed=session.getStopped().getValue().getThreadId();await(c->session.continueThread(resumed,c));waitFor(ended::get,"Program did not terminate");broker.until("SECOND:"+second);broker.until("RESULT:你好 用户输入张三");if(exit.get()!=0)throw new AssertionError("Wrong exit "+exit);
    }
   }
   session.close();waitFor(()->session.getState().getValue()==DebugState.DISCONNECTED,"Disconnect incomplete");
   if(pid.get()>0)waitFor(()->!running(pid.get()),"Debuggee leaked after disconnect: "+pid);
   if(!sentinel.isAlive())throw new AssertionError("Unrelated owned process was killed");
   if(wire.get().sent.toString(java.nio.charset.StandardCharsets.UTF_8).contains("用户输入张三"))throw new AssertionError("stdin was written into DAP transport");
   JSONObject result=new JSONObject().put("language",language).put("mode",mode).put("result","PASS").put("exitCode",exit.get()==Integer.MIN_VALUE?JSONObject.NULL:exit.get()).put("realCoreSession",true).put("debuggeePid",pid.get()).put("unrelatedRetained",true).put("dapAndStdinSeparate",true);
   Files.writeString(root.resolve("result.json"),result.toString(2));System.out.println(result);
  } finally {
   session.close();for(Broker broker:brokers)broker.close();sentinel.destroyForcibly();sentinel.waitFor();Files.writeString(root.resolve("adapter-output.log"),String.join("",adapterOutput));
  }
 }
}
