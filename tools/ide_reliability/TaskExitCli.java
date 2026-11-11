import dev.blamspot.jcode.core.distro.*;
import dev.blamspot.jcode.tasks.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;

/** Production launcher + durable store, real PTY/PRoot, authentic package recipes. */
public class TaskExitCli {
 public static void main(String[] args)throws Exception {
  if(!"1".equals(System.getenv("SUI_OWNED_INTEGRATION")))throw new IllegalStateException("Owned integration required");
  JSONObject cfg=new JSONObject(Files.readString(Path.of(args[0])));Path root=Path.of(cfg.getString("evidence"));Files.createDirectories(root);
  String token=UUID.randomUUID().toString(),owner="sui-quickrun:"+token+"/task";
  Path recipe=Path.of("/tmp/sui-native-task-"+token+".sh"),ready=Path.of("/tmp/sui-task-launch-"+token+".sh.ready"),wrapper=root.resolve("launcher.sh");
  List<String> argv=new ArrayList<>();cfg.getJSONArray("argv").forEach(v->argv.add((String)v));
  List<String> quoted=new ArrayList<>();for(String value:argv)quoted.add(JavaEnvironmentKt.shellQuote(value));
  Files.writeString(recipe,"#!/bin/bash\nexec "+String.join(" ",quoted)+"\n");
  Files.writeString(wrapper,NativeTaskLauncher.INSTANCE.script(recipe.toString(),owner,token,ready.toString()));
  TaskHistoryStore history=new TaskHistoryStore(root.resolve("history").toFile());
  NativeTaskRecord record=history.start(cfg.getString("name"),cfg.getString("package"),cfg.getString("component"),cfg.getString("source"),cfg.optString("project",null),token,"task",cfg.optString("version","source"));
  String session="owned-terminal-"+token;history.bind(token,session);
  Process sentinel=new ProcessBuilder("/bin/sleep","300").start();
  DebugInputCli.Broker broker=null;
  try {
   broker=new DebugInputCli.Broker(new JSONObject().put("cwd",cfg.optString("hostCwd",cfg.getString("cwd"))).put("args",new JSONArray(List.of("/bin/bash","--noprofile","--norc",wrapper.toString()))),root,"task",cfg);
   final DebugInputCli.Broker active=broker;
   Files.writeString(ready,"ready");Path childPid=Path.of(ready+".pid");
   DebugInputCli.waitFor(()->Files.isRegularFile(childPid),"No production task PID acknowledgement");
   int pid=Integer.parseInt(Files.readString(childPid).trim());Files.writeString(Path.of(ready+".go"),"go");
   String mode=cfg.optString("mode","normal");
   if(cfg.has("input")){broker.until("INPUT:");broker.send(new JSONObject().put("input",cfg.getString("input")));}
   if(mode.equals("cancel")) {
    broker.until("RUNNING:");history.finish(token,null,true,"Stopped by user");Files.writeString(Path.of(ready+".cancel"),"cancelled");
    GuestProcessTree.INSTANCE.terminateOwnerBlocking(owner);
   }
   String marker="\u001b]7713;native-"+token+";";
   DebugInputCli.waitFor(()->active.text().contains(marker)&&active.text().indexOf('\u0007',active.text().indexOf(marker))>0,"No actual exit acknowledgement: "+cfg.getString("name"));
   String output=broker.text();int begin=output.indexOf(marker)+marker.length(),end=output.indexOf('\u0007',begin),exit=Integer.parseInt(output.substring(begin,end));
   byte[] bytes=Files.readAllBytes(broker.output);history.append(session,bytes,bytes.length);history.finish(token,exit,false,"");
   NativeTaskRecord result=history.getRecords().getValue().getFirst();
   TaskOutcome expected=mode.equals("cancel")?TaskOutcome.CANCELLED:cfg.getBoolean("success")?TaskOutcome.SUCCEEDED:TaskOutcome.FAILED;
   if(result.getOutcome()!=expected||(!mode.equals("cancel")&&((exit==0)!=cfg.getBoolean("success"))))throw new AssertionError(result+"\n"+output);
   if(cfg.has("contains")&&!output.contains(cfg.getString("contains")))throw new AssertionError(output);
   DebugInputCli.waitFor(()->!DebugInputCli.running(pid),"Task child leaked after completion");
   if(!sentinel.isAlive()||!broker.process.isAlive())throw new AssertionError("Unrelated process or retained transcript shell was killed");
   TaskHistoryStore reopened=new TaskHistoryStore(root.resolve("history").toFile());reopened.load();if(!reopened.getRecords().getValue().getFirst().equals(result))throw new AssertionError("Durable result changed on reopen");
   JSONObject value=new JSONObject().put("name",cfg.getString("name")).put("result","PASS").put("exitCode",exit).put("outcome",result.getOutcome().name()).put("retainedShell",true).put("unrelatedRetained",true).put("log",history.logFile(token).toString()).put("realProductionLauncherAndStore",true);
   Files.writeString(root.resolve("result.json"),value.toString(2));System.out.println(value);
  } finally {
   if(broker!=null)broker.close();sentinel.destroyForcibly();sentinel.waitFor();Files.deleteIfExists(recipe);
   for(String suffix:List.of("",".pid",".go",".cancel"))Files.deleteIfExists(Path.of(ready+suffix));
  }
 }
}
