/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.event;

import com.tc.asm.Type;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.dmi.DmiClassSpec;
import com.tc.object.dmi.DmiDescriptor;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;
import com.tcclient.object.DistributedMethodCall;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

public class DmiManagerImpl implements DmiManager {
  private final ClassProvider       classProvider;
  private final ClientObjectManager objMgr;

  public DmiManagerImpl(ClassProvider classProvider, ClientObjectManager objMgr) {
    Assert.pre(classProvider != null);
    Assert.pre(objMgr != null);
    this.classProvider = classProvider;
    this.objMgr = objMgr;
  }

  public void distributedInvoke(Object receiver, String method, Object[] params) {
    Assert.pre(receiver != null);
    Assert.pre(method != null);
    Assert.pre(params != null);

    final String methodName = method.substring(0, method.indexOf('('));
    final String paramDesc = method.substring(method.indexOf('('));
    final DistributedMethodCall dmc = new DistributedMethodCall(receiver, params, methodName, paramDesc);
    final ObjectID receiverId = objMgr.lookupOrCreate(receiver).getObjectID();
    final ObjectID dmiCallId = objMgr.lookupOrCreate(dmc).getObjectID();
    final DmiClassSpec[] classSpecs = getClassSpecs(classProvider, receiver, params);
    DmiDescriptor dd = new DmiDescriptor(receiverId, dmiCallId, classSpecs);
    dd.getClass();
    // FIXME: add dd to current transaction...
  }

  public void invoke(DmiDescriptor dd) {
    Assert.pre(dd != null);
    try {
      checkClassAvailability(classProvider, dd.getClassSpecs());
    } catch (ClassNotFoundException e1) {
      // FIXME: log
      return;
    }
    try {
      DistributedMethodCall dmc = (DistributedMethodCall) objMgr.lookupObject(dd.getDmiCallId());
      invoke(dmc);
    } catch (Throwable e) {
      // drop this call
      // FIXME: log error
    }
  }

  private static void invoke(DistributedMethodCall dmc) throws IllegalArgumentException, IllegalAccessException,
      InvocationTargetException {
    final ClassLoader origContextLoader = Thread.currentThread().getContextClassLoader();
    Method m = getMethod(dmc);
    m.setAccessible(true);
    try {
      Thread.currentThread().setContextClassLoader(dmc.getReceiver().getClass().getClassLoader());
      m.invoke(dmc.getReceiver(), dmc.getParameters());
    } finally {
      Thread.currentThread().setContextClassLoader(origContextLoader);
    }
  }

  private static Method getMethod(DistributedMethodCall dmc) {
    String methodName = dmc.getMethodName();
    String paramDesc = dmc.getParameterDesc();

    Class c = dmc.getReceiver().getClass();

    while (c != null) {
      Method[] methods = c.getDeclaredMethods();
      for (int i = 0; i < methods.length; i++) {
        Method m = methods[i];
        if (!m.getName().equals(methodName)) {
          continue;
        }
        Class[] argTypes = m.getParameterTypes();
        StringBuffer signature = new StringBuffer("(");
        for (int j = 0; j < argTypes.length; j++) {
          signature.append(Type.getDescriptor(argTypes[j]));
        }
        signature.append(")");
        signature.append(Type.getDescriptor(m.getReturnType()));
        if (signature.toString().equals(paramDesc)) { return m; }
      }

      c = c.getSuperclass();
    }
    throw new RuntimeException("Method " + methodName + paramDesc + " does not exist on this object: "
                               + dmc.getReceiver());
  }

  private static void checkClassAvailability(ClassProvider classProvider, DmiClassSpec[] classSpecs)
      throws ClassNotFoundException {
    Assert.pre(classSpecs != null);
    for (int i = 0; i < classSpecs.length; i++) {
      DmiClassSpec s = classSpecs[i];
      classProvider.getClassFor(s.getClassName(), s.getClassLoaderDesc());
    }
  }

  private static DmiClassSpec[] getClassSpecs(ClassProvider classProvider, Object receiver, Object[] params) {
    Assert.pre(classProvider != null);
    Assert.pre(receiver != null);
    Assert.pre(params != null);

    Set set = new HashSet();
    set.add(getClassSpec(classProvider, receiver));
    for (int i = 0; i < params.length; i++) {
      final Object p = params[i];
      if (p != null) set.add(getClassSpec(classProvider, p));
    }
    DmiClassSpec[] rv = new DmiClassSpec[set.size()];
    set.toArray(rv);
    return rv;
  }

  private static Object getClassSpec(ClassProvider classProvider, Object obj) {
    Assert.pre(classProvider != null);
    Assert.pre(obj != null);
    final String classLoader = classProvider.getLoaderDescriptionFor(obj.getClass());
    final String className = obj.getClass().getName();
    return new DmiClassSpec(classLoader, className);
  }
}
