import dev.blamspot.jcode.core.distro.*;
import java.io.File;
import java.util.Base64;
import java.util.Collections;

/** Execute the same compiled path planner as the APK; only prints argv data, never a shell string. */
public final class RuntimePathCli {
    private static String encode(String value) { return Base64.getEncoder().encodeToString(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
    public static void main(String[] args) {
        File directory=new File(args[0]);
        NativeTaskPaths paths=NativeTaskPaths.Companion.resolve(directory,directory,Collections.emptyList(),args[1]);
        System.out.println("cwd\t"+encode(paths.getGuestDirectory()));
        for(DistroBind bind: paths.getBinds()) {
            DistroBind actual=RuntimeMounts.INSTANCE.addressable(bind,new File(args[2]));
            System.out.println("bind\t"+encode(actual.getHost())+"\t"+encode(actual.getTarget()));
        }
    }
}
