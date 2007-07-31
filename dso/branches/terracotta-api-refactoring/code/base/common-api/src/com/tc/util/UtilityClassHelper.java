/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public abstract class UtilityClassHelper {

  public static final Object invokeStaticMethod(final String className, final String methodName, final Class[] ptypes,
                                                final Object[] args) {
    try {
      final Class _class = Class.forName(className);
      Method _method = _class.getMethod(methodName, ptypes);
      return _method.invoke(null, args);
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    } catch (IllegalArgumentException e) {
      throw new Error(e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    } catch (InvocationTargetException e) {
      throw new Error(e);
    } catch (SecurityException e) {
      throw new Error(e);
    } catch (NoSuchMethodException e) {
      throw new Error(e);
    }
  }

  public static final Object createInstance(final String className) {
    try {
      final Class _class = Class.forName(className);
      return _class.newInstance();
    } catch (ClassNotFoundException e) {
      throw new Error(e);
    } catch (InstantiationException e) {
      throw new Error(e);
    } catch (IllegalAccessException e) {
      throw new Error(e);
    }
  }

}
