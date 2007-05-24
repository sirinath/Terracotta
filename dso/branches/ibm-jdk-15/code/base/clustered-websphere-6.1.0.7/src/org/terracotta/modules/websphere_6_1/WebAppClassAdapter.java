package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class WebAppClassAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public WebAppClassAdapter() {
    super(null);
  }

  public WebAppClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new WebAppClassAdapter(visitor);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.WEBAPPCONFIG_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("createSessionContext".equals(name) && "(Lcom/ibm/ws/container/DeployedModule;)V".equals(desc)) {
      mv = new CreateSessionContextMethodAdapter(mv);
    }
    return mv;
  }

  private static class CreateSessionContextMethodAdapter extends MethodAdapter implements Opcodes {

    public CreateSessionContextMethodAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitInsn(final int opcode) {
      if (opcode == RETURN) {
        super.visitVarInsn(ALOAD, 0);
        super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/webapp/WebApp",
                              ByteCodeUtil.TC_METHOD_PREFIX + "initTerracottaSessionsWebAppConfig", "()V");
      }
      super.visitInsn(opcode);
    }

  }

  public void visitEnd() {
    addInitTerracottaSessionsWebAppConfig();
    addTerracottaSessionsWebAppConfigField();
    addSessionAttributeListeners();
    addSessionListener();
    addSessionCookieComment();
    addSessionCookieDomain();
    addSessionCookieMaxAge();
    addSessionCookieName();
    addSessionCookiePath();
    addSessionCookieSecure();
    addSessionCookieEnabled();
    addSessionIdLength();
    addSessionTimeoutSeconds();
    addServerId();
    addSessionTrackingEnabled();
    addSessionUrlRewritingEnabled();
    super.visitEnd();
  }

  private void addInitTerracottaSessionsWebAppConfig() {
    MethodVisitor mv = super.visitMethod(ACC_PRIVATE, "__tc_initTerracottaSessionsWebAppConfig", "()V", null,
                                         new String[] { "java/lang/Exception" });
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "sessionCtx",
                      "Lcom/ibm/ws/webcontainer/session/IHttpSessionContext;");
    mv.visitTypeInsn(INSTANCEOF, "com/terracotta/session/WebAppConfig");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "sessionCtx",
                      "Lcom/ibm/ws/webcontainer/session/IHttpSessionContext;");
    mv.visitTypeInsn(CHECKCAST, "com/terracotta/session/WebAppConfig");
    mv.visitFieldInsn(PUTFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l1);
    mv.visitTypeInsn(NEW, "java/lang/Exception");
    mv.visitInsn(DUP);
    mv.visitTypeInsn(NEW, "java/lang/StringBuffer");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("Session context does not extend com.ibm.ws.webcontainer.httpsession.SessionContext: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuffer", "<init>", "(Ljava/lang/String;)V");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "sessionCtx",
                      "Lcom/ibm/ws/webcontainer/session/IHttpSessionContext;");
    Label l5 = new Label();
    mv.visitJumpInsn(IFNULL, l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "sessionCtx",
                      "Lcom/ibm/ws/webcontainer/session/IHttpSessionContext;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getName", "()Ljava/lang/String;");
    Label l6 = new Label();
    mv.visitJumpInsn(GOTO, l6);
    mv.visitLabel(l5);
    mv.visitLdcInsn("<<null context>>");
    mv.visitLabel(l6);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "append",
                       "(Ljava/lang/String;)Ljava/lang/StringBuffer;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Exception", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    mv.visitLabel(l3);
    mv.visitInsn(RETURN);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l8, 0);
    mv.visitMaxs(5, 1);
    mv.visitEnd();
  }

  private void addTerracottaSessionsWebAppConfigField() {
    FieldVisitor fv = super.visitField(ACC_PRIVATE, "__tc_terracottaSessionsWebAppConfig",
                                       "Lcom/terracotta/session/WebAppConfig;", null, null);
    fv.visitEnd();
  }

  private void addSessionAttributeListeners() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                                         "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig",
                       "__tc_session_getHttpSessionAttributeListeners",
                       "()[Ljavax/servlet/http/HttpSessionAttributeListener;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionListener() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                                         "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getHttpSessionListener",
                       "()[Ljavax/servlet/http/HttpSessionListener;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieComment() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieComment", "()Ljava/lang/String;", null,
                                         null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookieComment",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieDomain() {
    MethodVisitor mv = super
        .visitMethod(ACC_PUBLIC, "__tc_session_getCookieDomain", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookieDomain",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieMaxAge() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieMaxAgeSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookieMaxAgeSecs",
                       "()I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieName() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookieName",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookiePath() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiePath", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookiePath",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieSecure() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieSecure", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookieSecure", "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiesEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getCookiesEnabled", "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionIdLength() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getIdLength", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getIdLength", "()I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionTimeoutSeconds() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getSessionTimeoutSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getSessionTimeoutSecs",
                       "()I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addServerId() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getServerId", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getServerName", "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionTrackingEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getTrackingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv
        .visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getTrackingEnabled",
                         "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionUrlRewritingEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getURLRewritingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/webapp/WebApp", "__tc_terracottaSessionsWebAppConfig",
                      "Lcom/terracotta/session/WebAppConfig;");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/terracotta/session/WebAppConfig", "__tc_session_getURLRewritingEnabled",
                       "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/webapp/WebApp;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

}
