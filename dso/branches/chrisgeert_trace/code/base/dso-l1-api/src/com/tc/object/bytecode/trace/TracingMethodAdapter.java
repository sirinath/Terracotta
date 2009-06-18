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
    super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/trace/TraceListener", "methodEnter", "()V");
    super.visitJumpInsn(GOTO, finish);
    
    super.visitLabel(nullListener);
    super.visitInsn(POP);
    super.visitLabel(finish);
  }
  
  @Override
  protected void onMethodExit(int opcode) {
    Label nullListener = new Label();
    Label finish = new Label();

    super.visitFieldInsn(GETSTATIC, info.getDeclaringType().getName().replace('.', '/'), listenerField, "Lcom/tc/object/bytecode/trace/TraceListener;");
    super.visitInsn(DUP);
    super.visitJumpInsn(IFNULL, nullListener);
    super.visitLdcInsn(Integer.valueOf(opcode));
    super.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/bytecode/trace/TraceListener", "methodExit", "(I)V");
    super.visitJumpInsn(GOTO, finish);
    
    super.visitLabel(nullListener);
    super.visitInsn(POP);
    super.visitLabel(finish);
  }
}
