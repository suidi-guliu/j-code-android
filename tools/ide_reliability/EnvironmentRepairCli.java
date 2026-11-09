import dev.blamspot.jcode.core.distro.*;
import java.util.*;
public class EnvironmentRepairCli {
 public static void main(String[] args) {
  NativeEnvironment language=NativeEnvironment.Cpp;
  List<NativePackage> packages=new ArrayList<>();
  for(String name:language.getPackages()) packages.add(new NativePackage(name,"1.0","1.0"));
  System.out.print(new NativeInstallPlan(language,packages,"ubuntu","amd64",false).script("/tmp/sui-env-cancel-regression"));
 }
}
