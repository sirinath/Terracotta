/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import org.apache.commons.lang.ArrayUtils;

import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.config.Directories;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.process.StreamCopier;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TestConfigObject;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;

public class ExtraProcessServerControl extends ServerControlBase {
  private static final String NOT_DEF            = "";
  private static final String ERR_STREAM         = "ERR";
  private static final String OUT_STREAM         = "OUT";
  private final long          SHUTDOWN_WAIT_TIME = 2 * 60 * 1000;

  private final String        name;
  private final boolean       mergeOutput;

  protected LinkedJavaProcess process;
  protected File              javaHome;
  protected final String      configFileLoc;
  protected final List        jvmArgs;
  private final File          runningDirectory;
  private String              serverName;
  private OutputStream        outStream;
  private StreamCopier        outCopier;
  private StreamCopier        errCopier;
  private final boolean       useIdentifier;
  private String              stopperOutput;

  // constructor 1: used by container tests
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput) {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput);
  }

  // constructor 2: used by ExtraL1ProceddControl and constructor 1
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput) {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, mergeOutput, null, new ArrayList(), NOT_DEF, null,
         false);
  }

  public ExtraProcessServerControl(DebugParams params, String host, int dsoPort, int adminPort, String configFileLoc,
                                   boolean mergeOutput, List jvmArgs) {
    this(params, host, dsoPort, adminPort, configFileLoc, null, mergeOutput, null, jvmArgs, NOT_DEF, null, false);
  }

  // constructor 3: used by ControlSetup, Setup, and container tests
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, File runningDirectory, boolean mergeOutput, List jvmArgs,
                                   String undefString) {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, runningDirectory, mergeOutput, null, jvmArgs,
         undefString, null, false);
  }

  // constructor 4: used by TransparentTestBase for single failure case
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   File javaHome) {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput, javaHome);
  }

  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   File javaHome, List jvmArgs) {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput, javaHome, jvmArgs);
  }

  // constructor 5: used by active-passive tests
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   String servername, List additionalJvmArgs, File javaHome, boolean useIdentifier) {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, null, mergeOutput, servername, additionalJvmArgs,
         NOT_DEF, javaHome, useIdentifier);
  }

  // constructor 6: used by constructor 4, crash tests, and normal tests running in 1.4 jvm
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput, File javaHome) {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, mergeOutput, null, new ArrayList(), NOT_DEF,
         javaHome, false);
  }

  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput, File javaHome, List jvmArgs) {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, mergeOutput, null, jvmArgs, NOT_DEF, javaHome,
         false);
  }

  // only called by constructors in this class
  protected ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                      String configFileLoc, File runningDirectory, boolean mergeOutput,
                                      String serverName, List additionalJvmArgs, String undefString, File javaHome,
                                      boolean useIdentifier) {
    super(host, dsoPort, adminPort);
    this.useIdentifier = useIdentifier;
    this.javaHome = javaHome;
    this.serverName = serverName;
    jvmArgs = new ArrayList();

    if (additionalJvmArgs != null) {
      for (Iterator i = additionalJvmArgs.iterator(); i.hasNext();) {
        String next = (String) i.next();
        if (!next.equals(undefString)) {
          this.jvmArgs.add(next);
        }
      }
    }

    this.configFileLoc = configFileLoc;
    this.mergeOutput = mergeOutput;
    this.name = "DSO process @ " + getHost() + ":" + getDsoPort() + ", jmx-port:" + adminPort;
    this.runningDirectory = runningDirectory;
    jvmArgs.add("-Dcom.tc.l1.modules.repositories=" + System.getProperty("com.tc.l1.modules.repositories"));
    jvmArgs.add("-Dtc.base-dir=" + System.getProperty("tc.base-dir"));
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME + "=true");
    jvmArgs.add("-Djava.net.preferIPv4Stack=true");
    debugParams.addDebugParamsTo(jvmArgs);
    jvmArgs.add("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.TC_MANAGEMENT_TEST_MBEANS_ENABLED
                + "=true");
    addLibPath(jvmArgs);
    addEnvVarsForWindows(jvmArgs);

    if (!Vm.isIBM() && !(Os.isMac() && Vm.isJDK14())) {
      jvmArgs.add("-XX:+HeapDumpOnOutOfMemoryError");
    }
  }

  private String getStreamIdentifier(int dsoPort, String streamType) {
    String portString = "" + dsoPort;
    int numSpaces = 5 - portString.length();
    for (int i = 0; i < numSpaces; i++) {
      portString = " " + portString;
    }
    return "[" + portString + "][" + streamType + "]     ";
  }

  private void addLibPath(List args) {
    String libPath = System.getProperty("java.library.path");
    if (libPath == null || libPath.equals("")) { throw new AssertionError("java.library.path is not set!"); }
    args.add("-Djava.library.path=" + libPath);
  }

  private void addEnvVarsForWindows(List args) {
    String tcBaseDir = System.getProperty("tc.base-dir");
    if (tcBaseDir == null || tcBaseDir.equals("")) { throw new AssertionError("tc.base-dir is not set!"); }
    args.add("-Dtc.base-dir=" + tcBaseDir);
    String val = System.getProperty("tc.tests.info.property-files");
    if (val != null && !val.trim().equals("")) {
      args.add("-Dtc.tests.info.property-files=" + val);
    }
  }

  public void mergeSTDOUT() {
    if (useIdentifier) {
      process.mergeSTDOUT(getStreamIdentifier(getDsoPort(), OUT_STREAM));
    } else {
      process.mergeSTDOUT();
    }
  }

  public void mergeSTDERR() {
    if (useIdentifier) {
      process.mergeSTDERR(getStreamIdentifier(getDsoPort(), ERR_STREAM));
    } else {
      process.mergeSTDERR();
    }
  }

  protected String getMainClassName() {
    return "com.tc.server.TCServerMain";
  }

  public void setServerName(String serverName) {
    this.serverName = serverName;
  }

  /**
   * The JAVA_HOME for the JVM to use when creating a {@link LinkedChildProcess}.
   */
  public File getJavaHome() {
    if (javaHome == null) {
      javaHome = new File(TestConfigObject.getInstance().getL2StartupJavaHome());
    }
    return javaHome;
  }

  public void setJavaHome(File javaHome) {
    this.javaHome = javaHome;
  }

  protected String[] getMainClassArguments() {
    if (serverName != null && !serverName.equals("")) {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc,
          StandardTVSConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName };
    } else {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc };
    }
  }

  public void writeOutputTo(OutputStream outputStream) {
    if (mergeOutput) { throw new IllegalStateException(); }
    this.outStream = outputStream;
  }

  public void start() throws Exception {
    startWithoutWait();
    waitUntilStarted();
    System.err.println(this.name + " started.");
  }

  public void startAndWait(long seconds) throws Exception {
    startWithoutWait();
    waitUntilStarted(seconds);
  }

  public void startWithoutWait() throws Exception {
    System.err.println("Starting " + this.name + ", main=" + getMainClassName() + ", main args="
                       + ArrayUtils.toString(getMainClassArguments()) + ", jvm=[" + getJavaHome() + "]");
    process = createLinkedJavaProcess();
    process.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    process.start();
    if (mergeOutput) {
      mergeSTDOUT();
      mergeSTDERR();
    } else if (outStream != null) {
      outCopier = new StreamCopier(process.STDOUT(), outStream);
      errCopier = new StreamCopier(process.STDERR(), outStream);
      outCopier.start();
      errCopier.start();
    }
  }

  protected LinkedJavaProcess createLinkedJavaProcess(String mainClassName, String[] arguments) {
    LinkedJavaProcess result = new LinkedJavaProcess(mainClassName, arguments);
    result.setMaxRuntime(TestConfigObject.getInstance().getJunitTimeoutInSeconds() + 180);
    result.setDirectory(this.runningDirectory);
    File processJavaHome = getJavaHome();
    if (processJavaHome != null) {
      result.setJavaHome(processJavaHome);
    }
    return result;
  }

  protected LinkedJavaProcess createLinkedJavaProcess() {
    return createLinkedJavaProcess(getMainClassName(), getMainClassArguments());
  }

  public void crash() throws Exception {
    System.out.println("Crashing server " + this.name + "...");
    if (process != null) {
      process.destroy();
      waitUntilShutdown();
    }
    System.out.println(this.name + " crashed.");
  }

  public void attemptShutdown() throws Exception {
    System.out.println("Shutting down server " + this.name + "...");
    String[] args = getMainClassArguments();
    LinkedJavaProcess stopper = createLinkedJavaProcess("com.tc.admin.TCStop", args);
    stopper.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    stopper.start();

    ByteArrayOutputStream stopperLog = null;
    try {
      stopperLog = new ByteArrayOutputStream();
      StreamCopier stdoutCopier = new StreamCopier(stopper.STDOUT(), stopperLog);
      StreamCopier stderrCopier = new StreamCopier(stopper.STDERR(), stopperLog);

      stdoutCopier.start();
      stderrCopier.start();

      stdoutCopier.join(60 * 1000);
      stderrCopier.join(60 * 1000);

      if (stderrCopier.isAlive() || stdoutCopier.isAlive()) {
        System.err.println("\n" + "TCStop output: " + stopperLog.toString() + "\n");
      }
    } finally {
      if (stopperLog != null) {
        stopperOutput = stopperLog.toString();
        stopperLog.close();
      }
      stopper.STDIN().close();
    }

  }

  public void shutdown() throws Exception {
    try {
      attemptShutdown();
    } catch (Exception e) {
      System.err.println("Attempt to shutdown server but it might have already crashed: " + e.getMessage());
    }
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  private void waitUntilStarted() throws Exception {
    while (true) {
      if (isRunning()) return;
      Thread.sleep(1000);
    }
  }

  private void waitUntilStarted(long timeoutInSeconds) throws InterruptedException {
    if (timeoutInSeconds < 0) throw new IllegalArgumentException("timeout can't be negative");
    long timeout = (timeoutInSeconds * 1000) + System.currentTimeMillis();
    while (System.currentTimeMillis() < timeout) {
      if (isRunning()) return;
      Thread.sleep(1000);
    }
  }

  public void waitUntilShutdown() throws Exception {
    long start = System.currentTimeMillis();
    long timeout = start + SHUTDOWN_WAIT_TIME;
    while (isRunning()) {
      Thread.sleep(1000);
      if (System.currentTimeMillis() > timeout) {
        System.err.println("TCStoper output: " + stopperOutput);
        System.out.println("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME);
        dumpServerControl();
        throw new Exception("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME + " ms");
      }
    }
  }

  public int waitFor() throws Exception {
    int rv = process.waitFor();

    if (outCopier != null) {
      try {
        outCopier.join();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (errCopier != null) {
      try {
        errCopier.join();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    if (outStream != null) {
      try {
        outStream.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    return rv;
  }

  public static final class DebugParams {
    private final boolean debug;
    private final int     debugPort;

    public DebugParams() {
      this(false, 0);
    }

    public DebugParams(int debugPort) {
      this(true, debugPort);
    }

    private DebugParams(boolean debug, int debugPort) {
      if (debugPort < 0) throw new AssertionError("Debug port must be >= 0: " + debugPort);
      this.debugPort = debugPort;
      this.debug = debug;
    }

    private void addDebugParamsTo(Collection jvmArgs) {
      if (debug) {
        jvmArgs.add("-Xdebug");
        String address = debugPort > 0 ? "address=" + debugPort + "," : "";
        jvmArgs.add("-Xrunjdwp:transport=dt_socket," + address + "server=y,suspend=n");
      }
    }
  }

  public List getJvmArgs() {
    return jvmArgs;
  }

  public void dumpServerControl() throws Exception {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = CommandLineBuilder.getJMXConnector("localhost", getAdminPort());
      MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      L2DumperMBean mbean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.DUMPER, L2DumperMBean.class,
                                                                     false);
      while (true) {
        try {
          mbean.doServerDump();
          break;
        } catch (Exception e) {
          System.out.println("Could not find L2DumperMBean... sleep for 1 sec.");
          Thread.sleep(1000);
        }
      }

      mbean.setThreadDumpCount(3);
      mbean.setThreadDumpInterval(500);
      System.out.println("XXX Thread dumping server=[" + getDsoPort() + "]");
      mbean.doThreadDump();
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (jmxConnector != null) {
        try {
          jmxConnector.close();
        } catch (Exception e) {
          System.out.println("Exception while trying to close the JMX connector for port no: " + getAdminPort());
        }
      }
    }
  }

}
