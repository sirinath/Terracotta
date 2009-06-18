/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

public interface TracingManagerMBean {

  void startTracingMethod(String clazz, String method) throws Exception;
  
  void stopTracingMethod(String clazz, String method) throws Exception;
}
