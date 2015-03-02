/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.config;

import org.osgi.framework.Bundle;

import com.tc.object.bytecode.ClassAdapterFactory;

import java.net.URL;

public interface StandardDSOClientConfigHelper {

  void enableCapability(TimCapability cap);

  // HACK: available only in StandardDSOClientConfigHelper

  void addAspectModule(String classNamePrefix, String moduleName);

  // HACK: duplicated from DSOApplicationConfig

  void addRoot(String rootName, String rootFieldName);

  void addIncludePattern(String classPattern);

  void addWriteAutolock(String methodPattern);

  void addReadAutolock(String methodPattern);

  void addIncludePattern(String classname, boolean honorTransient);

  void addAutoLockExcludePattern(String expression);

  void addPermanentExcludePattern(String pattern);

  void addNonportablePattern(String pattern);

  LockDefinition createLockDefinition(String name, ConfigLockLevel level);

  void addLock(String methodPattern, LockDefinition lockDefinition);

  // HACK: duplicated from DSOClientConfigHelper

  TransparencyClassSpec getOrCreateSpec(String className);

  TransparencyClassSpec getOrCreateSpec(String className, String applicator);

  void addCustomAdapter(String name, ClassAdapterFactory adapterFactory);

  void addClassReplacement(final String originalClassName, final String replacementClassName,
                           final URL replacementResource);

  void addClassReplacement(String originalClassName, String replacementClassName, URL url, ClassReplacementTest test);

  void addClassResource(final String className, final URL resource, final boolean targetSystemLoaderOnly);

  void addIncludePattern(String expression, boolean honorTransient, boolean oldStyleCallConstructorOnLoad,
                         boolean honorVolatile);

  void addIncludePattern(String expression, boolean honorTransient, String methodToCallOnLoad, boolean honorVolatile);

  void addAutolock(String methodPattern, ConfigLockLevel type);

  void addExcludePattern(String string);

  URL getBundleURL(Bundle bundle);
}
