import dev.blamspot.jcode.core.lsp.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.*;
import kotlin.Unit;

/** Exact production LspSession against real clangd; no scripted LSP replies. */
public class CppDefinitionCli {
 static JSONObject cfg;
 static JSONArray checks=new JSONArray();
 static class Wire implements LspTransport {
  final Process process;
  Wire(String command,Path project,Path log)throws Exception {
   List<String> argv=new ArrayList<>();cfg.getJSONArray("proot").forEach(v->argv.add((String)v));
   Collections.addAll(argv,"/bin/bash","--noprofile","--norc","-c",command);
   process=new ProcessBuilder(argv).directory(project.toFile()).redirectError(log.toFile()).start();
  }
  public int read(byte[] b){try{return process.getInputStream().read(b);}catch(IOException e){return -1;}}
  public synchronized void write(byte[] b){try{process.getOutputStream().write(b);process.getOutputStream().flush();}catch(IOException e){throw new RuntimeException(e);}}
  public void close(){process.destroy();try{if(!process.waitFor(5,TimeUnit.SECONDS))process.destroyForcibly();}catch(InterruptedException e){Thread.currentThread().interrupt();}}
 }
 static void open(LspSession session,Path file,String language)throws Exception {String text=Files.readString(file);DebugInputCli.await(c->session.didOpen(file.toUri().toString(),language,1,text,c));}
 static void definition(LspSession session,Path source,String text,String needle,Path target,String targetNeedle,String label)throws Exception {
  int offset=text.indexOf(needle);if(offset<0)throw new AssertionError(needle);
  String before=text.substring(0,offset);int line=(int)before.chars().filter(v->v=='\n').count(),column=before.length()-before.lastIndexOf('\n')-1;
  String targetText=Files.readString(target);int targetOffset=targetText.indexOf(targetNeedle),targetLine=(int)targetText.substring(0,targetOffset).chars().filter(v->v=='\n').count(),targetColumn=targetOffset-targetText.lastIndexOf('\n',targetOffset)-1;
  long start=System.nanoTime(),end=start+TimeUnit.SECONDS.toNanos(30);List<LocationResult> locations;
  do {
   locations=DebugInputCli.await(c->session.definition(source.toUri().toString(),line,column,c));
   if(locations.stream().anyMatch(v->v.getPath().equals(target.toString())&&v.getLine()==targetLine&&v.getCharacter()==targetColumn))break;
   if(System.nanoTime()>end)throw new AssertionError(label+" expected "+target+":"+targetLine+":"+targetColumn+"; got "+locations);
   Thread.sleep(100);
  }while(true);
  JSONArray values=new JSONArray();for(LocationResult location:locations)values.put(new JSONObject().put("path",location.getPath()).put("line",location.getLine()).put("character",location.getCharacter()));
  checks.put(new JSONObject().put("case",label).put("result","PASS").put("targets",values).put("elapsedMs",TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-start)));
 }
 public static void main(String[] args)throws Exception {
  if(!"1".equals(System.getenv("SUI_OWNED_INTEGRATION")))throw new IllegalStateException("Owned Ubuntu integration required");
  cfg=new JSONObject(Files.readString(Path.of(args[0])));Path project=Path.of(cfg.getString("project")),output=Path.of(cfg.getString("evidence"));Files.createDirectories(output);
  JSONObject descriptor=new JSONObject(Files.readString(Path.of(cfg.getString("descriptor"))));
  LspServerDescriptor d=new LspServerDescriptor(descriptor.getString("serverId"),List.of("c","cpp"),"true","true",descriptor.getString("runCommand"),List.of(".c",".cpp",".hpp"),List.of("CMakeLists.txt"),descriptor.getString("initializationOptions"),"dev.suicode.cpp.ide");
  Path main=project.resolve("src/main.cpp"),header=project.resolve("include/math.hpp"),implementation=project.resolve("src/math.cpp"),cfile=project.resolve("src/c_math.c");
  for(int repeat=1;repeat<=2;repeat++){
   final int run=repeat;
   LspSession session=new LspSession(d,project.toString(),(int)ProcessHandle.current().pid(),command->{try{return new Wire(command,project,output.resolve("clangd-"+run+".stderr"));}catch(Exception e){throw new RuntimeException(e);}});
   List<String> progress=new java.util.concurrent.CopyOnWriteArrayList<>();session.setOnNotification((method,params)->{if(method.equals("$/progress"))progress.add(params.toString());return Unit.INSTANCE;});
   try {
    DebugInputCli.await(c->session.start(project.toUri().toString(),c));if(session.getState().getValue()!=LspState.READY)throw new AssertionError(session.getErrorMessage());
    for(Path file:List.of(main,header,implementation,cfile))open(session,file,file.equals(cfile)?"c":"cpp");
    definition(session,main,Files.readString(main),"add(",implementation,"add(","main add -> math.cpp (open "+repeat+")");
    definition(session,header,Files.readString(header),"add(",implementation,"add(","math.hpp declaration -> math.cpp (open "+repeat+")");
    String original=Files.readString(main),edited="// 未保存编辑同步\n"+original;
    DebugInputCli.await(c->session.didChange(main.toUri().toString(),2,edited,c));
    definition(session,main,edited,"add(",implementation,"add(","unsaved full-document edit uses new caret (open "+repeat+")");
    DebugInputCli.await(c->session.didClose(main.toUri().toString(),c));open(session,main,"cpp");
    definition(session,main,original,"add(",implementation,"add(","closed and reopened source (open "+repeat+")");
    // A declaration-only function is a legitimate navigation target, not a fabricated implementation.
    String missing=original.replace("return add(","return missing(");
    DebugInputCli.await(c->session.didChange(main.toUri().toString(),2,missing,c));
    definition(session,main,missing,"missing(",header,"missing(","missing definition returns actual declaration (open "+repeat+")");
   }finally{session.close();Files.writeString(output.resolve("progress-"+repeat+".json"),new JSONArray(progress).toString(2));}
  }
  Files.writeString(output.resolve("results.json"),checks.toString(2));System.out.println("PASS "+checks.length()+" strict production-core clangd definition checks: "+project);
 }
}
