/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.websphere.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WSLauncherAdapter extends ClassAdapter implements ClassAdapterFactory {

  public WSLauncherAdapter(ClassVisitor cv) {
    super(cv);
  }

  public WSLauncherAdapter() {
    super(null);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WSLauncherAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("createExtClassLoader".equals(name)) {
      mv = new CreateExtClassLoaderAdapater(mv);
    }

    return mv;
  }

  private static class CreateExtClassLoaderAdapater extends MethodAdapter implements Opcodes {

    public CreateExtClassLoaderAdapater(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (INVOKESPECIAL == opcode && "com/ibm/ws/bootstrap/ExtClassLoader".equals(owner) && "<init>".equals(name)) {
        super.visitInsn(DUP);
        super.visitMethodInsn(INVOKESTATIC, "com/tc/websphere/WebsphereLoaderNaming",
                              "nameAndRegisterBootstrapExtLoader", "(Lcom/tc/object/loaders/NamedClassLoader;)V");
      }
    }

  }

}
