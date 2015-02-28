/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.ibm;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class SystemInitializationAdapter extends ClassVisitor implements ClassAdapterFactory {

  public SystemInitializationAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5, cv);
  }

  public SystemInitializationAdapter() {
    super(Opcodes.ASM5);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("lastChanceHook".equals(name)) { return new LastChanceHookAdapter(mv); }

    return mv;

  }

  private static class LastChanceHookAdapter extends MethodVisitor implements Opcodes {

    public LastChanceHookAdapter(MethodVisitor mv) {
      super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      super.visitMethodInsn(opcode, owner, name, desc, itf);

      // The important bit with this particular location is that it happens
      // before the jmx remote agent thread is started
      if ((opcode == INVOKESTATIC) && "getProperties".equals(name) && "java/lang/System".equals(owner)) {
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "systemLoaderInitialized", "()V", false);
      }
    }
  }

  @Override
  public ClassVisitor create(ClassVisitor visitor, ClassLoader loader) {
    return new SystemInitializationAdapter(visitor);
  }

}
