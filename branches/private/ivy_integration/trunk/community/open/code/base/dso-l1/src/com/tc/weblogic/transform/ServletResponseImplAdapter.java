/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.weblogic.transform;

import com.sleepycat.asm.Opcodes;
import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;

public class ServletResponseImplAdapter extends ClassAdapter implements Opcodes {

  public ServletResponseImplAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    access &= (~ACC_FINAL); // class is final in weblogic 8.1 less than SP6
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("<init>".equals(name)) {
      access |= ACC_PROTECTED;
    }

    access &= ~ACC_FINAL; // make it possible to delegate all methods in our psuedo-wrapper

    return super.visitMethod(access, name, desc, signature, exceptions);
  }
}
