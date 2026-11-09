package android.system;
/** Test-only Linux binding. Calls the real kernel through kill(1) and unix file attributes. */
public class Os {
  public static StructStat stat(String path) throws Exception { return new StructStat(((Number)java.nio.file.Files.getAttribute(java.nio.file.Path.of(path),"unix:uid")).intValue()); }
  public static long sysconf(int name) { return 4096; }
  public static void kill(int pid, int signal) throws Exception {
    if(pid<=1) throw new IllegalArgumentException("Refusing system PID");
    Process p=new ProcessBuilder("/bin/kill","-"+signal,"--",String.valueOf(pid)).start();
    if(p.waitFor()!=0) throw new java.io.IOException("kill failed: "+pid);
  }
}
