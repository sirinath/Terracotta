/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class ArrayListAdapter {

  private static final String FAST_REMOVE_RENAMED     = ByteCodeUtil.TC_METHOD_PREFIX + "fastRemove";
  private static final String FAST_REMOVE_RENAMED_SIG = "(IZ)V";

  public static class FastRemoveMethodCreator implements MethodCreator, Opcodes {

    @Override
    public void createMethods(ClassVisitor cv) {
      MethodVisitor mv = cv.visitMethod(ACC_PRIVATE | ACC_SYNTHETIC, FAST_REMOVE_RENAMED, FAST_REMOVE_RENAMED_SIG,
                                        null, null);
      mv.visitCode();
      mv.visitInsn(ACONST_NULL);
      mv.visitVarInsn(ASTORE, 3);
      mv.visitVarInsn(ILOAD, 2);
      Label isSharedCheck1 = new Label();
      mv.visitJumpInsn(IFEQ, isSharedCheck1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/ArrayList", "get", "(I)Ljava/lang/Object;", false);
      mv.visitVarInsn(ASTORE, 3);
      mv.visitLabel(isSharedCheck1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ILOAD, 1);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "fastRemove", "(I)V", false);
      mv.visitVarInsn(ILOAD, 2);
      Label isSharedCheck2 = new Label();
      mv.visitJumpInsn(IFEQ, isSharedCheck2);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitLdcInsn(SerializationUtil.REMOVE_SIGNATURE);
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V", false);
      mv.visitLabel(isSharedCheck2);
      mv.visitInsn(RETURN);
      mv.visitMaxs(0, 0);
      mv.visitEnd();
    }
  }

  public static class RemoveAdaptor extends AbstractMethodAdapter {

    @Override
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private static class Adapter extends MethodVisitor implements Opcodes {

      public Adapter(MethodVisitor mv) {
        super(Opcodes.ASM5, mv);
      }

      @Override
      public void visitCode() {
        super.visitCode();

        ByteCodeUtil.pushThis(this);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z", false);
        super.visitVarInsn(ISTORE, 4);

        super.visitVarInsn(ILOAD, 4);
        Label notShared = new Label();
        super.visitJumpInsn(IFEQ, notShared);
        ByteCodeUtil.pushThis(this);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "checkWriteAccess",
                              "(Ljava/lang/Object;)V", false);
        super.visitLabel(notShared);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {

        if (opcode == INVOKESPECIAL && "fastRemove".equals(name) && "java/util/ArrayList".equals(owner)
            && "(I)V".equals(desc)) {
          name = FAST_REMOVE_RENAMED;
          desc = FAST_REMOVE_RENAMED_SIG;
          super.visitVarInsn(ILOAD, 4);
        }

        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }

    }

  }

}
