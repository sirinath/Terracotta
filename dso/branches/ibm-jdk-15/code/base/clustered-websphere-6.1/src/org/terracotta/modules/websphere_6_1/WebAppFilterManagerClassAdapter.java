package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppFilterManagerClassAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {
  public WebAppFilterManagerClassAdapter() {
    super(null);
  }

  public WebAppFilterManagerClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppFilterManagerClassAdapter(visitor);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (name.equals("init") && desc.equals("()V")) {
      mv = new InitMethodAdapter(mv);
    }
    return mv;
  }

  public void visitEnd() {
    createLoadDSOFilterMethod();
    super.visitEnd();
  }

//  private void loadDSOFilterInfo() {
//    if (ClassProcessorHelper.isDSOSessions(webAppConfig.getContextRoot())) {
//      FilterConfig dsoFilterConfig = new FilterConfig("TerracottaSessionFilterConfig");
//      dsoFilterConfig.setName("TerracottaSessionFilter");
//      dsoFilterConfig.addInitParameter("app-server", "IBM-Websphere");
//      try {
//        SessionsHelper.injectClasses(webAppConfig.getWebApp().getClassLoader());
//      } catch (Exception e) {
//        throw new RuntimeException("Unable to inject Terracotta session filter classes into application "
//            + webAppConfig.getApplicationName(), e);
//      }
//      dsoFilterConfig.setFilterClassName("com.terracotta.session.SessionFilter");
//      webAppConfig.addFilterInfo(dsoFilterConfig);
//      FilterMapping dsoFilterMapping = new FilterMapping("/*", dsoFilterConfig, null);
//      addFilterMapping(dsoFilterMapping);
//    }
//  }

  private void createLoadDSOFilterMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "loadDSOFilterInfo", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    Label l1 = new Label();
    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/Exception");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions", "(Ljava/lang/String;)Z");
    Label l3 = new Label();
    mv.visitJumpInsn(IFEQ, l3);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("TerracottaSessionFilterConfig");
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ASTORE, 1);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("TerracottaSessionFilter");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setName", "(Ljava/lang/String;)V");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("app-server");
    mv.visitLdcInsn("IBM-Websphere");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "addInitParameter", "(Ljava/lang/String;Ljava/lang/String;)V");
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getWebApp", "()Lcom/ibm/ws/webcontainer/webapp/WebApp;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getClassLoader", "()Ljava/lang/ClassLoader;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses", "(Ljava/lang/ClassLoader;)V");
    Label l7 = new Label();
    mv.visitJumpInsn(GOTO, l7);
    mv.visitLabel(l1);
    mv.visitVarInsn(ASTORE, 2);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Unable to inject Terracotta session filter classes into application ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getApplicationName", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
    mv.visitVarInsn(ALOAD, 2);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassName", "(Ljava/lang/String;)V");
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo", "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterMapping");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("/*");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ACONST_NULL);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterMapping", "<init>", "(Ljava/lang/String;Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;Lcom/ibm/wsspi/webcontainer/servlet/IServletConfig;)V");
    mv.visitVarInsn(ASTORE, 2);
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "addFilterMapping", "(Lcom/ibm/ws/webcontainer/filter/FilterMapping;)V");
    mv.visitLabel(l3);
    mv.visitInsn(RETURN);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/filter/WebAppFilterManager;", null, l2, l14, 0);
    mv.visitLocalVariable("dsoFilterConfig", "Lcom/ibm/ws/webcontainer/filter/FilterConfig;", null, l5, l3, 1);
    mv.visitLocalVariable("e", "Ljava/lang/Exception;", null, l8, l7, 2);
    mv.visitLocalVariable("dsoFilterMapping", "Lcom/ibm/ws/webcontainer/filter/FilterMapping;", null, l13, l3, 2);
    mv.visitMaxs(5, 3);
    mv.visitEnd();
  }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();
      super.visitVarInsn(ALOAD, 0);
      super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "loadDSOFilterInfo",
                            "()V");
    }
  }

}
