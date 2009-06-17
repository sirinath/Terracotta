/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

public class LoggingTraceListener implements TraceListener {

  public void methodEnter(String clazz, String method) {
    System.err.println(Thread.currentThread().getName() + " entering method " + clazz + "." + method);
  }

  public void methodExit(String clazz, String method) {
    System.err.println(Thread.currentThread().getName() + " exiting method " + clazz + "." + method);
  }

}
