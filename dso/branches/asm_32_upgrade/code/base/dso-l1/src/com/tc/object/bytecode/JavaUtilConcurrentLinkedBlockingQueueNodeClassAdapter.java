/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;
import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

public class JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilConcurrentLinkedBlockingQueueNodeClassAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visitEnd() {
    addGetItemMethod();
    super.visitEnd();
  }
  
  private void addGetItemMethod() {
    MethodVisitor mv = cv.visitMethod(0, "getItem", "()Ljava/lang/Object;", "()TE;", null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(64, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitTypeInsn(INSTANCEOF, "com/tc/object/ObjectID");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(65, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitTypeInsn(CHECKCAST, "com/tc/object/ObjectID");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "lookupObject", "(Lcom/tc/object/ObjectID;)Ljava/lang/Object;");
    mv.visitFieldInsn(PUTFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitLabel(l1);
    mv.visitLineNumber(67, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue$Node", "item", "Ljava/lang/Object;");
    mv.visitInsn(ARETURN);
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLocalVariable("this", "Ljava/util/concurrent/LinkedBlockingQueue$Node;", "Ljava/util/concurrent/LinkedBlockingQueue<TE;>.Node<TE;>;", l0, l3, 0);
    mv.visitMaxs(2, 1);
    mv.visitEnd();
  }
}