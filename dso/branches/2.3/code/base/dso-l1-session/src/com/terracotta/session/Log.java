/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.terracotta.session;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {
 private static final SimpleDateFormat DF = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss,S");

  public static void log(String msg) {
    StringBuffer buf = new StringBuffer(DF.format(new Date()));
    buf.append(" [");
    buf.append(Thread.currentThread().getName());
    buf.append("] ").append(msg);
    System.err.println(buf.toString());
  }

}
