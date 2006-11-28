/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.net;

import com.tc.util.runtime.Os;

import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NIOWorkarounds {

  private NIOWorkarounds() {
    //
  }

  public static boolean windowsWritevWorkaround(IOException ioe) {
    final String err = ioe.getMessage();
    if (null != err) {
      // java.io.IOException can be thrown here, but it should be ignored on windows
      // http://developer.java.sun.com/developer/bugParade/bugs/4854354.html
      if (err.equals("A non-blocking socket operation could not be completed immediately")) {
        if (Os.isWindows()) { return true; }
      }
    }

    return false;
  }

  public static boolean linuxSelectWorkaround(IOException ioe) {
    // workaround bug in Sun VM when select() gets interrupted
    // see sun bug 4504001 (http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4504001)

    if (Os.isLinux()) {
      String msg = ioe.getMessage();
      if ("Interrupted system call".equals(msg)) { return true; }
    }

    return false;
  }

  public static void solaris10Workaround() {
    boolean workaround = solaris10Workaround(System.getProperties());
    if (workaround) {
      String prev = System.getProperty("java.nio.channels.spi.SelectorProvider");
      System.setProperty("java.nio.channels.spi.SelectorProvider", "sun.nio.ch.PollSelectorProvider");

      String msg = "\nWARNING: Terracotta is forcing the use of poll based NIO selector to workaround Sun bug 6322825\n";
      if (prev != null) {
        msg += "This overrides the previous value of " + prev + "\n";
      }
      System.err.println(msg.toString());
    }
  }

  /**
   * see LKC-2436 and http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6322825
   *
   * @return true if the workaround should be applied
   */
  static boolean solaris10Workaround(Properties props) {
    String vendor = props.getProperty("java.vendor", "");
    String osName = props.getProperty("os.name", "");
    String osVersion = props.getProperty("os.version", "");
    String javaVersion = props.getProperty("java.version", "");

    if (vendor.toLowerCase().startsWith("sun") && "SunOS".equals(osName) && "5.10".equals(osVersion)) {
      if (javaVersion.matches("^1\\.[12345]\\..*")) { // bug is fixed in 1.6+ (supposedly)
        // Bug is fixed in 1.5.0_08+ (again, supposedly)
        Pattern p = Pattern.compile("^1\\.5\\.0_(\\d\\d)$");
        Matcher m = p.matcher(javaVersion);
        if (m.matches()) {
          String minorRev = m.group(1);
          int ver = Integer.parseInt(minorRev);
          if (ver >= 8) { return false; }
        }

        return true;
      }
    }
    return false;
  }

  // http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6427854
  public static boolean selectorOpenRace(NullPointerException npe) {
    StackTraceElement source = npe.getStackTrace()[0];
    if (source.getClassName().equals("sun.nio.ch.Util") && source.getMethodName().equals("atBugLevel")) { return true; }
    return false;
  }

  public static void main(String args[]) {
    NIOWorkarounds.solaris10Workaround();
  }

}
