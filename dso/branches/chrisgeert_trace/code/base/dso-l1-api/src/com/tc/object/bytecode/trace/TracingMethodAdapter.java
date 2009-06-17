/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.bytecode.trace;

import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.aspectwerkz.reflect.MemberInfo;

public class TracingMethodAdapter extends AdviceAdapter implements MethodVisitor {

  private final MemberInfo info;
  private final String listenerField;
  
  public TracingMethodAdapter(MethodVisitor mv, String name, MemberInfo info, String listenerField) {
    super(mv, info.getModifiers(), name, info.getSignature());
    this.info = info;
    this.listenerField = listenerField;
  }
  
  @Override
  protected void onMethodEnter() {
    Label nullListener = new Label();
    Label finish = new Label();

    super.visitFieldInsn(GETSTATIC, info.getDeclaringType().getName().replace('.', '/'), listenerField, "Lcom/tc/object/bytecode/trace/TraceListener;");
    super.visitInsn(DUP);
    super.visitJumpInsn(IFNULL, nullListener);
    super.visitLdcInsn(info.getDeclaringType().getName());
    super.visitLdcInsn(info.getName() + info.getSignature());
    super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/trace/TraceListener", "methodEnter", "(Ljava/lang/String;Ljava/lang/String;)V");
    super.visitJumpInsn(GOTO, finish);
    
    super.visitLabel(nullListener);
    super.visitInsn(POP);
    super.visitLabel(finish);
  }
  
  @Override
  protected void onMethodExit(int opcode) {
//    Label start = new Label();
//    Label end = new Label();
//    Label handler = new Label();
//    Label finish = new Label();
//    super.visitTryCatchBlock(start, end, handler, null);
//
//    super.visitLabel(start);
//    super.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
//    super.visitLdcInsn("Exiting Method " + name);
//    super.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
//    super.visitLabel(end);
//    super.visitJumpInsn(GOTO, finish);
//    super.visitLabel(handler);
//    super.visitInsn(POP);      
//    super.visitLabel(finish);
  }
}
