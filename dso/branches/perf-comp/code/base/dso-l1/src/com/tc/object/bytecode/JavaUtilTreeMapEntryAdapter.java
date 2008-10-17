/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilTreeMapEntryAdapter extends ClassAdapter {

  public JavaUtilTreeMapEntryAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    access &= ~ Opcodes.ACC_FINAL;
    super.visit(version, access, name, signature, superName, interfaces);
  }



}
