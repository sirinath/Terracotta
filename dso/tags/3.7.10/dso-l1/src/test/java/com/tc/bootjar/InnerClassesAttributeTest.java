/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.bootjar;

import com.tc.test.TCTestCase;

public class InnerClassesAttributeTest extends TCTestCase {

  public void testReflectionOnAddedInnerClass() throws ClassNotFoundException {
    java.util.concurrent.locks.ReentrantReadWriteLock.class.getCanonicalName();
    Class.forName("java.util.concurrent.locks.ReentrantReadWriteLock$DsoLock").getCanonicalName();
  }
}
