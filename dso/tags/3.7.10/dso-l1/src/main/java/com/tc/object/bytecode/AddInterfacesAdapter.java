/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Opcodes;

public class AddInterfacesAdapter extends ClassVisitor {

  private final String[] toAdd;

  public AddInterfacesAdapter(ClassVisitor cv, String[] toAdd) {
    super(Opcodes.ASM4, cv);
    this.toAdd = toAdd;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, ByteCodeUtil.addInterfaces(interfaces, toAdd));
  }
}