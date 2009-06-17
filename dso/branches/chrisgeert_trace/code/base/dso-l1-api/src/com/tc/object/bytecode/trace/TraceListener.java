/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

public interface TraceListener {

  void methodEnter(String clazz, String method);
  
  void methodExit(String clazz, String method);
}
