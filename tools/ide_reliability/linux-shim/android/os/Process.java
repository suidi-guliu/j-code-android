package android.os;
public class Process {
  public static int myPid() { return (int)ProcessHandle.current().pid(); }
  public static int myUid() { try { return ((Number)java.nio.file.Files.getAttribute(java.nio.file.Path.of("/proc/self"),"unix:uid")).intValue(); } catch(Exception e) { throw new RuntimeException(e); } }
}
