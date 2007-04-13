/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.object.TCObject;

import java.lang.reflect.AccessibleObject;

public class AccessibleObjectAdapter extends ClassAdapter implements Opcodes {

  public AccessibleObjectAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("setAccessible0".equals(name) && "(Ljava/lang/reflect/AccessibleObject;Z)V".equals(desc)) {
      return new AccessibleSetAccessibleMethodVisitor(mv);
    }

    return mv;
  }
  
  private static class AccessibleSetAccessibleMethodVisitor extends MethodAdapter {

    private AccessibleSetAccessibleMethodVisitor(MethodVisitor mv) {
      super(mv);
    }    
    
    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(ManagerUtil.class), "lookupExistingOrNull", "(Ljava/lang/Object;)Lcom/tc/object/TCObject;");
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 2);
        Label label_tcobject_null = new Label();
        mv.visitJumpInsn(IFNULL, label_tcobject_null);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Object.class), "getClass", "()Ljava/lang/Class;");
        mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(Class.class), "getName", "()Ljava/lang/String;");
        mv.visitLdcInsn(AccessibleObject.class.getName()+".override");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, Type.getInternalName(AccessibleObject.class), "override", "Z");
        mv.visitInsn(ICONST_M1);
        mv.visitMethodInsn(INVOKEINTERFACE, Type.getInternalName(TCObject.class), "booleanFieldChanged", "(Ljava/lang/String;Ljava/lang/String;ZI)V");
        mv.visitLabel(label_tcobject_null);
      }
      
      mv.visitInsn(opcode);
    }
  }
}