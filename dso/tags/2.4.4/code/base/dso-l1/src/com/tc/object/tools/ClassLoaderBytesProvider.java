/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.tools;

import com.tc.object.bytecode.ByteCodeUtil;

public class ClassLoaderBytesProvider implements ClassBytesProvider {

  private final ClassLoader source;

  public ClassLoaderBytesProvider(ClassLoader source) {
    this.source = source;
  }

  public byte[] getBytesForClass(String className) throws ClassNotFoundException {
    return ByteCodeUtil.getBytesForClass(className, source);
  }
}