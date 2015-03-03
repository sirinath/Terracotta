/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

public class BufferedWriterAdapter extends ClassVisitor implements Opcodes {

  public BufferedWriterAdapter(ClassVisitor cv) {
    super(Opcodes.ASM5, cv);
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("write".equals(name) && "(Ljava/lang/String;II)V".equals(desc)) { return new WriteStringAdatper(mv); }
    return mv;
  }

  private static class WriteStringAdatper extends MethodVisitor {

    public WriteStringAdatper(MethodVisitor mv) {
      super(Opcodes.ASM5, mv);
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      if ((INVOKEVIRTUAL == opcode) && ("java/lang/String".equals(owner) && "getChars".equals(name))) {
        super.visitMethodInsn(opcode, owner, "getCharsFast", desc, itf);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc, itf);
      }
    }

  }

}
