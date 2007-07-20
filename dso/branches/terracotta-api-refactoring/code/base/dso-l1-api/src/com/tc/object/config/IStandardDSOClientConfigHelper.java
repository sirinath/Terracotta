/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.config;

public interface IStandardDSOClientConfigHelper {

  ITransparencyClassSpec getOrCreateSpec(String className);

  ITransparencyClassSpec getOrCreateSpec(String className, String applicator);
  
}
