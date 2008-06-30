/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.tcdev.launch;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.environments.IExecutionEnvironment;
import org.eclipse.jdt.launching.environments.IExecutionEnvironmentsManager;
import org.terracotta.tcdev.refreshall.Activator;

import java.io.File;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

public final class JDKEnvironment {

  public static final JDKEnvironment J2SE_1_4   = new JDKEnvironment("J2SE-1.4");
  public static final JDKEnvironment J2SE_1_5   = new JDKEnvironment("J2SE-1.5");
  public static final JDKEnvironment JavaSE_1_6 = new JDKEnvironment("JavaSE-1.6");

  private final String               environment;

  private JDKEnvironment(final String environment) {
    this.environment = environment;
  }

  public File getJavaHome() {
    final IExecutionEnvironmentsManager envManager = JavaRuntime.getExecutionEnvironmentsManager();
    final IExecutionEnvironment jdkEnv = envManager.getEnvironment(environment);

    if (jdkEnv == null) {
      return null;
    } else {
      final IVMInstall jdk = jdkEnv.getDefaultVM();
      if (jdk == null) {
        IVMInstall[] allInstalls = jdkEnv.getCompatibleVMs();
        if (allInstalls == null || allInstalls.length == 0) {
          return null;
        } else {
          return guessProperVM(jdkEnv, allInstalls);
        }
      } else {
        return jdk.getInstallLocation().getAbsoluteFile();
      }
    }
  }

  private File guessProperVM(IExecutionEnvironment jdkEnv, IVMInstall[] allInstalls) {
    List<IVMInstall> possible = new ArrayList<IVMInstall>();

    for (int i = 0; i < allInstalls.length; i++) {
      IVMInstall install = allInstalls[i];
      if (jdkEnv.isStrictlyCompatible(install)) {
        possible.add(install);
      }
    }

    File rv = null;

    if (possible.size() > 1) {
      List<String> vmLocations = new ArrayList<String>();
      IVMInstall[] vms = possible.toArray(new IVMInstall[] {});
      for (int i = 0; i < vms.length; i++) {
        IVMInstall vm = vms[i];
        vmLocations.add(vm.getInstallLocation().getAbsolutePath());
      }

      Status status = new Status(IStatus.WARNING, "tcdev", "multiple compatible VMs available " + vmLocations
                                                           + " using the first one");
      Activator.getDefault().getLog().log(status);
    }

    if (possible.size() >= 1) {
      rv = possible.get(0).getInstallLocation().getAbsoluteFile();
    }

    if (rv == null) {
      Status status = new Status(IStatus.ERROR, "tcdev", "no suitable VM found for " + jdkEnv.getDescription());
      Activator.getDefault().getLog().log(status);
    }

    return rv;

  }

  public static void dumpTCJDKs() {
    System.err.println(J2SE_1_4.getJavaHome().getAbsolutePath());
    System.err.println(J2SE_1_5.getJavaHome().getAbsolutePath());
    System.err.println(JavaSE_1_6.getJavaHome().getAbsolutePath());
  }

  public static void dumpJDKs(final PrintStream output) {
    final IExecutionEnvironmentsManager envManager = JavaRuntime.getExecutionEnvironmentsManager();
    final IExecutionEnvironment[] jdkEnvs = envManager.getExecutionEnvironments();
    if (jdkEnvs == null || jdkEnvs.length == 0) {
      output.println("No JVM environments available");
    } else {
      for (int pos = 0; pos < jdkEnvs.length; ++pos) {
        final IExecutionEnvironment env = jdkEnvs[pos];
        output.println("Environment[" + env.getId() + "/" + env.getDescription() + "]:");
        final IVMInstall defaultVM = env.getDefaultVM();
        if (defaultVM != null) {
          output.println("\tDefault VM: " + defaultVM.getId() + " installed at "
                         + defaultVM.getInstallLocation().getAbsolutePath());
        } else {
          output.println("\tNo default VM for this environment");
        }
        final IVMInstall[] installs = env.getCompatibleVMs();
        for (int installPos = 0; installPos < installs.length; ++installPos) {
          final IVMInstall install = installs[installPos];
          output.println("\tCompatible VM: " + install.getId() + " installed at "
                         + install.getInstallLocation().getAbsolutePath());
        }
      }
    }
  }

}
