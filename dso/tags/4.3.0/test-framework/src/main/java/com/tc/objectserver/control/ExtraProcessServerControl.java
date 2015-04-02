/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.objectserver.control;

import org.apache.commons.lang.ArrayUtils;
import org.terracotta.test.util.JMXUtils;
import org.terracotta.test.util.TestProcessUtil;

import com.tc.admin.TCStop;
import com.tc.admin.common.MBeanServerInvocationProxy;
import com.tc.cli.CommandLineBuilder;
import com.tc.cli.ManagementToolUtil;
import com.tc.config.Directories;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lcp.LinkedJavaProcess;
import com.tc.management.beans.L2DumperMBean;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.process.StreamCopier;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TestConfigObject;
import com.tc.test.TestConfigUtil;
import com.tc.test.config.builder.ClusterManager;
import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.OutputStream;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.management.MBeanServerConnection;
import javax.management.remote.JMXConnector;
import javax.ws.rs.client.WebTarget;

public class ExtraProcessServerControl extends ServerControlBase {
  private static final String  DEFAULT_MIN_HEAP          = "-Xms128m";
  private static final String  DEFAULT_MAX_HEAP          = "-Xmx128m";
  private static final String  DEFAULT_MAX_DIRECT_MEMORY = "-XX:MaxDirectMemorySize=1g";
  private static final String  NOT_DEF                   = "";
  private static final String  ERR_STREAM                = "ERR";
  private static final String  OUT_STREAM                = "OUT";
  private final long           SHUTDOWN_WAIT_TIME        = 2 * 60 * 1000;

  private final String         name;
  private final boolean        mergeOutput;

  protected LinkedJavaProcess  process;
  protected File               javaHome;
  protected final String       configFileLoc;
  protected final List<String> jvmArgs;
  private File                 runningDirectory;
  private String               serverName;
  private OutputStream         outStream;
  private StreamCopier         outCopier;
  private StreamCopier         errCopier;
  private final boolean        useIdentifier;

  public Collection<String>    additionalArgs            = new ArrayList<String>();

  // constructor 1: used by container tests
  public ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean mergeOutput) {
    this(new DebugParams(), host, tsaPort, adminPort, configFileLoc, mergeOutput);
  }

  // constructor 2: used by ExtraL1ProceddControl and constructor 1
  public ExtraProcessServerControl(DebugParams debugParams, String host, int tsaPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput) {
    this(debugParams, host, tsaPort, adminPort, configFileLoc, null, mergeOutput, null, new ArrayList(), NOT_DEF, null,
         false);
  }

  public ExtraProcessServerControl(DebugParams params, String host, int tsaPort, int adminPort, String configFileLoc,
                                   boolean mergeOutput, List jvmArgs) {
    this(params, host, tsaPort, adminPort, configFileLoc, null, mergeOutput, null, jvmArgs, NOT_DEF, null, false);
  }

  // constructor 3: used by ControlSetup, Setup, and container tests
  public ExtraProcessServerControl(DebugParams debugParams, String host, int tsaPort, int adminPort,
                                   String configFileLoc, File runningDirectory, boolean mergeOutput, List jvmArgs,
                                   String undefString) {
    this(debugParams, host, tsaPort, adminPort, configFileLoc, runningDirectory, mergeOutput, null, jvmArgs,
         undefString, null, false);
  }

  // constructor 4: used by TransparentTestBase for single failure case
  public ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   File javaHome) {
    this(new DebugParams(), host, tsaPort, adminPort, configFileLoc, mergeOutput, javaHome);
  }

  public ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   File javaHome, List jvmArgs) {
    this(new DebugParams(), host, tsaPort, adminPort, configFileLoc, mergeOutput, javaHome, jvmArgs);
  }

  // constructor 5: used by active-passive tests
  public ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   String servername, List additionalJvmArgs, File javaHome, boolean useIdentifier) {
    this(new DebugParams(), host, tsaPort, adminPort, configFileLoc, null, mergeOutput, servername, additionalJvmArgs,
         NOT_DEF, javaHome, useIdentifier);
  }

  public ExtraProcessServerControl(String host, int tsaPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   String servername, List additionalJvmArgs, File javaHome, boolean useIdentifier,
                                   File runningDirectory) {
    this(new DebugParams(), host, tsaPort, adminPort, configFileLoc, runningDirectory, mergeOutput, servername,
         additionalJvmArgs, NOT_DEF, javaHome, useIdentifier);
  }

  // constructor 6: used by constructor 4, crash tests, and normal tests running in 1.4 jvm
  public ExtraProcessServerControl(DebugParams debugParams, String host, int tsaPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput, File javaHome) {
    this(debugParams, host, tsaPort, adminPort, configFileLoc, null, mergeOutput, null, new ArrayList(), NOT_DEF,
         javaHome, false);
  }

  public ExtraProcessServerControl(DebugParams debugParams, String host, int tsaPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput, File javaHome, List jvmArgs) {
    this(debugParams, host, tsaPort, adminPort, configFileLoc, null, mergeOutput, null, jvmArgs, NOT_DEF, javaHome,
         false);
  }

  private void pruneJVMArgsList(List<String> vmArgs) {
    // Arguments added later in the list have precedence.
    try {
      Map<String, String> map = new HashMap<String, String>();
      for (String vmArg : vmArgs) {
        String[] kv = vmArg.split("=");
        if (kv.length == 2) {
          if (map.containsKey(kv[0]) && !map.get(kv[0]).equals(kv[1])) {
            System.out.println("Overriding duplicate JVM arg definition " + kv[0] + "=" + map.get(kv[0]) + " with "
                               + kv[0] + "=" + kv[1]);
          }
          map.put(kv[0], kv[1]);
        } else {
          map.put(kv.length == 1 ? kv[0] : vmArg, null);
        }
      }
      vmArgs.clear();
      for (Entry<String, String> entry : map.entrySet()) {
        if (entry.getValue() != null) {
          vmArgs.add(entry.getKey() + "=" + entry.getValue());
        } else {
          vmArgs.add(entry.getKey());
        }
      }
    } catch (Exception e) {
      System.err.println("XXX error in pruneJVMArgs " + e);
    }
  }

  // only called by constructors in this class
  protected ExtraProcessServerControl(DebugParams debugParams, String host, int tsaPort, int adminPort,
                                      String configFileLoc, File runningDirectory, boolean mergeOutput,
                                      String serverName, List additionalJvmArgs, String undefString, File javaHome,
                                      boolean useIdentifier) {
    super(host, tsaPort, adminPort);
    this.useIdentifier = useIdentifier;
    this.javaHome = javaHome;
    this.serverName = serverName;
    jvmArgs = new ArrayList();

    this.configFileLoc = configFileLoc;
    this.mergeOutput = mergeOutput;
    this.name = "DSO process @ " + getHost() + ":" + getTsaPort() + ", jmx-port:" + adminPort;
    this.runningDirectory = runningDirectory;

    addProductKeyIfExists(jvmArgs);
    jvmArgs.add("-Dtc.base-dir=" + System.getProperty("tc.base-dir"));
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME + "=true");
    debugParams.addDebugParamsTo(jvmArgs);
    jvmArgs.add("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.TC_MANAGEMENT_TEST_MBEANS_ENABLED
                + "=true");
    addLibPath(jvmArgs);
    addEnvVarsForWindows(jvmArgs);
    addTCPropsJvmArgsFromCurrentJvm(jvmArgs);

    if (!Vm.isIBM() && !(Os.isMac() && Vm.isJDK14())) {
      jvmArgs.add("-XX:+HeapDumpOnOutOfMemoryError");
    }

    jvmArgs.add("-Dcom.tc.management.war="
                + ClusterManager.findWarLocation("org.terracotta", "management-tsa-war",
                                                 ClusterManager.guessMavenArtifactVersion()));

    // Add test defined parameters last so they have the final word in what gets passed to the spawned process.
    if (additionalJvmArgs != null) {
      for (Iterator i = additionalJvmArgs.iterator(); i.hasNext();) {
        String next = (String) i.next();
        if (!next.equals(undefString)) {
          this.jvmArgs.add(next);
        }
      }
    }
    // pruneJVMArgsList(jvmArgs);
    setDefaultHeapIfUnspecified();
  }

  private void setDefaultHeapIfUnspecified() {
    boolean hasMin = false;
    boolean hasMax = false;
    boolean hasDirect = false;

    for (String arg : jvmArgs) {
      arg = arg.trim();
      if (arg.startsWith("-Xms")) {
        hasMin = true;
      } else if (arg.startsWith("-Xmx")) {
        hasMax = true;
      } else if (arg.startsWith("-XX:MaxDirectMemorySize")) {
        hasDirect = true;
      }
    }

    if (!hasMin) {
      jvmArgs.add(DEFAULT_MIN_HEAP);
    }

    if (!hasMax) {
      jvmArgs.add(DEFAULT_MAX_HEAP);
    }

    if (!hasDirect) {
      jvmArgs.add(DEFAULT_MAX_DIRECT_MEMORY);
    }
  }

  protected void addProductKeyIfExists(List args) {
    String propertyKey = TCPropertiesImpl.SYSTEM_PROP_PREFIX + TCPropertiesConsts.PRODUCTKEY_PATH;
    String productKeyPath = System.getProperty(propertyKey);
    if (productKeyPath != null) {
      args.add("-D" + propertyKey + "=" + productKeyPath);
      System.out.println("XXX ading to server process: " + "-D" + propertyKey + "=" + productKeyPath);
    }
  }

  private String getStreamIdentifier(int tsaPort, String streamType) {
    String portString = "" + tsaPort;
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
    String tcBaseDir = TestConfigUtil.getTcBaseDirPath();
    args.add("-Dtc.base-dir=" + tcBaseDir);
    String val = System.getProperty("tc.tests.info.property-files");
    if (val != null && !val.trim().equals("")) {
      args.add("-Dtc.tests.info.property-files=" + val);
    }
  }

  private void addTCPropsJvmArgsFromCurrentJvm(List args) {
    RuntimeMXBean mxbean = ManagementFactory.getRuntimeMXBean();
    try {
      for (String parentJvmArg : mxbean.getInputArguments()) {
        // DEV-5808: Replace the explicit check for tc properties define with something
        // that can properly account for all properties that should not be passed
        // through.
        if (parentJvmArg.startsWith("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX)
            && !parentJvmArg.startsWith("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + "properties")
            && !parentJvmArg.contains(TCPropertiesConsts.PRODUCTKEY_PATH)) {
          args.add(parentJvmArg);
        }
      }
    } catch (SecurityException e) {
      System.out.println("Unable to add TCProperties defines from parent JVM. Error: " + e.getMessage());
    }
  }

  @Override
  public void mergeSTDOUT() {
    if (useIdentifier) {
      process.mergeSTDOUT(getStreamIdentifier(getTsaPort(), OUT_STREAM));
    } else {
      process.mergeSTDOUT();
    }
  }

  @Override
  public void mergeSTDERR() {
    if (useIdentifier) {
      process.mergeSTDERR(getStreamIdentifier(getTsaPort(), ERR_STREAM));
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

  public void setRunningDirectory(final File runningDirectory) {
    this.runningDirectory = runningDirectory;
  }

  /**
   * The JAVA_HOME for the JVM to use when creating a {@link LinkedJavaProcess}.
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

  protected List<String> getMainClassArguments() {
    final List<String> args = new ArrayList<String>();
    if (serverName != null && !serverName.equals("")) {
      args.addAll(Arrays.asList(StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc,
                                StandardConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName,
                                "-force"));
    } else {
      args.addAll(Arrays.asList(StandardConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc,
                                "-force"));
    }
    args.addAll(additionalArgs);
    return args;
  }

  public void writeOutputTo(OutputStream outputStream) {
    if (mergeOutput) { throw new IllegalStateException(); }
    this.outStream = outputStream;
  }

  @Override
  public void start() throws Exception {
    startWithoutWait();
    waitUntilStarted();
    System.err.println(this.name + " started.");
  }

  public void startAndWait(long seconds) throws Exception {
    startWithoutWait();
    waitUntilStarted(seconds);
  }

  @Override
  public void startWithoutWait() throws Exception {
    System.err.println("Starting " + this.name + ", main=" + getMainClassName() + ", main args="
                       + ArrayUtils.toString(getMainClassArguments()) + ", jvm=[" + getJavaHome() + "]");
    process = createLinkedJavaProcess();
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

  protected LinkedJavaProcess createLinkedJavaProcess(String mainClassName, List<String> args, List<String> jvmargs) {
    VerboseGCHelper.getInstance().setupVerboseGcLogging(jvmargs, serverName, mainClassName);
    LinkedJavaProcess result = new LinkedJavaProcess(mainClassName, args, jvmargs);
    String l2Classpath = TestConfigObject.getInstance().l2Classpath();
    if (l2Classpath != null) {
      result.setClasspath(l2Classpath + File.pathSeparator + TestConfigObject.getInstance().linkedChildProcessPath());
    }
    result.setMaxRuntime(TestConfigObject.getInstance().getJunitTimeoutInSeconds() + 180);
    result.setDirectory(this.runningDirectory);
    File processJavaHome = getJavaHome();
    if (processJavaHome != null) {
      result.setJavaHome(processJavaHome);
    }
    return result;
  }

  protected LinkedJavaProcess createLinkedJavaProcess() {
    return createLinkedJavaProcess(getMainClassName(), getMainClassArguments(), jvmArgs);
  }

  @Override
  public void crash() throws Exception {
    System.out.println("Crashing server " + this.name + "...");
    if (process != null) {
      process.destroy();
      waitUntilShutdown();
    }
    System.out.println(this.name + " crashed.");
  }

  @Override
  public void attemptForceShutdown() throws Exception {
    attemptForceShutdownInternal(false, null, null);
  }

  public void attemptForceShutdown(String username, String passwd) throws Exception {
    attemptForceShutdownInternal(false, username, passwd);
  }

  public void attemptForceShutdownSecured(String username, String passwd) throws Exception {
    attemptForceShutdownInternal(true, username, passwd);
  }

  private void attemptForceShutdownInternal(boolean secured, String username, String passwd) throws Exception {
    System.out.println("Force Shutting down server (secured=" + secured
                       + (username != null ? ", username: " + username : "")
                       + (passwd != null ? ", passwd: " + passwd : "") + ")");
    List<String> mainClassArguments = new ArrayList<String>();
    mainClassArguments.addAll(getMainClassArguments());
    mainClassArguments.add("-force");
    if (secured) {
      mainClassArguments.add("-k");
      mainClassArguments.add("-s");
    }
    if (username != null) {
      mainClassArguments.add("-u");
      mainClassArguments.add(username);
    }
    if (passwd != null) {
      mainClassArguments.add("-w");
      mainClassArguments.add(passwd);
    }
    TCStop.stop(mainClassArguments.toArray(new String[mainClassArguments.size()]));
  }

  @Override
  public void shutdown() throws Exception {
    try {
      attemptForceShutdown();
    } catch (Exception e) {
      System.err.println("Attempt to shutdown server but it might have already crashed: " + e.getMessage());
    }
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  public void shutdown(String username, String passwd) throws Exception {
    try {
      attemptForceShutdown(username, passwd);
    } catch (Exception e) {
      System.err.println("Attempt to shutdown server but it might have already crashed: " + e.getMessage());
    }
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  public void shutdownSecured(String username, String passwd) throws Exception {
    try {
      attemptForceShutdownSecured(username, passwd);
    } catch (Exception e) {
      System.err.println("Attempt to shutdown server (securely) but it might have already crashed: " + e.getMessage());
    }
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  private void waitUntilStarted() throws Exception {
    while (true) {
      if (isRunning()) return;
      try {
        throw new IllegalStateException("process exited with: " + process.exitValue());
      } catch (IllegalThreadStateException state) {
        // can continue
      }
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

  @Override
  public void waitUntilShutdown() throws Exception {
    long start = System.currentTimeMillis();
    long timeout = start + SHUTDOWN_WAIT_TIME;
    while (isRunning()) {
      Thread.sleep(1000);
      if (System.currentTimeMillis() > timeout) {
        System.out.println("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME);
        dumpServerControl();
        throw new Exception("Server was shutdown but still up after " + SHUTDOWN_WAIT_TIME + " ms");
      }
    }
  }

  @Override
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

    private void addDebugParamsTo(List jvmArgs) {
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
      jmxConnector = JMXUtils.getJMXConnector("localhost", getAdminPort());
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
      System.out.println("XXX Thread dumping server=[" + getTsaPort() + "]");
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

  public void waitUntilL2BecomesActive() {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", getAdminPort());
      MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      TCServerInfoMBean infoMBean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, false);
      while (true) {
        try {
          if (!infoMBean.isActive()) {
            System.out.println("waiting for L2 to become active...");
            Thread.sleep(1000);
          } else {
            return;
          }
        } catch (Exception e) {
          // ignore
        }
      }
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

  public void waitUntilL2BecomesPassiveStandBy() {
    JMXConnector jmxConnector = null;
    try {
      jmxConnector = JMXUtils.getJMXConnector("localhost", getAdminPort());
      MBeanServerConnection mbs = jmxConnector.getMBeanServerConnection();
      TCServerInfoMBean infoMBean = MBeanServerInvocationProxy.newMBeanProxy(mbs, L2MBeanNames.TC_SERVER_INFO,
                                                                             TCServerInfoMBean.class, false);
      while (true) {
        try {
          if (!infoMBean.isPassiveStandby()) {
            System.out.println("waiting for L2 to become passive standby...");
            Thread.sleep(1000);
          } else {
            return;
          }
        } catch (Exception e) {
          // ignore
        }
      }
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

  @Override
  public void pauseServer(long pauseTimeMillis) throws InterruptedException {
    TestProcessUtil.pauseProcess(process, pauseTimeMillis);

  }

  @Override
  public void pauseServer() throws InterruptedException {
    TestProcessUtil.pauseProcess(process);
  }

  @Override
  public void unpauseServer() throws InterruptedException {
    TestProcessUtil.unpauseProcess(process);
  }
}
