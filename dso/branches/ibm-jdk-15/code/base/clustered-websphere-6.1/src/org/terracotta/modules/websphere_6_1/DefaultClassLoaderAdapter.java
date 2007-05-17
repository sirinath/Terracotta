/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.LocalVariablesSorter;
import com.tc.object.bytecode.ClassAdapterFactory;

public class DefaultClassLoaderAdapter extends ClassAdapter implements ClassAdapterFactory {

  public DefaultClassLoaderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public DefaultClassLoaderAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new DefaultClassLoaderAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("<init>".equals(name)) {
      mv = new CstrAdapter(access, desc, mv);
    }

    return mv;
  }

  private static class CstrAdapter extends LocalVariablesSorter implements Opcodes {

    public CstrAdapter(int access, String desc, MethodVisitor mv) {
      super(access, desc, mv);
    }

    public void visitInsn(int opcode) {
      if (RETURN == opcode) {
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/websphere/WebsphereLoaderNaming", "nameAndRegisterOsgiLoader",
                           "(Lcom/tc/object/loaders/NamedClassLoader;)V");
      }
      super.visitInsn(opcode);
    }

  }

}
