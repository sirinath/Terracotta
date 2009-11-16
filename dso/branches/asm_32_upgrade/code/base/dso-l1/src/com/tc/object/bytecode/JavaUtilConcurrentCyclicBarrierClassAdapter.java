/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class JavaUtilConcurrentCyclicBarrierClassAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilConcurrentCyclicBarrierClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("nextGeneration".equals(name) && "()V".equals(desc)) {
      mv = new NextGenerationMethodAdapter(mv);
    }

    return mv;
  }

  private static class NextGenerationMethodAdapter extends MethodAdapter implements Opcodes {
    public NextGenerationMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc)
    {
      if ((!"java/util/concurrent/locks/Condition".equals(owner))
          || (!"signalAll".equals(name)) || (!desc.equals("()V"))) {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        super.visitMethodInsn(INVOKEINTERFACE, "java/util/concurrent/locks/Condition", "signalAll", "()V");
      }
      super.visitInsn(opcode);
    }
  }
}
