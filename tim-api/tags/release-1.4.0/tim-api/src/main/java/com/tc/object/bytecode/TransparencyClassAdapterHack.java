/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.MethodVisitor;

public interface TransparencyClassAdapterHack {

  MethodVisitor basicVisitMethodHack(int access, String name, String desc, String signature, String[] exceptions);

}
