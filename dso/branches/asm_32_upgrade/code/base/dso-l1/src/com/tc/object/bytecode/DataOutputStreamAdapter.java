/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodAdapter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.lang.reflect.Modifier;

public class DataOutputStreamAdapter extends ClassAdapter implements Opcodes {

  public DataOutputStreamAdapter(ClassVisitor cv) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (Modifier.isStatic(access) && "writeUTF".equals(name)
        && "(Ljava/lang/String;Ljava/io/DataOutput;)I".equals(desc)) { return new WriteUTFAdatper(mv); }
    return mv;
  }

  private static class WriteUTFAdatper extends MethodAdapter {

    public WriteUTFAdatper(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((INVOKEVIRTUAL == opcode) && ("java/lang/String".equals(owner) && "getChars".equals(name))) {
        super.visitMethodInsn(opcode, owner, "getCharsFast", desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

}
