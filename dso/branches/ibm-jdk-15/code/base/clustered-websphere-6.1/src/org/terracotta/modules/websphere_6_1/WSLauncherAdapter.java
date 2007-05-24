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

public class WSLauncherAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

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
      mv = new CreateExtClassLoaderMethodAdapter(mv);
    }

    return mv;
  }

  public void visitEnd() {
    addRegisterExtClassLoader();
    super.visitEnd();
  }

  /**
   * <pre>
   * private static void __tc_registerExtClassLoader(ExtClassLoader extclassloader) {
   *   extclassloader.__tc_setClassLoaderName(Namespace.createLoaderName(Namespace.WEBSPHERE_NAMESPACE, &quot;extensions&quot;));
   *   ClassProcessorHelper.registerGlobalLoader(extclassloader);
   * }
   * </pre>
   */
  private void addRegisterExtClassLoader() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE + ACC_STATIC, ByteCodeUtil.TC_METHOD_PREFIX
                                                                   + "registerExtClassLoader",
                                         "(Lcom/ibm/ws/bootstrap/ExtClassLoader;)V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitLdcInsn("Websphere.");
    mv.visitLdcInsn("extensions");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                       "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/bootstrap/ExtClassLoader", "__tc_setClassLoaderName",
                       "(Ljava/lang/String;)V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "registerGlobalLoader",
                       "(Lcom/tc/object/loaders/NamedClassLoader;)V");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitInsn(RETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("extclassloader", "Lcom/ibm/ws/bootstrap/ExtClassLoader;", null, l0, l3, 0);
    mv.visitMaxs(3, 1);
    mv.visitEnd();
  }

  private static class CreateExtClassLoaderMethodAdapter extends MethodAdapter implements Opcodes {

    public CreateExtClassLoaderMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    /**
     * <pre>
     * __tc_registerExtClassLoader(extclassloader);
     * </pre>
     */
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if (opcode == INVOKESPECIAL && "com/ibm/ws/bootstrap/ExtClassLoader".equals(owner) && "<init>".equals(name)) {
        super.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESTATIC, "com/ibm/wsspi/bootstrap/WSLauncher", ByteCodeUtil.TC_METHOD_PREFIX
                                                                               + "registerExtClassLoader",
                           "(Lcom/ibm/ws/bootstrap/ExtClassLoader;)V");
      }
    }

  }

}
