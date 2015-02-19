/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.object.bytecode.Manager;

import java.lang.instrument.ClassFileTransformer;
import java.net.URL;

/**
 * The idea behind DSOContext is to encapsulate a DSO "world" in a client VM. But this idea has not been fully realized.
 */
public interface DSOContext extends ClassProcessor, ClassFileTransformer {

  public static final String CLASS = "com/tc/object/bytecode/hook/DSOContext";
  public static final String TYPE  = "L" + CLASS + ";";

  /**
   * @return The Manager instance
   */
  public Manager getManager();

  public void shutdown();

  /**
   * Get url to class file
   * 
   * @param className Class name
   * @param loader the calling classloader
   * @param hideSystemResources true if resources destined only for the system class loader should be hidden
   * @return URL to class itself
   */
  public URL getClassResource(String className, ClassLoader loader, boolean hideSystemResources);

  public void addModules(URL[] modules) throws Exception;
}
