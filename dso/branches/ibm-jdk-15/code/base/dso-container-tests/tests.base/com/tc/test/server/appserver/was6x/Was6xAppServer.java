package com.tc.test.server.appserver.was6x;

import org.apache.commons.io.IOUtils;

import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.test.TestConfigObject;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AbstractAppServer;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.util.PortChooser;
import com.tc.util.runtime.Os;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class Was6xAppServer extends AbstractAppServer {
  private static final String PORTS_DEF = "ports.def";

  private String[]            scripts   = new String[] { "deployApps.py", "terracotta.py", 
                                                         "toggle-dso.py", "wait-for-shutdown.py" };

  private String              policy    = "grant codeBase \"file:FILENAME\" {" + IOUtils.LINE_SEPARATOR
                                          + "  permission java.security.AllPermission;" + IOUtils.LINE_SEPARATOR + "};"
                                          + IOUtils.LINE_SEPARATOR;
  private String              instanceName;
  private int                 webspherePort;
  private File                sandbox;
  private File                instanceDir;
  private File                dataDir;
  private File                portDefFile;
  private File                serverInstallDir;

  public Was6xAppServer(Was6xAppServerInstallation installation) {
    super(installation);
  }
  
  public ServerResult start(ServerParameters parameters) throws Exception {
    init(parameters);
    createPortFile();    
    createProfile();
    copyPythonScripts();
    deployWarFile();
    addTerracottaToServerPolicy();
    startWebsphere();
    return new AppServerResult(webspherePort, this);
  }
  
  public void stop() throws Exception {
    try {
      stopWebsphere();
    } finally {
      deleteProfile();
    }
  }

  private void createPortFile() throws Exception {
    PortChooser portChooser = new PortChooser();
    webspherePort = portChooser.chooseRandomPort();

    List lines = IOUtils.readLines(getClass().getResourceAsStream(PORTS_DEF));
    lines.set(0, (String) lines.get(0) + webspherePort);

    for (int i = 1; i < lines.size(); i++) {
      String line = (String) lines.get(i);
      lines.set(i, line + portChooser.chooseRandomPort());
    }

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(portDefFile);
      IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }

  }

  private void copyPythonScripts() throws Exception {
    for (int i = 0; i < scripts.length; i++) {
      copyResourceTo(scripts[i], new File(dataDir, scripts[i]));
    }
  }

  private void deleteProfile() throws Exception {
    String[] args = new String[] { "-delete", "-profileName", instanceName };
    System.out.println("Deleting current profile before creating a new one: " + instanceName);
    executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "Error in deleting profile for " + instanceName);
  }

  private void createProfile() throws Exception {
    String defaultTemplate = new File(serverInstallDir.getAbsolutePath(), "profileTemplates/default").getAbsolutePath();
    String[] args = new String[] { "-create", "-templatePath", defaultTemplate, "-profileName", instanceName,
        "-profilePath", instanceDir.getAbsolutePath(), "-portFile", portDefFile.getAbsolutePath(),
        "-enableAdminSecurity", "false", "-isDeveloperServer" };
    executeCommand(serverInstallDir, "manageprofiles", args, serverInstallDir, "Error in creating profile for " + instanceName);
  }

  private void addTerracottaToServerPolicy() throws Exception {
    String classpath = System.getProperty("java.class.path");
    Set set = new HashSet();
    String[] entries = classpath.split(File.pathSeparator);
    for (int i = 0; i < entries.length; i++) {
      File filename = new File(entries[i]);
      if (filename.isDirectory()) {
        set.add(filename);
      } else {
        set.add(filename.getParentFile());
      }
    }

    List lines = new ArrayList(set.size() + 1);
    for (Iterator it = set.iterator(); it.hasNext();) {
      lines.add(getPolicyFor((File) it.next()));
    }
    lines.add(getPolicyFor(new File(TestConfigObject.getInstance().normalBootJar())));

    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(new File(instanceDir, "properties/server.policy"), true);
      IOUtils.writeLines(lines, IOUtils.LINE_SEPARATOR, fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }

  private String getPolicyFor(File filename) {
    String entry = filename.getAbsolutePath().replace('\\', '/');

    if (filename.isDirectory()) {
      return policy.replaceFirst("FILENAME", entry + "/-");
    } else {
      return policy.replaceFirst("FILENAME", entry);
    }
  }

  private void copyResourceTo(String filename, File dest) throws Exception {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(dest);
      IOUtils.copy(getClass().getResourceAsStream(filename), fos);
    } finally {
      IOUtils.closeQuietly(fos);
    }
  }
  
  private void deployWarFile() throws Exception {
    String[] args = new String[] { "-lang", "jython", "-connType", "NONE", "-profileName", instanceName,
        "-javaoption", "-Dwebapp.dir=\"" + dataDir.getAbsolutePath() + "\"", "-f",
        new File(dataDir, "deployApps.py").getAbsolutePath() };
    executeCommand(instanceDir, "wsadmin", args, dataDir, "Error in deploying warfile for " + instanceName);
  }

  private void startWebsphere() throws Exception {
    String[] args = new String[] { "server1", "-profileName", instanceName };
    executeCommand(instanceDir, "startServer", args, instanceDir, "Error in starting " + instanceName);
  }

  private void stopWebsphere() throws Exception {
    String[] args = new String[] { "server1", "-profileName", instanceName };
    executeCommand(instanceDir, "stopServer", args, instanceDir, "Error in stopping " + instanceName);
  }
  
  private void init(ServerParameters parameters) {
    AppServerParameters params = (AppServerParameters) parameters;
    this.sandbox = sandboxDirectory();
    this.instanceName = params.instanceName();
    this.instanceDir = new File(sandbox, instanceName);
    this.dataDir = new File(sandbox, "data");
    this.portDefFile = new File(dataDir, PORTS_DEF);
    this.serverInstallDir = serverInstallDirectory();
  }
  
  private String getScriptPath(File root, String scriptName) {
    String fullScriptName = Os.isWindows() ? scriptName + ".bat" : scriptName + ".sh";
    return new File(root.getAbsolutePath(), "bin/" + fullScriptName).getAbsolutePath();
  }

  private void executeCommand(File rootDir, String scriptName, String[] args, File workingDir, String errorMessage) throws Exception {
    String script = getScriptPath(rootDir, scriptName);
    String[] cmd = new String[args.length+1];
    cmd[0] = script;
    System.arraycopy(args, 0, cmd, 1, args.length);
    Result result = Exec.execute(cmd, null, null, workingDir == null ? instanceDir : workingDir);
    System.out.println(result.getStdout() + IOUtils.LINE_SEPARATOR + result.getStderr());
    if (result.getExitCode() != 0) { throw new Exception(errorMessage); }
  }
}
