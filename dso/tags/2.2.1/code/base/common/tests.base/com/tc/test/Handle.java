/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 *
 * To be used for Windows only.
 *
 * Get java processes that are holding locks on a given file.
 *
 * This class depends on 2 free programs
 *   - Handle  from http://www.sysinternals.com and
 *   - PrcView from http://www.prcview.com
 *
 * Author: Hung Huynh
 */
package com.tc.test;

import com.tc.util.runtime.Os;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class Handle {

  private static String runProcess(String[] args) throws IOException {
    Process p = Runtime.getRuntime().exec(args);
    BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
    String line;
    StringBuffer buffer = new StringBuffer();
    while ((line = reader.readLine()) != null) {
      buffer.append(line + "\n");
    }
    reader.close();
    return buffer.toString();
  }

  private static String getPathFor(String path, String cmd) {
    String[] dirs = path.split(File.pathSeparator);
    String thePath = "";
    for (int i = 0; i < dirs.length; i++) {
      File test = new File(dirs[i] + File.separator + cmd);
      if (test.exists()) {
        thePath = dirs[i];
        break;
      }
    }
    return thePath;
  }

  /**
   * @param file observed file
   * @param path global path that contains path to handle.exe and pv.exe
   * @return java processes that holding lock on the given file.
   * @throws IOException
   */
  public static String getJavaProcessFileHandles(File file) throws IOException {
    if (!Os.isWindows())
      return "Not a Windows box";

    String searchPath = TestConfigObject.getInstance().executableSearchPath();
    String thePath = getPathFor(searchPath, "handle.exe") + File.separator;

    String[] args = new String[] { thePath + "handle.exe", "-p", "java", file.getAbsolutePath() };
    String handleResult = runProcess(args);

    String processResult = "";
    // if "No matching handles found" from handle.exe, no need to display java processes
    if (handleResult.indexOf("No matching handles found") < 0)
    {
      args = new String[] { thePath + "pv.exe", "-l", "java.exe" };
      processResult = runProcess(args);
    }

    return handleResult + "\n" + processResult;
  }

}
