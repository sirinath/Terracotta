/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.object.bytecode.hook.ClassPostProcessor;
import com.tc.object.bytecode.hook.ClassPreProcessor;
import com.tc.object.bytecode.hook.DSOContext;

import java.util.Map;
import java.util.WeakHashMap;

public class ContextHelper {

  private volatile static DSOContext globalContext;

  // This map should only hold a weak reference to the loader (key).
  // If we didn't we'd prevent loaders from being GC'd
  private static final Map           contextMap                = new WeakHashMap();

  public static final boolean        USE_GLOBAL_CONTEXT;

  private static final String        TC_DSO_GLOBALMODE_SYSPROP = "tc.dso.globalmode";

  private static final boolean       GLOBAL_MODE_DEFAULT       = true;

  static {
    String global = System.getProperty(TC_DSO_GLOBALMODE_SYSPROP, null);
    if (global != null) {
      USE_GLOBAL_CONTEXT = Boolean.valueOf(global).booleanValue();
    } else {
      USE_GLOBAL_CONTEXT = GLOBAL_MODE_DEFAULT;
    }
  }

  public synchronized static void setGlobalContext(DSOContext context) {
    if (!USE_GLOBAL_CONTEXT) { throw new AssertionError("Not global mode"); }

    if (globalContext != null) { throw new AssertionError("Global context already set"); }

    globalContext = context;
  }

  /**
   * WARNING: used by test framework only
   */
  public static Manager getManager(ClassLoader caller) {
    if (ContextHelper.USE_GLOBAL_CONTEXT) { return globalContext.getManager(); }

    DSOContext context;
    synchronized (contextMap) {
      context = (DSOContext) contextMap.get(caller);
    }
    if (context == null) { return null; }
    return context.getManager();
  }

  /**
   * Get the DSOContext for this classloader
   * 
   * @param cl Loader
   * @return Context
   */
  public static DSOContext getContext(ClassLoader cl) {
    if (ContextHelper.USE_GLOBAL_CONTEXT) return globalContext;

    synchronized (contextMap) {
      return (DSOContext) contextMap.get(cl);
    }
  }

  /**
   * @return Global Manager
   */
  public static Manager getGlobalManager() {
    return globalContext.getManager();
  }

  public static ClassPreProcessor getPreProcessor(ClassLoader caller) {
    if (ContextHelper.USE_GLOBAL_CONTEXT) { return globalContext; }
    synchronized (contextMap) {
      return (ClassPreProcessor) contextMap.get(caller);
    }
  }

  public static ClassPostProcessor getPostProcessor(ClassLoader caller) {
    if (ContextHelper.USE_GLOBAL_CONTEXT) { return globalContext; }

    synchronized (contextMap) {
      return (ClassPostProcessor) contextMap.get(caller);
    }
  }

  public static void setContext(ClassLoader loader, DSOContext context) {
    if (ContextHelper.USE_GLOBAL_CONTEXT) { throw new IllegalStateException("DSO Context is global in this VM"); }

    if ((loader == null) || (context == null)) {
      // bad dog
      throw new IllegalArgumentException("Loader and/or context may not be null");
    }

    synchronized (contextMap) {
      contextMap.put(loader, context);
    }
  }

}
