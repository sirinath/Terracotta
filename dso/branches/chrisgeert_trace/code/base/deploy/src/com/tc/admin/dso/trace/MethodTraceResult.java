/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.admin.dso.trace;

public class MethodTraceResult {

  private final String methodSignature;

  private final long executionCount;
  private final long executionTime;
  private final long normalCount;
  private final long exceptionCount;
  
  public MethodTraceResult(String method, long executionCount, long executionTime, long normalCount, long exceptionCount) {
    this.methodSignature = method;
    this.executionCount = executionCount;
    this.executionTime = executionTime;
    this.normalCount = normalCount;
    this.exceptionCount = exceptionCount;
  }
  
  public long getExecutionCount() {
    return executionCount;
  }

  public long getTotalExecutionTime() {
    return executionTime;
  }

  public long getNormalReturnCount() {
    return normalCount;
  }
  
  public long getExceptionalReturnCount() {
    return exceptionCount;
  }

  public String getMethodSignature() {
    return methodSignature;
  }
  
  public String toString() {
    return getMethodSignature() + " : " + getExecutionCount() + ", " + getTotalExecutionTime() + ", " + getNormalReturnCount() + ", " + getExceptionalReturnCount();
  }
}
