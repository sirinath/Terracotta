/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.control;

import org.apache.commons.lang.ArrayUtils;

import com.tc.admin.TCStop;
import com.tc.config.Directories;
import com.tc.config.schema.setup.StandardTVSConfigurationSetupManagerFactory;
import com.tc.process.LinkedJavaProcess;
import com.tc.process.StreamCopier;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.TCServerMain;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

public class ExtraProcessServerControl extends ServerControlBase {
  private static final String NOT_DEF    = "";
  private static final String ERR_STREAM = "ERR";
  private static final String OUT_STREAM = "OUT";

  private final String        name;
  private final boolean       mergeOutput;

  protected LinkedJavaProcess process;
  protected final String      configFileLoc;
  protected final List        jvmArgs;
  private final File          runningDirectory;
  private final String        serverName;
  private File                javaHome;
  private File                out;
  private FileOutputStream    fileOut;
  private StreamCopier        outCopier;
  private StreamCopier        errCopier;

  private final String        errStreamIdentifier;
  private final String        outStreamIdentifier;

  // constructor 1: used by container tests
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput)
      throws FileNotFoundException {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput);
  }

  // constructor 2: used by ExtraL1ProceddControl and constructor 1
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput) throws FileNotFoundException {
    // 2006-07-11 andrew -- We should get rid of the reference to Directories.getInstallationRoot() here.
    // Tests don't run in an environment where such a thing even exists. If the server needs an
    // "installation directory", the tests should be creating one themselves.
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, Directories.getInstallationRoot(), mergeOutput,
         null, new ArrayList(), NOT_DEF, null);
  }

  // constructor 3: used by ControlSetup, Setup, and container tests
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, File runningDirectory, File installationRoot,
                                   boolean mergeOutput, List jvmArgs, String undefString) {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, runningDirectory, installationRoot, mergeOutput, null,
         jvmArgs, undefString, null);
  }

  // constructor 4: used by TransparentTestBase for single failure case
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   File javaHome) throws FileNotFoundException {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, mergeOutput, javaHome);
  }

  // constructor 5: used by active-passive tests
  public ExtraProcessServerControl(String host, int dsoPort, int adminPort, String configFileLoc, boolean mergeOutput,
                                   String servername, List additionalJvmArgs, File javaHome)
      throws FileNotFoundException {
    this(new DebugParams(), host, dsoPort, adminPort, configFileLoc, null, Directories.getInstallationRoot(),
         mergeOutput, servername, additionalJvmArgs, NOT_DEF, javaHome);
  }

  // constructor 6: used by constructor 4, crash tests, and normal tests running in 1.4 jvm
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, boolean mergeOutput, File javaHome)
      throws FileNotFoundException {
    this(debugParams, host, dsoPort, adminPort, configFileLoc, null, Directories.getInstallationRoot(), mergeOutput,
         null, new ArrayList(), NOT_DEF, javaHome);
  }

  // only called by constructors in this class
  public ExtraProcessServerControl(DebugParams debugParams, String host, int dsoPort, int adminPort,
                                   String configFileLoc, File runningDirectory, File installationRoot,
                                   boolean mergeOutput, String serverName, List additionalJvmArgs, String undefString,
                                   File javaHome) {
    super(host, dsoPort, adminPort);
    errStreamIdentifier = getStreamIdentifier(dsoPort, ERR_STREAM);
    outStreamIdentifier = getStreamIdentifier(dsoPort, OUT_STREAM);
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
    this.name = "DSO process @ " + getHost() + ":" + getDsoPort();
    this.runningDirectory = runningDirectory;
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "=" + installationRoot);
    jvmArgs.add("-D" + Directories.TC_INSTALL_ROOT_IGNORE_CHECKS_PROPERTY_NAME + "=true");
    jvmArgs.add("-Djava.net.preferIPv4Stack=true");
    debugParams.addDebugParamsTo(jvmArgs);
    jvmArgs.add("-D" + TCPropertiesImpl.SYSTEM_PROP_PREFIX + ".tc.management.test.mbeans.enabled=true");
    addClasspath(jvmArgs);
    addLibPath(jvmArgs);
    addEnvVarsForWindows(jvmArgs);
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

  private void addClasspath(List args) {
    String classpath = System.getProperty("java.class.path");
    if (classpath == null || classpath.equals("")) { throw new AssertionError("java.class.path is not set!"); }
    args.add("-Djava.class.path=" + classpath);
  }

  private void addEnvVarsForWindows(List args) {
    String tcBaseDir = System.getProperty("tc.base-dir");
    if (tcBaseDir == null || tcBaseDir.equals("")) { throw new AssertionError("tc.base-dir is not set!"); }
    args.add("-Dtc.base-dir=" + tcBaseDir);
    String val = System.getProperty("tc.tests.info.property-files");
    if (val == null || val.equals("")) { throw new AssertionError("tc.tests.info.property-files is not set!"); }
    args.add("-Dtc.tests.info.property-files=" + val);
  }

  public void mergeSTDOUT() {
    this.process.mergeSTDOUT(outStreamIdentifier);
  }

  public void mergeSTDERR() {
    this.process.mergeSTDERR(errStreamIdentifier);
  }

  protected String getMainClassName() {
    return TCServerMain.class.getName();
  }

  /**
   * The JAVA_HOME for the JVM to use when creating a {@link LinkedChildProcess}.
   */
  public File getJavaHome() {
    return javaHome;
  }

  protected String[] getMainClassArguments() {
    if (serverName != null && !serverName.equals("")) {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc,
          StandardTVSConfigurationSetupManagerFactory.SERVER_NAME_ARGUMENT_WORD, serverName };
    } else {
      return new String[] { StandardTVSConfigurationSetupManagerFactory.CONFIG_SPEC_ARGUMENT_WORD, this.configFileLoc };
    }
  }

  public void writeOutputTo(File outputFile) {
    if (mergeOutput) { throw new IllegalStateException(); }
    this.out = outputFile;
  }

  public void start(long timeout) throws Exception {
    System.err.println("Starting " + this.name + ": jvmArgs=" + jvmArgs + ", main=" + getMainClassName()
                       + ", main args=" + ArrayUtils.toString(getMainClassArguments()));
    process = createLinkedJavaProcess();
    process.setJavaArguments((String[]) jvmArgs.toArray(new String[jvmArgs.size()]));
    process.start();
    if (mergeOutput) {
      mergeSTDOUT();
      mergeSTDERR();
    } else if (out != null) {
      fileOut = new FileOutputStream(out);
      outCopier = new StreamCopier(process.STDOUT(), fileOut);
      errCopier = new StreamCopier(process.STDERR(), fileOut);
      outCopier.start();
      errCopier.start();
    }
    waitUntilStarted(timeout);
    System.err.println(this.name + " started.");
  }

  protected LinkedJavaProcess createLinkedJavaProcess() {
    LinkedJavaProcess rv = new LinkedJavaProcess(getMainClassName(), getMainClassArguments());
    rv.setDirectory(this.runningDirectory);
    File processJavaHome = getJavaHome();
    if (processJavaHome != null) {
      rv.setJavaHome(processJavaHome);
    }
    return rv;
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
    TCStop stopper = new TCStop(getHost(), getAdminPort());
    stopper.stop();
  }

  public void shutdown() throws Exception {
    attemptShutdown();
    waitUntilShutdown();
    System.out.println(this.name + " stopped.");
  }

  private void waitUntilStarted(long timeout) throws Exception {
    long timeoutTime = System.currentTimeMillis() + timeout;
    while (true) {
      if (isRunning()) return;
      if (System.currentTimeMillis() > timeoutTime) {
        //
        throw new RuntimeException("Timeout occurred waiting for server to start: " + timeout + " ms.");
      }
      Thread.sleep(1000);
    }
  }

  public void waitUntilShutdown() throws Exception {
    while (isRunning()) {
      Thread.sleep(1000);
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

    if (fileOut != null) {
      try {
        fileOut.close();
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

}
