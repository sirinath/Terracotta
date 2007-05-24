/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.websphere_6_1;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.bytecode.ClassAdapterFactory;

public class SessionContextClassAdapter extends ClassAdapter implements Opcodes, ClassAdapterFactory {

  public SessionContextClassAdapter() {
    super(null);
  }

  public SessionContextClassAdapter(ClassVisitor visitor) {
    super(visitor);
  }

  public ClassAdapter create(ClassVisitor visitor, ClassLoader loader) {
    return new SessionContextClassAdapter(visitor);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { ByteCodeUtil.WEBAPPCONFIG_CLASS });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public void visitEnd() {
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

  private void addSessionAttributeListeners() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionAttributeListeners",
                                         "()[Ljavax/servlet/http/HttpSessionAttributeListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext",
                       "getHttpSessionAttributeListeners", "()Ljava/util/ArrayList;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 1);
    Label l2 = new Label();
    mv.visitJumpInsn(IFNULL, l2);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
    Label l4 = new Label();
    mv.visitJumpInsn(GOTO, l4);
    mv.visitLabel(l2);
    mv.visitInsn(ICONST_0);
    mv.visitLabel(l4);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionAttributeListener");
    mv.visitVarInsn(ASTORE, 2);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARRAYLENGTH);
    Label l6 = new Label();
    mv.visitJumpInsn(IFLE, l6);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitInsn(POP);
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARETURN);
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l8, 0);
    mv.visitLocalVariable("listenerList", "Ljava/util/List;", null, l1, l8, 1);
    mv.visitLocalVariable("listeners", "[Ljavax/servlet/http/HttpSessionAttributeListener;", null, l5, l8, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  private void addSessionListener() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getHttpSessionListener",
                                         "()[Ljavax/servlet/http/HttpSessionListener;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getHttpSessionListeners",
                       "()Ljava/util/ArrayList;");
    mv.visitVarInsn(ASTORE, 1);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 1);
    Label l2 = new Label();
    mv.visitJumpInsn(IFNULL, l2);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "size", "()I");
    Label l3 = new Label();
    mv.visitJumpInsn(GOTO, l3);
    mv.visitLabel(l2);
    mv.visitInsn(ICONST_0);
    mv.visitLabel(l3);
    mv.visitTypeInsn(ANEWARRAY, "javax/servlet/http/HttpSessionListener");
    mv.visitVarInsn(ASTORE, 2);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARRAYLENGTH);
    Label l5 = new Label();
    mv.visitJumpInsn(IFLE, l5);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "([Ljava/lang/Object;)[Ljava/lang/Object;");
    mv.visitInsn(POP);
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitInsn(ARETURN);
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l7, 0);
    mv.visitLocalVariable("listenerList", "Ljava/util/List;", null, l1, l7, 1);
    mv.visitLocalVariable("listeners", "[Ljavax/servlet/http/HttpSessionListener;", null, l4, l7, 2);
    mv.visitMaxs(2, 3);
    mv.visitEnd();
  }

  private void addSessionCookieComment() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieComment", "()Ljava/lang/String;", null,
                                         null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieComment",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
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
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieDomain",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieMaxAge() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieMaxAgeSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieMaxAge",
                       "()I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieName() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieName", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieName",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookiePath() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiePath", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookiePath",
                       "()Ljava/lang/String;");
    mv.visitInsn(ARETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieSecure() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookieSecure", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionCookieSecure",
                       "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionCookieEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getCookiesEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "isUsingCookies", "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionIdLength() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getIdLength", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitFieldInsn(GETSTATIC, "com/ibm/ws/webcontainer/httpsession/SessionContext", "sessionIDLength", "I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionTimeoutSeconds() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getSessionTimeoutSecs", "()I", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "getSessionTimeOut", "()I");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addServerId() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getServerId", "()Ljava/lang/String;", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
    mv.visitInsn(DUP);
    mv
        .visitLdcInsn("Assertion failed: __tc_session_getServerId() should not be called here, it should be implemented instead in com.ibm.ws.webcontainer.webapp.WebApp");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
    mv.visitInsn(ATHROW);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(3, 1);
    mv.visitEnd();
  }

  private void addSessionTrackingEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getTrackingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "isSessionTrackingActive",
                       "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

  private void addSessionUrlRewritingEnabled() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "__tc_session_getURLRewritingEnabled", "()Z", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/httpsession/SessionContext", "isUsingURL", "()Z");
    mv.visitInsn(IRETURN);
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLocalVariable("this", "Lcom/ibm/ws/webcontainer/httpsession/SessionContext;", null, l0, l1, 0);
    mv.visitMaxs(1, 1);
    mv.visitEnd();
  }

}
