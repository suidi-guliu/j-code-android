package android.os;

/** JVM tests supply the real host PID; Android production uses its framework implementation. */
public final class Process {
    public static int myPid() {
        try { return Integer.parseInt(new java.io.File("/proc/self").getCanonicalFile().getName()); }
        catch (java.io.IOException error) { throw new IllegalStateException(error); }
    }
}
