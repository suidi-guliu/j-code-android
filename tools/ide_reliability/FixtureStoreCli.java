import dev.suicode.packages.*;
import java.nio.file.*;
import java.util.*;
/** Owned upgrade-fixture authoring through the exact production PackageStore, never app data. */
public class FixtureStoreCli {
 public static void main(String[] args) throws Exception {
  PackageStore store=new PackageStore(Path.of(args[0]),new PackageVerifier(Collections.emptyMap()));
  byte[] bytes=Files.readAllBytes(Path.of(args[1]));
  VerifiedPack pack=store.inspect(bytes,"suik");
  store.install(bytes,"suik",new Consent(pack.getDigest(),pack.getPermissions(),true));
  store.enable(pack.getId(),Collections.emptyMap());store.close();
 }
}
