/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

public interface TraceListener {

  void methodEnter(Object self);
  
  void methodExit(Object self, int opcode);
}
