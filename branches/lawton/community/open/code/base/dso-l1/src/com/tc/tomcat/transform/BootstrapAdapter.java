/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.tomcat.transform;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.loaders.NamedClassLoader;

/**
 * All this adapter does is assign names to the three shared loaders in tomcat (common, catalina, and shared). See
 * http://tomcat.apache.org/tomcat-5.0-doc/class-loader-howto.html for more info about these loaders
 */
public class BootstrapAdapter extends ClassAdapter {

  public BootstrapAdapter(ClassVisitor cv, ClassLoader caller) {
    super(cv);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("initClassLoaders".equals(name)) {
      mv = new InitClassLoadersAdatper(mv);
    } else if ("init".equals(name) && "()V".equals(desc)) {
      mv = new InitMethodAdapter(mv);
    }

    return mv;
  }

  private static class InitClassLoadersAdatper extends MethodAdapter implements Opcodes {

    public InitClassLoadersAdatper(MethodVisitor mv) {
      super(mv);
    }

    private void nameAndRegisterLoader(String fieldName, String loaderName) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "org/apache/catalina/startup/Bootstrap", fieldName, "Ljava/lang/ClassLoader;");
      mv.visitTypeInsn(CHECKCAST, NamedClassLoader.CLASS);
      mv.visitInsn(DUP);
      mv.visitFieldInsn(GETSTATIC, "com/tc/object/loaders/Namespace", "TOMCAT_NAMESPACE", "Ljava/lang/String;");
      mv.visitLdcInsn(loaderName);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/loaders/Namespace", "createLoaderName",
                         "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
      mv.visitMethodInsn(INVOKEINTERFACE, NamedClassLoader.CLASS, "__tc_setClassLoaderName", "(Ljava/lang/String;)V");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "registerGlobalLoader",
                         "(" + NamedClassLoader.TYPE + ")V");
    }

    public void visitInsn(int opcode) {
      if (opcode == RETURN) {
        nameAndRegisterLoader("commonLoader", "common");
        nameAndRegisterLoader("catalinaLoader", "catalina");
        nameAndRegisterLoader("sharedLoader", "shared");
      }
      super.visitInsn(opcode);
    }
  }

  static class InitMethodAdapter extends MethodAdapter implements Opcodes {

    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(int opcode) {
      if (opcode == Opcodes.RETURN) {
        mv.visitVarInsn(ALOAD, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;");
        mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses",
                           "(Ljava/lang/ClassLoader;)V");
      }
      super.visitInsn(opcode);
    }

  }

}
