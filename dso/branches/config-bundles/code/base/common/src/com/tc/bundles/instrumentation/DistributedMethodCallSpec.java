/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.bundles.instrumentation;

public class DistributedMethodCallSpec {
  private final String returnType;
  private final String methodName;
  private final String[] methodParameterTypes;
  
  public DistributedMethodCallSpec(String returnType, String methodName, String[] methodParameterTypes) {
    this.returnType = returnType;
    this.methodName = methodName;
    this.methodParameterTypes = methodParameterTypes;
  }

  public String getMethodName() {
    return methodName;
  }

  public String[] getMethodParameterTypes() {
    return methodParameterTypes;
  }

  public String getReturnType() {
    return returnType;
  }
}
