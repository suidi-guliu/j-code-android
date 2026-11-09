import dev.blamspot.jcode.core.distro.GuestProcessTree;
public class StopOwnerCli {
 public static void main(String[] args) {
   GuestProcessTree.INSTANCE.terminateOwnerBlocking(args[0]);
   if(!GuestProcessTree.INSTANCE.pidsOwnedBy(args[0]).isEmpty()) throw new AssertionError("Owned processes leaked");
   System.out.println("PASS actual compiled Core owner cleanup: "+args[0]);
 }
}
