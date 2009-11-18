/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class CopyOnWriteArrayListAdapter {

  public static class RemoveAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = visitOriginal(cv);
      return new RemoveMethodAdapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class RemoveMethodAdapter extends MethodAdapter implements Opcodes {

    public RemoveMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == PUTFIELD && "array".equals(name)) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitLdcInsn("remove(Ljava/lang/Object;)Z");
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 5);
        mv.visitInsn(AALOAD);
        mv.visitInsn(AASTORE);

        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                           "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  public static class RemoveAllAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor cv) {
      MethodVisitor mv = cv
          .visitMethod(this.access, this.methodName, this.description, this.signature, this.exceptions);
      adaptRemoveAll(mv);
      return null;
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }

    private void adaptRemoveAll(MethodVisitor mv) {
      mv.visitCode();
      Label l0 = new Label();
      mv.visitLabel(l0);

      mv.visitTypeInsn(NEW, "java/util/ArrayList");
      mv.visitInsn(DUP);
      mv.visitMethodInsn(INVOKESPECIAL, "java/util/ArrayList", "<init>", "()V");
      mv.visitVarInsn(ASTORE, 2);
      Label l1 = new Label();
      mv.visitLabel(l1);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      mv.visitVarInsn(ASTORE, 3);
      Label l2 = new Label();
      mv.visitLabel(l2);

      mv.visitVarInsn(ALOAD, 3);
      mv.visitInsn(ARRAYLENGTH);
      mv.visitVarInsn(ISTORE, 4);
      Label l3 = new Label();
      mv.visitLabel(l3);

      mv.visitVarInsn(ILOAD, 4);
      Label l4 = new Label();
      mv.visitJumpInsn(IFNE, l4);
      Label l5 = new Label();
      mv.visitLabel(l5);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l4);

      mv.visitVarInsn(ILOAD, 4);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 5);
      Label l6 = new Label();
      mv.visitLabel(l6);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 6);
      Label l7 = new Label();
      mv.visitLabel(l7);

      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ISTORE, 7);
      Label l8 = new Label();
      mv.visitLabel(l8);
      Label l9 = new Label();
      mv.visitJumpInsn(GOTO, l9);
      Label l10 = new Label();
      mv.visitLabel(l10);

      mv.visitVarInsn(ALOAD, 3);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitInsn(AALOAD);
      mv.visitVarInsn(ASTORE, 8);
      Label l11 = new Label();
      mv.visitLabel(l11);

      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Collection", "contains", "(Ljava/lang/Object;)Z");
      Label l12 = new Label();
      mv.visitJumpInsn(IFNE, l12);
      Label l13 = new Label();
      mv.visitLabel(l13);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitIincInsn(6, 1);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitInsn(AASTORE);
      Label l14 = new Label();
      mv.visitJumpInsn(GOTO, l14);
      mv.visitLabel(l12);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitVarInsn(ALOAD, 8);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "add", "(Ljava/lang/Object;)Z");
      mv.visitInsn(POP);
      mv.visitLabel(l14);

      mv.visitIincInsn(7, 1);
      mv.visitLabel(l9);
      mv.visitVarInsn(ILOAD, 7);
      mv.visitVarInsn(ILOAD, 4);
      mv.visitJumpInsn(IF_ICMPLT, l10);
      Label l15 = new Label();
      mv.visitLabel(l15);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitVarInsn(ILOAD, 4);
      Label l16 = new Label();
      mv.visitJumpInsn(IF_ICMPNE, l16);
      Label l17 = new Label();
      mv.visitLabel(l17);

      mv.visitInsn(ICONST_0);
      mv.visitInsn(IRETURN);
      mv.visitLabel(l16);

      mv.visitVarInsn(ILOAD, 6);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitVarInsn(ASTORE, 7);
      Label l18 = new Label();
      mv.visitLabel(l18);

      mv.visitVarInsn(ALOAD, 5);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ILOAD, 6);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      Label l19 = new Label();
      mv.visitLabel(l19);

      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 7);
      mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
      Label l20 = new Label();
      mv.visitLabel(l20);

      mv.visitVarInsn(ALOAD, 0);
      Label l21 = new Label();
      mv.visitLabel(l21);

      mv.visitLdcInsn("removeAll(Ljava/util/Collection;)Z");
      Label l22 = new Label();
      mv.visitLabel(l22);

      mv.visitVarInsn(ALOAD, 2);
      mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;");
      Label l23 = new Label();
      mv.visitLabel(l23);

      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      Label l24 = new Label();
      mv.visitLabel(l24);

      mv.visitInsn(ICONST_1);
      mv.visitInsn(IRETURN);
      Label l25 = new Label();
      mv.visitLabel(l25);
      mv.visitLocalVariable("this", "Ljava/util/concurrent/CopyOnWriteArrayList;", null, l0, l25, 0);
      mv.visitLocalVariable("c", "Ljava/util/Collection;", null, l0, l25, 1);
      mv.visitLocalVariable("removedObjects", "Ljava/util/List;", null, l1, l25, 2);
      mv.visitLocalVariable("elementData", "[Ljava/lang/Object;", null, l2, l25, 3);
      mv.visitLocalVariable("len", "I", null, l3, l25, 4);
      mv.visitLocalVariable("temp", "[Ljava/lang/Object;", null, l6, l25, 5);
      mv.visitLocalVariable("newlen", "I", null, l7, l25, 6);
      mv.visitLocalVariable("i", "I", null, l8, l15, 7);
      mv.visitLocalVariable("element", "Ljava/lang/Object;", null, l11, l14, 8);
      mv.visitLocalVariable("newArray", "[Ljava/lang/Object;", null, l18, l25, 7);
      mv.visitMaxs(5, 9);
      mv.visitEnd();
    }
  }
}
