/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilConcurrentLocksAQSAdapter extends ClassVisitor implements ClassAdapterFactory, Opcodes {

  public static final String TC_STAGE_CHANGED      = ByteCodeUtil.TC_METHOD_PREFIX + "AQS_stateChanged";
  public static final String TC_STAGE_CHANGED_DESC = "(I)V";

  public JavaUtilConcurrentLocksAQSAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5, cv);
  }

  public JavaUtilConcurrentLocksAQSAdapter() {
    super(Opcodes.ASM5);
  }

  @Override
  public ClassVisitor create(ClassVisitor visitor, ClassLoader loader) {
    return new JavaUtilConcurrentLocksAQSAdapter(visitor);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("setState".equals(name)) {
      mv = new SetStateAdapter(mv);
    } else if ("compareAndSetState".equals(name)) {
      mv = new CompareAndSetStateAdapter(mv);
    }

    return mv;
  }

  @Override
  public void visitEnd() {
    addTCStateChangedMethod();
    super.visitEnd();
  }

  private void addTCStateChangedMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PROTECTED, TC_STAGE_CHANGED, TC_STAGE_CHANGED_DESC, null, null);
    mv.visitCode();
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class CompareAndSetStateAdapter extends MethodVisitor implements Opcodes {

    public CompareAndSetStateAdapter(MethodVisitor mv) {
      super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitInsn(int opcode) {
      if (IRETURN == opcode) {
        mv.visitVarInsn(ISTORE, 3);
        mv.visitVarInsn(ILOAD, 3);
        Label returningFalse = new Label();
        mv.visitJumpInsn(IFEQ, returningFalse);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/AbstractQueuedSynchronizer", TC_STAGE_CHANGED,
                           TC_STAGE_CHANGED_DESC, false);
        mv.visitLabel(returningFalse);
        mv.visitVarInsn(ILOAD, 3);
      }

      super.visitInsn(opcode);

    }

  }

  private static class SetStateAdapter extends MethodVisitor implements Opcodes {

    public SetStateAdapter(MethodVisitor mv) {
      super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/locks/AbstractQueuedSynchronizer", TC_STAGE_CHANGED,
                           TC_STAGE_CHANGED_DESC, false);
      }
      super.visitInsn(opcode);
    }

  }

}
