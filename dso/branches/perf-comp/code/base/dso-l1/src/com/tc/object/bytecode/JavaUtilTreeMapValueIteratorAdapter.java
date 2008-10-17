/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class JavaUtilTreeMapValueIteratorAdapter extends ClassAdapter {

  public JavaUtilTreeMapValueIteratorAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    return new ValueFieldAdapter(mv);
  }

  private static class ValueFieldAdapter extends MethodAdapter implements Opcodes {

    public ValueFieldAdapter(com.tc.asm.MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (GETFIELD == opcode && "java/util/TreeMap$Entry".equals(owner) && "value".equals(name)) {
        super.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getValue", "()Ljava/lang/Object;");
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }
  }

}
