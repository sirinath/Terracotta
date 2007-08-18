/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.hibernate_3_1_2;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class SessionFactoryClassAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {
  public SessionFactoryClassAdapter() {
    super(null);
  }

  private SessionFactoryClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new SessionFactoryClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if ("close".equals(name) && "()V".equals(desc) && exceptions.length == 1
        && "org/hibernate/HibernateException".equals(exceptions[0])) {
      recreateCloseMethod(access, name, desc, signature, exceptions);
      return null;
    } else {
      return super.visitMethod(access, name, desc, signature, exceptions);
    }
  }

  private void recreateCloseMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = cv.visitMethod(access, name, desc, signature, exceptions);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(727, l0);
    mv.visitFieldInsn(GETSTATIC, "org/hibernate/impl/SessionFactoryImpl", "log", "Lorg/apache/commons/logging/Log;");
    mv.visitLdcInsn("closing");
    mv.visitMethodInsn(INVOKEINTERFACE, "org/apache/commons/logging/Log", "info", "(Ljava/lang/Object;)V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(729, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_1);
    mv.visitFieldInsn(PUTFIELD, "org/hibernate/impl/SessionFactoryImpl", "isClosed", "Z");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(731, l2);
    mv.visitInsn(RETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Lorg/hibernate/impl/SessionFactoryImpl;", null, l0, l3, 0);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }
}
