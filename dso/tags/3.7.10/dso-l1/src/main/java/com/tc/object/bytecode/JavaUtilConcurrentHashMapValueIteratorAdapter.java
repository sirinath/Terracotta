/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentHashMapValueIteratorAdapter extends ClassVisitor implements Opcodes {

  public JavaUtilConcurrentHashMapValueIteratorAdapter(ClassVisitor cv) {
    super(Opcodes.ASM4, cv);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    return new JavaUtilConcurrentHashMapLazyValuesMethodAdapter(access, desc, mv, false);
  }
}