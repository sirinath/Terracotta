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

  /**
   * <pre>
   * private void loadDSOFilterInfo() {
   *   if (ClassProcessorHelper.isDSOSessions(webAppConfig.getContextRoot())) {
   *     FilterConfig dsoFilterConfig = new FilterConfig(&quot;TerracottaSessionFilterConfig&quot;);
   *     dsoFilterConfig.setName(&quot;TerracottaSessionFilter&quot;);
   *     dsoFilterConfig.addInitParameter(&quot;app-server&quot;, &quot;IBM-Websphere&quot;);
   *     dsoFilterConfig.setFilterClassName(&quot;com.terracotta.session.SessionFilter&quot;);
   *     webAppConfig.addFilterInfo(dsoFilterConfig);
   *     FilterMapping dsoFilterMapping = new FilterMapping(&quot;/*&quot;, dsoFilterConfig, null);
   *     addFilterMapping(dsoFilterMapping);
   *   }
   * }
   * </pre>
   */
  private void createLoadDSOFilterMethod() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "loadDSOFilterInfo", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot",
                       "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions",
                       "(Ljava/lang/String;)Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("TerracottaSessionFilterConfig");
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ASTORE, 1);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("TerracottaSessionFilter");
    mv
        .visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setName",
                         "(Ljava/lang/String;)V");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("app-server");
    mv.visitLdcInsn("IBM-Websphere");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "addInitParameter",
                       "(Ljava/lang/String;Ljava/lang/String;)V");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "setFilterClassName",
                       "(Ljava/lang/String;)V");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig",
                      "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo",
                       "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterMapping");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("/*");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ACONST_NULL);
    mv
        .visitMethodInsn(
                         INVOKESPECIAL,
                         "com/ibm/ws/webcontainer/filter/FilterMapping",
                         "<init>",
                         "(Ljava/lang/String;Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;Lcom/ibm/wsspi/webcontainer/servlet/IServletConfig;)V");
    mv.visitVarInsn(ASTORE, 2);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "addFilterMapping",
                       "(Lcom/ibm/ws/webcontainer/filter/FilterMapping;)V");
    mv.visitLabel(l1);
    mv.visitInsn(RETURN);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/filter/WebAppFilterManager;", null, l0, l9, 0);
    mv.visitLocalVariable("dsoFilterConfig", "Lcom/ibm/ws/webcontainer/filter/FilterConfig;", null, l3, l1, 1);
    mv.visitLocalVariable("dsoFilterMapping", "Lcom/ibm/ws/webcontainer/filter/FilterMapping;", null, l8, l1, 2);
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
