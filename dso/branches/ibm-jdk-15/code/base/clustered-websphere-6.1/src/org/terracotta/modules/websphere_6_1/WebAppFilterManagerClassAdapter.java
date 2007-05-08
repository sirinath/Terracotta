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
//    if (name.equals("getFilterChain")
//        && desc
//            .equals("(Ljava/lang/String;Lcom/ibm/wsspi/webcontainer/servlet/IServletWrapper;I)Lcom/ibm/ws/webcontainer/filter/WebAppFilterChain;")) {
//      mv = new GetFilterChainMethodAdapter(mv);
//    } else if (name.equals("doFilter")) {
//      mv = new DoFilterMethodAdapter(mv);
//    }

    return mv;
  }

  public void visitEnd() {
    //createDSOFilterMethod();
    createLoadDSOFilterMethod();
    super.visitEnd();
  }
  
  private void createLoadDSOFilterMethod() {
    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "loadDSOFilterInfo", "()V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(49, l0);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("!!! In loadDSOFilter !!! context root: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitLineNumber(50, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(49, l2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(51, l3);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLineNumber(52, l4);
    mv.visitLdcInsn("dsoSessionFilter");
    mv.visitVarInsn(ASTORE, 1);
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitLineNumber(53, l5);
    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
    mv.visitVarInsn(ASTORE, 2);
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitLineNumber(54, l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    Label l7 = new Label();
    mv.visitLabel(l7);
    mv.visitLineNumber(55, l7);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitLineNumber(54, l8);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions", "(Ljava/lang/String;)Z");
    Label l9 = new Label();
    mv.visitJumpInsn(IFEQ, l9);
    Label l10 = new Label();
    mv.visitLabel(l10);
    mv.visitLineNumber(56, l10);
    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("context: ");
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    Label l11 = new Label();
    mv.visitLabel(l11);
    mv.visitLineNumber(57, l11);
    mv.visitLdcInsn(" is dso session!!!");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
    Label l12 = new Label();
    mv.visitLabel(l12);
    mv.visitLineNumber(56, l12);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
    Label l13 = new Label();
    mv.visitLabel(l13);
    mv.visitLineNumber(58, l13);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
    mv.visitInsn(DUP);
    Label l14 = new Label();
    mv.visitLabel(l14);
    mv.visitLineNumber(59, l14);
    mv.visitLdcInsn("dsoSessionFilterConfig");
    Label l15 = new Label();
    mv.visitLabel(l15);
    mv.visitLineNumber(58, l15);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
    mv.visitVarInsn(ASTORE, 3);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitLdcInsn("app-server");
    mv.visitLdcInsn("IBM-Websphere");
    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "addInitParameter", "(Ljava/lang/String;Ljava/lang/String;)V");
    Label l16 = new Label();
    mv.visitLabel(l16);
    mv.visitLineNumber(60, l16);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setFilterClassName", "(Ljava/lang/String;)V");
    Label l17 = new Label();
    mv.visitLabel(l17);
    mv.visitLineNumber(61, l17);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setName", "(Ljava/lang/String;)V");
    Label l18 = new Label();
    mv.visitLabel(l18);
    mv.visitLineNumber(62, l18);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
    mv.visitVarInsn(ALOAD, 3);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo", "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
    Label l19 = new Label();
    mv.visitLabel(l19);
    mv.visitLineNumber(64, l19);
    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterMapping");
    mv.visitInsn(DUP);
    mv.visitLdcInsn("/*");
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ACONST_NULL);
    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterMapping", "<init>", "(Ljava/lang/String;Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;Lcom/ibm/wsspi/webcontainer/servlet/IServletConfig;)V");
    mv.visitVarInsn(ASTORE, 4);
    Label l20 = new Label();
    mv.visitLabel(l20);
    mv.visitLineNumber(65, l20);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 4);
    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "addFilterMapping", "(Lcom/ibm/ws/webcontainer/filter/FilterMapping;)V");
    mv.visitLabel(l9);
    mv.visitLineNumber(67, l9);
    mv.visitInsn(RETURN);
    Label l21 = new Label();
    mv.visitLabel(l21);
    mv.visitMaxs(5, 5);
    mv.visitEnd();
  }
  
//  private void createDSOFilterMethod() {
//    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "loadDSOFilterConfig", "(Lcom/ibm/ws/webcontainer/filter/WebAppFilterChain;)Lcom/ibm/ws/webcontainer/filter/WebAppFilterChain;", null, new String[] { "javax/servlet/ServletException" });
//    mv.visitCode();
//    Label l0 = new Label();
//    Label l1 = new Label();
//    mv.visitTryCatchBlock(l0, l1, l1, "java/lang/Exception");
//    Label l2 = new Label();
//    mv.visitLabel(l2);
//    mv.visitLineNumber(40, l2);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("!!! In loadDSOFilter !!! context root: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    Label l3 = new Label();
//    mv.visitLabel(l3);
//    mv.visitLineNumber(41, l3);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    Label l4 = new Label();
//    mv.visitLabel(l4);
//    mv.visitLineNumber(40, l4);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l5 = new Label();
//    mv.visitLabel(l5);
//    mv.visitLineNumber(42, l5);
//    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
//    Label l6 = new Label();
//    mv.visitLabel(l6);
//    mv.visitLineNumber(43, l6);
//    mv.visitLdcInsn("dsoSessionFilter");
//    mv.visitVarInsn(ASTORE, 2);
//    Label l7 = new Label();
//    mv.visitLabel(l7);
//    mv.visitLineNumber(44, l7);
//    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
//    mv.visitVarInsn(ASTORE, 3);
//    mv.visitLabel(l0);
//    mv.visitLineNumber(46, l0);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    Label l8 = new Label();
//    mv.visitLabel(l8);
//    mv.visitLineNumber(47, l8);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    Label l9 = new Label();
//    mv.visitLabel(l9);
//    mv.visitLineNumber(46, l9);
//    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions", "(Ljava/lang/String;)Z");
//    Label l10 = new Label();
//    mv.visitJumpInsn(IFEQ, l10);
//    Label l11 = new Label();
//    mv.visitLabel(l11);
//    mv.visitLineNumber(48, l11);
////    mv.visitVarInsn(ALOAD, 0);
////    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webApp", "Lcom/ibm/ws/webcontainer/webapp/WebApp;");
////    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getClassLoader", "()Ljava/lang/ClassLoader;");
////    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/SessionsHelper", "injectClasses", "(Ljava/lang/ClassLoader;)V");
//    Label l12 = new Label();
//    mv.visitLabel(l12);
//    mv.visitLineNumber(49, l12);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("context: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    Label l13 = new Label();
//    mv.visitLabel(l13);
//    mv.visitLineNumber(50, l13);
//    mv.visitLdcInsn(" is dso session!!!");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    Label l14 = new Label();
//    mv.visitLabel(l14);
//    mv.visitLineNumber(49, l14);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l15 = new Label();
//    mv.visitLabel(l15);
//    mv.visitLineNumber(51, l15);
//    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
//    mv.visitInsn(DUP);
//    Label l16 = new Label();
//    mv.visitLabel(l16);
//    mv.visitLineNumber(52, l16);
//    mv.visitLdcInsn("dsoSessionFilterConfig");
//    Label l17 = new Label();
//    mv.visitLabel(l17);
//    mv.visitLineNumber(51, l17);
//    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ASTORE, 4);
//    Label l18 = new Label();
//    mv.visitLabel(l18);
//    mv.visitLineNumber(53, l18);
//    mv.visitVarInsn(ALOAD, 4);
//    mv.visitVarInsn(ALOAD, 3);
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setFilterClassName", "(Ljava/lang/String;)V");
//    Label l19 = new Label();
//    mv.visitLabel(l19);
//    mv.visitLineNumber(54, l19);
//    mv.visitVarInsn(ALOAD, 4);
//    mv.visitVarInsn(ALOAD, 2);
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setName", "(Ljava/lang/String;)V");
//    Label l20 = new Label();
//    mv.visitLabel(l20);
//    mv.visitLineNumber(55, l20);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitVarInsn(ALOAD, 4);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo", "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
//    Label l21 = new Label();
//    mv.visitLabel(l21);
//    mv.visitLineNumber(57, l21);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitVarInsn(ALOAD, 2);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getFilterInfo", "(Ljava/lang/String;)Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;");
//    mv.visitVarInsn(ASTORE, 5);
//    Label l22 = new Label();
//    mv.visitLabel(l22);
//    mv.visitLineNumber(58, l22);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("getting back filterInfo: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 5);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l23 = new Label();
//    mv.visitLabel(l23);
//    mv.visitLineNumber(59, l23);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("filter name: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    Label l24 = new Label();
//    mv.visitLabel(l24);
//    mv.visitLineNumber(60, l24);
//    mv.visitVarInsn(ALOAD, 4);
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "getFilterName", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    Label l25 = new Label();
//    mv.visitLabel(l25);
//    mv.visitLineNumber(59, l25);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l26 = new Label();
//    mv.visitLabel(l26);
//    mv.visitLineNumber(62, l26);
//    mv.visitVarInsn(ALOAD, 1);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitVarInsn(ALOAD, 2);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "getFilterInstanceWrapper", "(Ljava/lang/String;)Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterChain", "addFilter", "(Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;)V");
//    Label l27 = new Label();
//    mv.visitJumpInsn(GOTO, l27);
//    mv.visitLabel(l10);
//    mv.visitLineNumber(64, l10);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("context: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    Label l28 = new Label();
//    mv.visitLabel(l28);
//    mv.visitLineNumber(65, l28);
//    mv.visitLdcInsn(" is not dso session!!!");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    Label l29 = new Label();
//    mv.visitLabel(l29);
//    mv.visitLineNumber(64, l29);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    mv.visitJumpInsn(GOTO, l27);
//    mv.visitLabel(l1);
//    mv.visitLineNumber(67, l1);
//    mv.visitVarInsn(ASTORE, 4);
//    Label l30 = new Label();
//    mv.visitLabel(l30);
//    mv.visitLineNumber(70, l30);
//    mv.visitVarInsn(ALOAD, 4);
//    Label l31 = new Label();
//    mv.visitLabel(l31);
//    mv.visitLineNumber(71, l31);
//    mv.visitLdcInsn("com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadDSOFilterConfig");
//    Label l32 = new Label();
//    mv.visitLabel(l32);
//    mv.visitLineNumber(72, l32);
//    mv.visitLdcInsn("298");
//    mv.visitVarInsn(ALOAD, 0);
//    Label l33 = new Label();
//    mv.visitLabel(l33);
//    mv.visitLineNumber(69, l33);
//    mv.visitMethodInsn(INVOKESTATIC, "com/ibm/ws/ffdc/FFDCFilter", "processException", "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
//    Label l34 = new Label();
//    mv.visitLabel(l34);
//    mv.visitLineNumber(73, l34);
//    mv.visitTypeInsn(NEW, "javax/servlet/ServletException");
//    mv.visitInsn(DUP);
//    Label l35 = new Label();
//    mv.visitLabel(l35);
//    mv.visitLineNumber(76, l35);
//    mv.visitFieldInsn(GETSTATIC, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "nls", "Lcom/ibm/ejs/sm/client/ui/NLS;");
//    Label l36 = new Label();
//    mv.visitLabel(l36);
//    mv.visitLineNumber(78, l36);
//    mv.visitLdcInsn("Could.not.find.required.filter.class");
//    Label l37 = new Label();
//    mv.visitLabel(l37);
//    mv.visitLineNumber(79, l37);
//    mv.visitLdcInsn("Filter [{0}]: Could not find required filter class - {1}.class");
//    Label l38 = new Label();
//    mv.visitLabel(l38);
//    mv.visitLineNumber(77, l38);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/sm/client/ui/NLS", "getString", "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
//    Label l39 = new Label();
//    mv.visitLabel(l39);
//    mv.visitLineNumber(80, l39);
//    mv.visitInsn(ICONST_2);
//    mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
//    mv.visitInsn(DUP);
//    mv.visitInsn(ICONST_0);
//    Label l40 = new Label();
//    mv.visitLabel(l40);
//    mv.visitLineNumber(80, l40);
//    mv.visitVarInsn(ALOAD, 2);
//    mv.visitInsn(AASTORE);
//    mv.visitInsn(DUP);
//    mv.visitInsn(ICONST_1);
//    Label l41 = new Label();
//    mv.visitLabel(l41);
//    mv.visitLineNumber(81, l41);
//    mv.visitVarInsn(ALOAD, 3);
//    mv.visitInsn(AASTORE);
//    Label l42 = new Label();
//    mv.visitLabel(l42);
//    mv.visitLineNumber(75, l42);
//    mv.visitMethodInsn(INVOKESTATIC, "java/text/MessageFormat", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
//    Label l43 = new Label();
//    mv.visitLabel(l43);
//    mv.visitLineNumber(82, l43);
//    mv.visitVarInsn(ALOAD, 4);
//    Label l44 = new Label();
//    mv.visitLabel(l44);
//    mv.visitLineNumber(73, l44);
//    mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletException", "<init>", "(Ljava/lang/String;Ljava/lang/Throwable;)V");
//    mv.visitInsn(ATHROW);
//    mv.visitLabel(l27);
//    mv.visitLineNumber(84, l27);
//    mv.visitVarInsn(ALOAD, 1);
//    mv.visitInsn(ARETURN);
//    Label l45 = new Label();
//    mv.visitLabel(l45);
//    mv.visitMaxs(7, 6);
//    mv.visitEnd();
//
//  }

//  private void createDSOFilterMethod() {
//    MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "loadDSOFilterConfig", "()V", null,
//                                      new String[] { "javax/servlet/ServletException" });
//    mv.visitCode();
//    Label l0 = new Label();
//    mv.visitLabel(l0);
//    mv.visitLineNumber(33, l0);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("!!! In loadDSOFilter !!! context root: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l1 = new Label();
//    mv.visitLabel(l1);
//    mv.visitLineNumber(34, l1);
//    mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
//    Label l2 = new Label();
//    mv.visitLabel(l2);
//    mv.visitLineNumber(35, l2);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/hook/impl/ClassProcessorHelper", "isDSOSessions", "(Ljava/lang/String;)Z");
//    Label l3 = new Label();
//    mv.visitJumpInsn(IFEQ, l3);
//    Label l4 = new Label();
//    mv.visitLabel(l4);
//    mv.visitLineNumber(36, l4);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("context: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitLdcInsn(" is dso session!!!");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l5 = new Label();
//    mv.visitLabel(l5);
//    mv.visitLineNumber(37, l5);
//    mv.visitTypeInsn(NEW, "com/ibm/ws/webcontainer/filter/FilterConfig");
//    mv.visitInsn(DUP);
//    Label l6 = new Label();
//    mv.visitLabel(l6);
//    mv.visitLineNumber(38, l6);
//    mv.visitLdcInsn("dsoSessionFilterConfig");
//    Label l7 = new Label();
//    mv.visitLabel(l7);
//    mv.visitLineNumber(37, l7);
//    mv.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/FilterConfig", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ASTORE, 1);
//    Label l8 = new Label();
//    mv.visitLabel(l8);
//    mv.visitLineNumber(39, l8);
//    mv.visitVarInsn(ALOAD, 1);
//    Label l9 = new Label();
//    mv.visitLabel(l9);
//    mv.visitLineNumber(40, l9);
//    mv.visitLdcInsn("com.terracotta.session.SessionFilter");
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setFilterClassName", "(Ljava/lang/String;)V");
//    Label l10 = new Label();
//    mv.visitLabel(l10);
//    mv.visitLineNumber(41, l10);
//    mv.visitVarInsn(ALOAD, 1);
//    mv.visitLdcInsn("dsoSessionFilter");
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "setName", "(Ljava/lang/String;)V");
//    Label l11 = new Label();
//    mv.visitLabel(l11);
//    mv.visitLineNumber(42, l11);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitVarInsn(ALOAD, 1);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "addFilterInfo", "(Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;)V");
//    Label l12 = new Label();
//    mv.visitLabel(l12);
//    mv.visitLineNumber(44, l12);
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitLdcInsn("dsoSessionFilter");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getFilterInfo", "(Ljava/lang/String;)Lcom/ibm/wsspi/webcontainer/filter/IFilterConfig;");
//    mv.visitVarInsn(ASTORE, 2);
//    Label l13 = new Label();
//    mv.visitLabel(l13);
//    mv.visitLineNumber(45, l13);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("getting back filterInfo: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 2);
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l14 = new Label();
//    mv.visitLabel(l14);
//    mv.visitLineNumber(46, l14);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("filter name: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 1);
//    mv.visitMethodInsn(INVOKEINTERFACE, "com/ibm/wsspi/webcontainer/filter/IFilterConfig", "getFilterName", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    Label l15 = new Label();
//    mv.visitJumpInsn(GOTO, l15);
//    mv.visitLabel(l3);
//    mv.visitLineNumber(48, l3);
//    mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//    mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
//    mv.visitInsn(DUP);
//    mv.visitLdcInsn("context: ");
//    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V");
//    mv.visitVarInsn(ALOAD, 0);
//    mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webAppConfig", "Lcom/ibm/ws/webcontainer/webapp/WebAppConfiguration;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebAppConfiguration", "getContextRoot", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitLdcInsn(" is not dso session!!!");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;");
//    mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    mv.visitLabel(l15);
//    mv.visitLineNumber(50, l15);
//    mv.visitInsn(RETURN);
//    Label l16 = new Label();
//    mv.visitLabel(l16);
//    mv.visitMaxs(4, 3);
//    mv.visitEnd();
//  }

  // private void createDSOFilterMethod() {
  // MethodVisitor mv = cv.visitMethod(ACC_PRIVATE, "loadDSOFilter",
  // "()Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;", null, new String[] { "javax/servlet/ServletException"
  // });
  // mv.visitCode();
  // Label l0 = new Label();
  // Label l1 = new Label();
  // Label l2 = new Label();
  // mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
  // Label l3 = new Label();
  // mv.visitLabel(l3);
  // mv.visitLineNumber(29, l3);
  // mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
  // mv.visitLdcInsn("!!! In loadDSOFilter !!!");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
  // Label l4 = new Label();
  // mv.visitLabel(l4);
  // mv.visitLineNumber(30, l4);
  // mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
  // mv.visitLabel(l0);
  // mv.visitLineNumber(32, l0);
  // mv.visitVarInsn(ALOAD, 0);
  // mv.visitFieldInsn(GETFIELD, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "webApp",
  // "Lcom/ibm/ws/webcontainer/webapp/WebApp;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/webapp/WebApp", "getClassLoader",
  // "()Ljava/lang/ClassLoader;");
  // Label l5 = new Label();
  // mv.visitLabel(l5);
  // mv.visitLineNumber(33, l5);
  // mv.visitLdcInsn("com.terracotta.session.SessionFilter");
  // Label l6 = new Label();
  // mv.visitLabel(l6);
  // mv.visitLineNumber(32, l6);
  // mv.visitMethodInsn(INVOKESTATIC, "java/beans/Beans", "instantiate",
  // "(Ljava/lang/ClassLoader;Ljava/lang/String;)Ljava/lang/Object;");
  // mv.visitTypeInsn(CHECKCAST, "javax/servlet/Filter");
  // mv.visitVarInsn(ASTORE, 1);
  // Label l7 = new Label();
  // mv.visitLabel(l7);
  // mv.visitLineNumber(34, l7);
  // mv.visitVarInsn(ALOAD, 0);
  // mv.visitLdcInsn("sessionFilter");
  // mv.visitVarInsn(ALOAD, 1);
  // mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager",
  // "createFilterInstanceWrapper",
  // "(Ljava/lang/String;Ljavax/servlet/Filter;)Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;");
  // mv.visitLabel(l1);
  // mv.visitInsn(ARETURN);
  // mv.visitLabel(l2);
  // mv.visitLineNumber(35, l2);
  // mv.visitVarInsn(ASTORE, 1);
  // Label l8 = new Label();
  // mv.visitLabel(l8);
  // mv.visitLineNumber(36, l8);
  // mv.visitVarInsn(ALOAD, 1);
  // mv.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
  // mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "printStackTrace", "(Ljava/io/PrintStream;)V");
  // Label l9 = new Label();
  // mv.visitLabel(l9);
  // mv.visitLineNumber(39, l9);
  // mv.visitVarInsn(ALOAD, 1);
  // Label l10 = new Label();
  // mv.visitLabel(l10);
  // mv.visitLineNumber(40, l10);
  // mv.visitLdcInsn("com.ibm.ws.webcontainer.filter.WebAppFilterManager.loadFilter");
  // Label l11 = new Label();
  // mv.visitLabel(l11);
  // mv.visitLineNumber(41, l11);
  // mv.visitLdcInsn("298");
  // mv.visitVarInsn(ALOAD, 0);
  // Label l12 = new Label();
  // mv.visitLabel(l12);
  // mv.visitLineNumber(38, l12);
  // mv.visitMethodInsn(INVOKESTATIC, "com/ibm/ws/ffdc/FFDCFilter", "processException",
  // "(Ljava/lang/Throwable;Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;)V");
  // Label l13 = new Label();
  // mv.visitLabel(l13);
  // mv.visitLineNumber(42, l13);
  // mv.visitTypeInsn(NEW, "javax/servlet/ServletException");
  // mv.visitInsn(DUP);
  // Label l14 = new Label();
  // mv.visitLabel(l14);
  // mv.visitLineNumber(45, l14);
  // mv.visitFieldInsn(GETSTATIC, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "nls",
  // "Lcom/ibm/ejs/sm/client/ui/NLS;");
  // Label l15 = new Label();
  // mv.visitLabel(l15);
  // mv.visitLineNumber(47, l15);
  // mv.visitLdcInsn("Could.not.find.required.filter.class");
  // Label l16 = new Label();
  // mv.visitLabel(l16);
  // mv.visitLineNumber(48, l16);
  // mv.visitLdcInsn("Filter [{0}]: Could not find required filter class - {1}.class");
  // Label l17 = new Label();
  // mv.visitLabel(l17);
  // mv.visitLineNumber(46, l17);
  // mv.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ejs/sm/client/ui/NLS", "getString",
  // "(Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;");
  // Label l18 = new Label();
  // mv.visitLabel(l18);
  // mv.visitLineNumber(49, l18);
  // mv.visitInsn(ICONST_2);
  // mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
  // mv.visitInsn(DUP);
  // mv.visitInsn(ICONST_0);
  // Label l19 = new Label();
  // mv.visitLabel(l19);
  // mv.visitLineNumber(49, l19);
  // mv.visitLdcInsn("sessionFilter");
  // mv.visitInsn(AASTORE);
  // mv.visitInsn(DUP);
  // mv.visitInsn(ICONST_1);
  // Label l20 = new Label();
  // mv.visitLabel(l20);
  // mv.visitLineNumber(50, l20);
  // mv.visitLdcInsn("com.terracotta.session.SessionFilter");
  // mv.visitInsn(AASTORE);
  // Label l21 = new Label();
  // mv.visitLabel(l21);
  // mv.visitLineNumber(44, l21);
  // mv.visitMethodInsn(INVOKESTATIC, "java/text/MessageFormat", "format",
  // "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
  // Label l22 = new Label();
  // mv.visitLabel(l22);
  // mv.visitLineNumber(51, l22);
  // mv.visitVarInsn(ALOAD, 1);
  // Label l23 = new Label();
  // mv.visitLabel(l23);
  // mv.visitLineNumber(42, l23);
  // mv.visitMethodInsn(INVOKESPECIAL, "javax/servlet/ServletException", "<init>",
  // "(Ljava/lang/String;Ljava/lang/Throwable;)V");
  // mv.visitInsn(ATHROW);
  // Label l24 = new Label();
  // mv.visitLabel(l24);
  // mv.visitMaxs(0, 0);
  // mv.visitEnd();
  // }

  private static class InitMethodAdapter extends MethodAdapter implements Opcodes {
    public InitMethodAdapter(MethodVisitor mv) {
      super(mv);
    }
    
    public void visitCode() {
      super.visitCode();
      super.visitVarInsn(ALOAD, 0);
      super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "loadDSOFilterInfo", "()V");
    }
  }
  
//  private static class DoFilterMethodAdapter extends MethodAdapter implements Opcodes {
//    public DoFilterMethodAdapter(MethodVisitor mv) {
//      super(mv);
//    }
//
//    public void visitCode() {
//      super.visitCode();
//      super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//      super.visitLdcInsn("!!!In doFilter!!! ...");
//      super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
//    }
//  }
//
//  private static class GetFilterChainMethodAdapter extends MethodAdapter implements Opcodes {
//    public GetFilterChainMethodAdapter(MethodVisitor mv) {
//      super(mv);
//    }
//
//    public void visitCode() {
//      super.visitCode();
//      super.visitFieldInsn(GETSTATIC, "java/lang/System", "err", "Ljava/io/PrintStream;");
//      super.visitLdcInsn("!!!In GetFilterChains ...");
//      super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//      mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "dumpStack", "()V");
//    }
//    
//    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
////      if (opcode == INVOKEVIRTUAL && "com/ibm/ws/webcontainer/filter/WebAppFilterChain".equals(owner)
////          && "setRequestedServlet".equals(name)
////          && "(Lcom/ibm/wsspi/webcontainer/servlet/IServletWrapper;)V".equals(desc)) {
////        // super.visitInsn(POP);
////        // super.visitVarInsn(ALOAD, 0);
////        // super.visitLdcInsn("com.terracotta.session.SessionFilter");
////        // super.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager",
////        // "getFilterInstanceWrapper", "(Ljava/lang/String;)Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;");
////        // super.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterChain", "addFilter",
////        // "(Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;)V");
////        super.visitInsn(POP);
////        super.visitVarInsn(ALOAD, 0);
////        super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager",
////                              "loadDSOFilterConfig", "()V");
////        
////        super.visitVarInsn(ALOAD, 0);
////        super.visitLdcInsn("dsoSessionFilter");
////        super.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager",
////                              "getFilterInstanceWrapper",
////                              "(Ljava/lang/String;)Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;");
////        super.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterChain", "addFilter",
////                              "(Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;)V");
////
////        // super.visitMethodInsn(INVOKEVIRTUAL, "com/ibm/ws/webcontainer/filter/WebAppFilterChain", "addFilter",
////        // "(Lcom/ibm/ws/webcontainer/filter/FilterInstanceWrapper;)V");
////        super.visitVarInsn(ALOAD, 7);
////        super.visitVarInsn(ALOAD, 2);
////      }
//      super.visitMethodInsn(opcode, owner, name, desc);
//      if (INVOKESPECIAL == opcode && "com/ibm/ws/webcontainer/filter/WebAppFilterChain".equals(owner) &&
//          "<init>".equals(name) && "(Lcom/ibm/ws/webcontainer/webapp/WebApp;)V".equals(desc)) {
//        super.visitMethodInsn(INVOKESPECIAL, "com/ibm/ws/webcontainer/filter/WebAppFilterManager", "loadDSOFilterConfig", "(Lcom/ibm/ws/webcontainer/filter/WebAppFilterChain;)Lcom/ibm/ws/webcontainer/filter/WebAppFilterChain;");
//      }
//    }
//    
//    public void visitTypeInsn(int opcode, String desc) {
//      if (NEW == opcode && "com/ibm/ws/webcontainer/filter/WebAppFilterChain".equals(desc)) {
//        mv.visitVarInsn(ALOAD, 0);
//      }
//      super.visitTypeInsn(opcode, desc);
//    }
//  }
}
