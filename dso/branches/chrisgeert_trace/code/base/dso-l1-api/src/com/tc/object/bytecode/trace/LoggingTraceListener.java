/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.asm.Opcodes;

public class LoggingTraceListener implements TraceListener {

  private final String clazz;
  private final String method;
  
  public LoggingTraceListener(String clazz, String method) {
    this.clazz = clazz;
    this.method = method;
  }
  
  public void methodEnter() {
    System.err.println(Thread.currentThread().getName() + " entering method " + clazz + "." + method);
  }

  public void methodExit(int opcode) {
    System.err.println(Thread.currentThread().getName() + " exiting method " + clazz + "." + method + " [" + opcodeToString(opcode) + "]");
  }
  
  private static String opcodeToString(int opcode) {
    switch (opcode) {
      case Opcodes.IRETURN: return "IRETURN";
      case Opcodes.LRETURN: return "LRETURN";
      case Opcodes.FRETURN: return "FRETURN";
      case Opcodes.DRETURN: return "DRETURN";
      case Opcodes.ARETURN: return "ARETURN";
      case Opcodes.RETURN: return "RETURN";
      case Opcodes.ATHROW: return "ATHROW";
      default: return " unexpected [invalid return bytecode?]";
    }
  }

  
}
