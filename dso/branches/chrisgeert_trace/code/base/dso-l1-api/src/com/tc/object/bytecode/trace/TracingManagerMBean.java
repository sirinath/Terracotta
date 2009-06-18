/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.management.TerracottaMBean;

import javax.management.NotificationEmitter;

public interface TracingManagerMBean extends TerracottaMBean, NotificationEmitter {

  void startTracingMethod(String clazz, String method) throws Exception;
  
  void stopTracingMethod(String clazz, String method) throws Exception;
  
  boolean isMethodTracingEnabled();
}
