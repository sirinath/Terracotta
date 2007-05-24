/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class DefaultClassLoaderAdapter extends ClassAdapter implements ClassAdapterFactory, Opcodes {

  public DefaultClassLoaderAdapter() {
    super(null);
  }

  public DefaultClassLoaderAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new DefaultClassLoaderAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("initialize".equals(name)) {
      mv = new InitializeMethodAdapter(mv);
    }

    return mv;
  }

  public void visitEnd() {
    addRegisterThisAsNamedClassloader();
    super.visitEnd();
  }

  /**
   * Java code for this method:
   * 
   * <pre>
   * private void __tc_registerThisAsNamedClassloader() {
   *   BaseData data = manager.getBaseData();
   *   __tc_setClassLoaderName(Namespace.createLoaderName(Namespace.WEBSPHERE_NAMESPACE, data.getSymbolicName() + &quot;-&quot;
   *                                                                                     + data.getVersion()));
   *   ClassProcessorHelper.registerGlobalLoader(this);
   * }
   * </pre>
   */
  private void addRegisterThisAsNamedClassloader() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, ByteCodeUtil.TC_METHOD_PREFIX + "registerThisAsNamedClassloader",
                                         "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "org/eclipse/osgi/internal/baseadaptor/DefaultClassLoader", "manager",
                      "Lorg/eclipse/osgi/baseadaptor/loader/ClasspathManager;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/osgi/baseadaptor/loader/ClasspathManager", "getBaseData",
                       "()Lorg/eclipse/osgi/baseadaptor/BaseData;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn("Websphere.");
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/osgi/baseadaptor/BaseData", "getSymbolicName",
                       "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    mv.visitLdcInsn("-");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/osgi/baseadaptor/BaseData", "getVersion",
                       "()Lorg/osgi/framework/Version;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/Object;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/osgi/internal/baseadaptor/DefaultClassLoader",
                       "__tc_setClassLoaderName", "(Ljava/lang/String;)V");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "registerGlobalLoader",
                       "(Lcom/tc/object/loaders/NamedClassLoader;)V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitInsn(RETURN);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLocalVariable("this", "Lorg/eclipse/osgi/internal/baseadaptor/DefaultClassLoader;", null, l0, l6, 0);
    mv.visitLocalVariable("data", "Lorg/eclipse/osgi/baseadaptor/BaseData;", null, l1, l6, 1);
    mv.visitMaxs(5, 2);
    mv.visitEnd();
  }

  private static class InitializeMethodAdapter extends MethodAdapter implements Opcodes {

    InitializeMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == Opcodes.RETURN) {
        visitVarInsn(ALOAD, 0);
        visitMethodInsn(INVOKESPECIAL, "org/eclipse/osgi/internal/baseadaptor/DefaultClassLoader",
                        ByteCodeUtil.TC_METHOD_PREFIX + "registerThisAsNamedClassloader", "()V");
      }
      super.visitInsn(opcode);
    }

  }

}
