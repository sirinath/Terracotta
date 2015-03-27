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
package com.tc.reporter;

import org.apache.xmlbeans.XmlException;

import com.tc.config.Loader;
import com.tc.config.schema.dynamic.ParameterSubstituter;
import com.tc.object.config.schema.L2DSOConfigObject;
import com.tc.sysinfo.EnvStats;
import com.tc.util.ArchiveBuilder;
import com.tc.util.ZipBuilder;
import com.terracottatech.config.Client;
import com.terracottatech.config.Server;
import com.terracottatech.config.Servers;
import com.terracottatech.config.TcConfigDocument.TcConfig;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.xml.namespace.QName;

/**
 * This utility is used to archive Terracotta execution environment information for debugging purposes. Run the
 * <tt>main()</tt> with no arguments for usage.
 */
public final class ArchiveUtil {

  private final boolean isClient;
  private final File tcConfig;
  private final File archiveFile;
  private static final String STDOUT = "stdout:";
  private static final String STDERR = "stderr:";
  private static final String ARCHIVE_FILE_NAME = "tc-archive";
  private static final String INVALID = "Invalid Arguments:\n\n";
  private static final String DASH_C = "-c";
  private static final String USAGE = "** Terracotta Archive Tool **\n\n"
                                      + "A utility for archiving Terracotta environment information.\n\n"
                                      + "\tValid Arguments are:\n\n\t["
                                      + DASH_C
                                                       + "] (Client - include files from the terracotta client)"
                                      + "\n\t<path to terracotta config xml file (tc-config.xml)>"
                                      + " | <path to logs directory>"
                                      + "\n\t[<output filename in .zip format>]\n\nExamples:\n\n\t"
                                      + "# java "
                                      + ArchiveUtil.class.getName()
                                      + " tc-config.xml /home/someuser/tc-archive_server.zip"
                                      + "\n\tor\n\t# java "
                                      + ArchiveUtil.class.getName()
                                      + " /export1/terracotta/server-logs"
                                      + "\n\nUsage Summary:\n\n\tTypically you will use this tool to create an "
                                      + "archive of the Terracotta server logs.\n\t"
                                                       + "You may also want to create archives on the terracotta client machines using"
                                      + " the -c option.\n\t"
                                      + "There are two scenarios where you may need to use the directory location "
                                      + "instead of the config file path."
                                                       + "\n\n\t\t1. The terracotta client may not have a local copy of the tc-config.xml"
                                      + "\n\t\t2. The tc-config.xml logs elements may contain wildcards"
                                      + " which use timestamps or \n\t\t   environment variables which cannot be"
                                      + " resolved.\n\nNotes:\n\n\tThe execution command may vary:"
                                      + "\n\t\t# ./archive-util ...\n\n\tSpecifying a directory location as the"
                                      + " first command will recursively archive it's entire contents";

  private static final Set<String> validDashArgs = new HashSet<String>();

  static {
    validDashArgs.add(DASH_C);
  }

  private ArchiveUtil(boolean isClient, File archivePath, File fileName) {
    this.isClient = isClient;
    this.tcConfig = archivePath;
    if (fileName == null) {
      File userDir = new File(System.getProperty("user.dir"));
      if (!userDir.exists()) {
        throw new RuntimeException("Unexpected error - system property user.dir " +
                                   "does not resolve to an actual directory: " + userDir);
      }
      DateFormat df = new SimpleDateFormat("y-M-d");
      String name = ARCHIVE_FILE_NAME + "_" + df.format(new Date(System.currentTimeMillis())) + ".zip";
      this.archiveFile = new File(userDir + File.separator + name);
    } else {
      this.archiveFile = fileName;
    }
  }

  private static void quit(String msg) {
    System.err.println(msg);
    System.exit(0);
  }

  private static void escape(String msg, Exception e) {
    System.out.println(INVALID + msg);
    if (e != null) e.printStackTrace();
    System.exit(0);
  }

  public static void main(String[] args) {
    if (args.length < 1) escape(USAGE, null);

    boolean dashArgs = true;
    int locationCmd = -1;
    int fileArg = -1;
    Set<String> dashSet = new HashSet<String>(2);
    for (int i = 0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
        if (!dashArgs) escape(USAGE, null);

        if (validDashArgs.contains(args[i])) {
          dashSet.add(args[i]);
        } else {
          escape(USAGE, null);
        }
      } else {
        dashArgs = false;
        if (fileArg + locationCmd > 1) escape(USAGE, null);
        if (locationCmd < 0) {
          locationCmd = i;
        } else if (fileArg < 0) {
          fileArg = i;
        }
        if (fileArg + locationCmd == -2) escape(USAGE, null);
      }
    }

    if (dashSet.size() > 2) escape(USAGE, null);
    boolean dashC = dashSet.contains(DASH_C);

    if (locationCmd < 0)
      escape("Please specify the Terracotta config file location or logs directory location\n\n" + USAGE, null);
    File tcConfigFile = new File(args[locationCmd]);
    if (!tcConfigFile.exists())
      escape("\tTerracotta Configuration file: " + tcConfigFile + "\n\tdoes not exist\n\n" + USAGE, null);

    File outputFile = null;
    if (fileArg > 0) {
      outputFile = new File(new File(args[fileArg]).getAbsolutePath());
      if (!new File(outputFile.getParent()).exists())
        escape("\tThe directory specified for the output file does not exist", null);
    }

    try {
      new ArchiveUtil(dashC, tcConfigFile, outputFile).createArchive();
    } catch (IOException e) {
      escape("\tUnable to read Terracotta configuration file\n", e);
    } catch (XmlException e) {
      escape("\tUnable to parse Terracotta configuration file\n", e);
    }
  }

  private File makeAbsolute(File file) {
    if (file.isAbsolute()) return file;
    return new File(tcConfig.getParent() + File.separator + file);
  }

  private File getClientLogsLocation(TcConfig configBeans) {
    Client clients = configBeans.getClients();
    if (clients == null) {
      quit("The Terracotta config specified doesn't contain the <clients> element.\nYou may have provided a server config by mistake.");
    }

    String logs = clients.getLogs();
    if (isStdX(logs)) return null;
    if (logs == null) {
      logs = Client.type.getElementProperty(QName.valueOf("logs")).getDefaultText();
    }
    String clientLogs = ParameterSubstituter.substitute(logs);
    File clientLogsDir = makeAbsolute(new File(clientLogs));
    if (!clientLogsDir.exists()) {
      quit("\nError occured while parsing: " + tcConfig
           + "\n\tUnable to locate client log files at: " + clientLogs);
    }
    return clientLogsDir;
  }

  private boolean isStdX(String value) {
    if (value == null) return false;
    return (value.equals(STDOUT) || value.equals(STDERR));
  }

  private Server[] getServersElement(TcConfig configBeans) {
    Servers servers = configBeans.getServers();
    if (servers == null) quit("The Terracotta config specified doesn't contain the <servers> element");
    return L2DSOConfigObject.getServers(servers);
  }

  private File[] getServerLogsLocation(TcConfig configBeans) {
    Server[] servers = getServersElement(configBeans);
    String[] logs = new String[servers.length];
    File[] logFiles = new File[servers.length];
    for (int i = 0; i < servers.length; i++) {
      logs[i] = servers[i].getLogs();
      if (isStdX(logs[i])) logs[i] = null;
      if (logs[i] == null) logs[i] = Server.type.getElementProperty(QName.valueOf("logs")).getDefaultText();
      logs[i] = ParameterSubstituter.substitute(logs[i]);
      File serverLogsDir = makeAbsolute(new File(logs[i]));
      if (!serverLogsDir.exists()) {
        quit("\nError occured while parsing: " + tcConfig
             + "\n\tUnable to resolve the server log location element to an actual file: "
             + logs[i]);
      }
      logFiles[i] = serverLogsDir;
    }
    return logFiles;
  }

  private void createPathArchive() {
    try {
      System.out.println("Archiving:\n----------------------------------------");
      ArchiveBuilder zip = new ZipBuilder(archiveFile, true);
      zip.putEntry("env-stats", EnvStats.report().getBytes("UTF-8"));
      zip.putTraverseDirectory(tcConfig, tcConfig.getName());
      zip.finish();
    } catch (IOException e) {
      System.out.println("Unexpected error - unable to write Terracotta archive: " + archiveFile);
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("\n\nWrote archive to:" + archiveFile);
  }

  private void createArchive() throws IOException, XmlException {
    if (tcConfig.isDirectory()) {
      createPathArchive();
      return;
    }
    TcConfig configBeans = new Loader().parse(tcConfig).getTcConfig();
    File clientLogsDir = null;
    File[] serverLogsDir = null;

    if (isClient) {
      clientLogsDir = getClientLogsLocation(configBeans);
    } else {
      serverLogsDir = getServerLogsLocation(configBeans);
    }

    try {
      ArchiveBuilder zip = new ZipBuilder(archiveFile, true);
      System.out.println("Archiving:");
      zip.putEntry(tcConfig.getName(), zip.readFile(tcConfig));
      if (isClient) {
        if (clientLogsDir != null) zip.putTraverseDirectory(clientLogsDir, clientLogsDir.getName());
      } else {
        for (File element : serverLogsDir) {
          if (element != null) zip.putTraverseDirectory(element, element.getName());
        }
      }
      zip.finish();
    } catch (IOException e) {
      System.out.println("Unexpected error - unable to write Terracotta archive: " + archiveFile);
      e.printStackTrace();
      System.exit(1);
    }
    System.out.println("\n\nWrote archive to:" + archiveFile);
  }
}
