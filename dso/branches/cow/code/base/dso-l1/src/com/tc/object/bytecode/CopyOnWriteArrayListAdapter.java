/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class CopyOnWriteArrayListAdapter {

  public static class RemoveAdaptor extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      MethodVisitor mv = visitOriginal(classVisitor);
      return new Adapter(mv);
    }

    @Override
    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class Adapter extends MethodAdapter implements Opcodes {

    public Adapter(MethodVisitor mv) {
      super(mv);
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == PUTFIELD && "array".equals(name)) {
        // adding below call before the "array" assignments
        // ManagerUtil.logicalInvoke(this, "remove(Ljava/lang/Object;)Z", new Object[] { array[i] });
        
        // this
        mv.visitVarInsn(ALOAD, 0);
        
        // "remove(Ljava/lang/Object;)Z"
        mv.visitLdcInsn("remove(Ljava/lang/Object;)Z");
        
        // new Object[] { array[i] }
        mv.visitInsn(ICONST_1);
        mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
        mv.visitInsn(DUP);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "java/util/concurrent/CopyOnWriteArrayList", "array", "[Ljava/lang/Object;");
        mv.visitVarInsn(ILOAD, 5);
        mv.visitInsn(AALOAD);
        mv.visitInsn(AASTORE);
        

        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke", "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      }
      super.visitFieldInsn(opcode, owner, name, desc);
    }
  }

}
