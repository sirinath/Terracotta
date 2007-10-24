/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package launch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.junit.launcher.JUnitLaunchShortcut;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstall2;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorPart;

import refreshall.Activator;
import refreshall.Activator.ConsoleStream;
import util.DirectoryCleaner;

public class LaunchShortcut extends JUnitLaunchShortcut implements
    IJavaLaunchConfigurationConstants {

  private static final String J2SE_14               = JDKEnvironment.J2SE_1_4.getJavaHome().getAbsolutePath();
  private static final String J2SE_15               = JDKEnvironment.J2SE_1_5.getJavaHome().getAbsolutePath();

  private static final String TESTS_PREP_PROP_LOC   = "common"
                                                        + File.separator
                                                        + "build.eclipse"
                                                        + File.separator
                                                        + "tests.base.classes"
                                                        + File.separator
                                                        + "tests-prepared.properties";
  // private static final String BUILD_PATH = "";
  private static final String VM_ARGS_COUNT         = "tcbuild.prepared.jvmargs";
  private static final String VM_ARG                = "tcbuild.prepared.jvmarg_";
  private static final String SYS_PROP_PREFIX       = "tcbuild.prepared.system-property.";
  private static final String JVM_VERSION           = "tcbuild.prepared.jvm.version";
  private static final String TESTS_INFO_PROP_FILES = SYS_PROP_PREFIX
                                                        + "tc.tests.info.property-files";
  private static final String TCBUILD               = "tcbuild";
  private static final byte[] NEWLINE               = "\n".getBytes();

  private Properties          argTypes;
  private File                prepProps;

  public void launch(IEditorPart editor, String mode) {
    IJavaElement element = JavaUI.getEditorInputJavaElement(editor.getEditorInput());
    if (element != null) {
      try {
        checkPrep(element);
      } catch (Exception e) {
        e.printStackTrace();
        return;
      }
    }
    super.launch(editor, mode);
  }

  public void launch(ISelection selection, String mode) {
    if (selection instanceof StructuredSelection) {
      Object[] elems = ((IStructuredSelection) selection).toArray();
      if (elems.length > 0 && elems[0] instanceof IJavaElement) {
        try {
          checkPrep((IJavaElement) elems[0]);
        } catch (Exception e) {
          e.printStackTrace();
          return;
        }
      }
    }
    super.launch(selection, mode);
  }

  public void checkPrep(final IJavaElement element) throws Exception {
    String relativePath = element.getPath().toString();
    String absolutePath = element.getResource().getLocation().toString();
    String basePath = absolutePath.substring(0, absolutePath.length()
        - relativePath.length() + 1);
    prepProps = new File(basePath + TESTS_PREP_PROP_LOC);
    String[] parts = relativePath.split("/");
    String module = parts[1];
    String subtree = parts[2];
    File workingDirectory = new File(basePath);
    File externallyRunTestFolder = new File(basePath, "build/externally-run-tests/" + module);
    
    if (!externallyRunTestFolder.exists())
      runCheckPrep(workingDirectory, module, subtree, basePath);
    loadProperties(prepProps, module, subtree, workingDirectory, basePath);
    cleanTempAndDataDirectories();
  }

  private void loadProperties(File prepProps, String module, String subtree,
      File wkDir, String basePath) throws Exception {
    Properties properties = new Properties();
    properties.load(new FileInputStream(prepProps));
    if (!validatePrep(properties, module, subtree)) {
      runCheckPrep(wkDir, module, subtree, basePath);
      loadProperties(prepProps, module, subtree, wkDir, basePath);
    } else {
      argTypes = properties;
    }
  }

  protected ILaunchConfigurationWorkingCopy createLaunchConfiguration(
      IJavaElement element) throws CoreException {
    if (argTypes == null) {
      RuntimeException re = new RuntimeException("vmArgs null, this should never happen. JUnit impl must have changed.");
      throw re;
    }

    try {
      ILaunchConfigurationWorkingCopy wc = super.createLaunchConfiguration(element);

      setInstalledJRE(argTypes.getProperty(JVM_VERSION), wc);

      final StringBuffer vmArgs = new StringBuffer();
      int argsCount = new Integer(argTypes.getProperty(VM_ARGS_COUNT)).intValue();
      for (int i = 0; i < argsCount; i++) {
        vmArgs.append(argTypes.getProperty(VM_ARG + i) + " ");
      }
      Enumeration enumx = argTypes.propertyNames();
      while (enumx.hasMoreElements()) {
        String key = (String) enumx.nextElement();
        if (key.startsWith(SYS_PROP_PREFIX)) {
          String value = (String) argTypes.getProperty(key);
          if (value.indexOf(' ') > 0) {
            value = "\"" + value + "\"";
          }
          vmArgs.append("-D"
              + key.substring(SYS_PROP_PREFIX.length(), key.length()) + "="
              + value + " ");
        }
      }
      if (vmArgs.length() > 0) {
        info("Setting VM args: " + vmArgs);
        wc.setAttribute(ATTR_VM_ARGUMENTS, vmArgs.toString());
      } else {
        info("Not setting VM args");
      }

      return wc;

    } catch (CoreException ce) {
      info(getStackTrace(ce));
    }

    throw new RuntimeException();
  }

  private void runCheckPrep(final File workingDirectory, final String module,
      final String subtree, final String basePath) throws Exception {
    if (!subtree.startsWith("tests.")) {
      throw new IllegalArgumentException("Subtree[" + subtree
          + "] must start with \"tests.\"");
    }
    if (!(subtree.endsWith("unit") || subtree.endsWith("system"))) {
      final IllegalArgumentException iae = new IllegalArgumentException("Subtree["
          + subtree + "] must end with \"unit\" or \"system\"");
      throw iae;
    }
    final String testType = subtree.replaceFirst("tests\\.", "");

    final List<String> commandLineList = new LinkedList<String>();
    if (Platform.OS_WIN32.equals(Platform.getOS())) {
      commandLineList.add("cmd");
      commandLineList.add("/c");
    }
    commandLineList.add(basePath + TCBUILD);
    commandLineList.add("check_prep");
    commandLineList.add(module);
    commandLineList.add(testType);
    commandLineList.add("--no-ivy");
    commandLineList.add("tests-jdk=1.5");

    final String[] commandLine = new String[commandLineList.size()];
    commandLineList.toArray(commandLine);

    Map<String, String> env = System.getenv();
    final Map<String, String> modifiedEnv = new HashMap<String, String>(env);
    modifiedEnv.put("JAVA_HOME", J2SE_15);
    modifiedEnv.put("J2SE_14", J2SE_14);
    modifiedEnv.put("J2SE_15", J2SE_15);
    final List<String> environmentList = new LinkedList<String>();
    for (Iterator<String> pos = modifiedEnv.keySet().iterator(); pos.hasNext();) {
      final String key = pos.next();
      final String value = modifiedEnv.get(key);
      environmentList.add(key + "=" + value);
    }
    final String[] environment = new String[modifiedEnv.size()];
    environmentList.toArray(environment);

    info("Running check prep: " + commandLineList + "\n");
    final ExternalJob tcbuild = new ExternalJob(TCBUILD, commandLine, environment, workingDirectory);
    tcbuild.setPriority(Job.BUILD);
    tcbuild.setUser(true);
    tcbuild.schedule();
    // We have the UI foreground at this point, in order to let the UI show
    // tcbuild output we have to yield here,
    // joining by group does just that
    Platform.getJobManager().join(TCBUILD, null);
    tcbuild.join();
  }

  private void setInstalledJRE(String jreVersion,
      ILaunchConfigurationWorkingCopy wc) throws CoreException {
    boolean jreAvailable = false;
    IVMInstallType[] installTypes = JavaRuntime.getVMInstallTypes();
    for (int i = 0; i < installTypes.length; i++) {
      IVMInstall[] installs = installTypes[i].getVMInstalls();
      for (int j = 0; j < installs.length; j++) {
        if (installs[j] instanceof IVMInstall2) {
          IVMInstall2 install2 = (IVMInstall2) installs[j];
          if (jreVersion.startsWith(install2.getJavaVersion())) {
            wc.setAttribute(ATTR_JRE_CONTAINER_PATH,
                JavaRuntime.newJREContainerPath(installs[j]).toPortableString());
            jreAvailable = true;
          }
        }
      }
    }
    if (!jreAvailable) {
      String msg = "Java Version: " + jreVersion
          + " not available as an installed JRE in Eclipse.";
      info(msg);
      Status status = new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, msg, null);
      throw new CoreException(status);
    } else {
      info("Using JRE Version: " + jreVersion);
    }
  }

  private boolean validatePrep(Properties properties, String module,
      String subtree) {
    if (!properties.getProperty("tcbuild.prepared.module", "").equals(module)) {
      removeProps();
      return false;
    }
    if (!properties.getProperty("tcbuild.prepared.subtree", "").equals(subtree)) {
      removeProps();
      return false;
    }
    return true;
  }

  /**
   * For some reason tests fail when the temp-root and data-root directories are
   * not clean. I don't care why, I just want them clean so we can run tests in
   * Eclipse more than once between check_prep calls.
   * 
   * @throws FileNotFoundException
   * @throws IOException
   */
  private void cleanTempAndDataDirectories() throws FileNotFoundException,
      IOException {
    final String testInfoPropertiesLocation = argTypes != null ? argTypes.getProperty(TESTS_INFO_PROP_FILES)
        : null;
    if (testInfoPropertiesLocation != null) {
      final File testInfoPropertiesFile = new File(testInfoPropertiesLocation);
      if (testInfoPropertiesFile.exists() && testInfoPropertiesFile.canRead()) {
        final String[] keysForDirectoriesToClean = new String[] {
            "tc.tests.info.temp-root", "tc.tests.info.data-root" };
        final Properties testInfoProps = new Properties();
        testInfoProps.load(new FileInputStream(testInfoPropertiesFile));
        for (int pos = 0; pos < keysForDirectoriesToClean.length; ++pos) {
          final String dirToCleanLocation = testInfoProps.getProperty(keysForDirectoriesToClean[pos]);
          if (dirToCleanLocation != null) {
            final File dirToClean = new File(dirToCleanLocation);
            if (dirToClean.exists() && dirToClean.isDirectory()
                && dirToClean.canWrite()) {
              info("Cleaning directory: " + dirToCleanLocation);
              DirectoryCleaner.cleanDirectory(dirToClean);
            }
          }
        }
      }
    }
  }

  // there is a race condition here but it will never occur
  private void removeProps() {
    if (prepProps.exists())
      prepProps.delete();
  }

  private void info(final String line) {
    consoleMessage(ConsoleStream.DEFAULT, line);
  }

  private void consoleMessage(final ConsoleStream stream, final String message) {
    try {
      synchronized (stream) {
        stream.stream().println(message);
        stream.stream().flush();
      }
    } catch (IOException ioe) {
      synchronized (System.err) {
        System.err.println("Unable to send line to console --> " + message);
        System.err.flush();
      }
    }
  }

  private String getStackTrace(final Throwable t) {
    final StringBuffer output = new StringBuffer(t.getMessage());
    final StackTraceElement[] stack = t.getStackTrace();
    for (int pos = 0; pos < stack.length; ++pos) {
      output.append("\tat ").append(stack[pos].toString()).append(NEWLINE);
    }
    final Throwable cause = t.getCause();
    if (cause != null && cause != t) {
      output.append("Caused by: " + getStackTrace(cause));
    }
    return output.toString();
  }

  private final class ExternalJob extends Job {

    private final class StreamToConsoleRunner implements ISafeRunnable {

      private final InputStream   stream;
      private final ConsoleStream consoleStream;

      StreamToConsoleRunner(final InputStream stream,
          final ConsoleStream consoleStream) {
        this.stream = stream;
        this.consoleStream = consoleStream;
      }

      public void run() {
        final BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        try {
          String line;
          while ((line = reader.readLine()) != null) {
            consoleMessage(consoleStream, line);
          }
        } catch (IOException e) {
          throw new RuntimeException(e);
        } finally {
          try {
            reader.close();
          } catch (IOException e) {
            // oh well
          }
        }
      }

      public void handleException(final Throwable exception) {
        consoleMessage(consoleStream, getStackTrace(exception));
      }

    }

    private final String[] commandLine;
    private final String[] environment;
    private final File     workingDirectory;

    ExternalJob(final String name, final String[] commandLine,
        final String[] environment, final File workingDirectory) {
      super("External command[" + name + "]");
      this.commandLine = commandLine;
      this.environment = environment;
      this.workingDirectory = workingDirectory;
    }

    public boolean belongsTo(final Object family) {
      return family == TCBUILD ? true : super.belongsTo(family);
    }

    protected IStatus run(final IProgressMonitor monitor) {
      monitor.beginTask(getName(), 2);
      final Process process;
      try {
        process = Runtime.getRuntime().exec(commandLine, environment,
            workingDirectory);
        monitor.worked(1);
        copyStreamsToConsoleInBackground(process.getInputStream(),
            process.getErrorStream());
        final int exitCode = process.waitFor();
        monitor.worked(1);
        monitor.done();
        return exitCode == 0 ? Status.OK_STATUS
            : new Status(IStatus.ERROR, Activator.PLUGIN_ID, exitCode, "External command failed", null);
      } catch (IOException ioe) {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "I/O exception when executing external command", ioe);
      } catch (InterruptedException ie) {
        return new Status(IStatus.ERROR, Activator.PLUGIN_ID, 1, "Interrupted while waiting for executing external command to finish", ie);
      }
    }

    private void copyStreamsToConsoleInBackground(final InputStream out,
        final InputStream err) {
      // We can't afford to schedule these as normal jobs, as pipe buffers fill
      // up we must guarantee that the output is
      // being read so we don't block the external program so we use normal
      // background threads here
      final Thread outWriter = new Thread() {
        public void run() {
          SafeRunner.run(new StreamToConsoleRunner(out, ConsoleStream.STDOUT));
        }
      };
      final Thread errWriter = new Thread() {
        public void run() {
          SafeRunner.run(new StreamToConsoleRunner(err, ConsoleStream.STDERR));
        }
      };
      outWriter.setDaemon(true);
      errWriter.setDaemon(true);
      outWriter.start();
      errWriter.start();
    }

  }
}
