/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.SerializationUtil;

public class JavaUtilConcurrentLinkedBlockingQueueAdapter implements Opcodes {

  public static class PutAdapter extends AbstractMethodAdapter {
    private int putLockVar;
    private int atomicIntegerVar;
    private int countVar;
    
    public PutAdapter(String originalMethodSignature) {
      if (SerializationUtil.OFFER_SIGNATURE.equals(originalMethodSignature)) {
        this.putLockVar = 4;
        this.atomicIntegerVar = 2;
        this.countVar = 3;
      } else if (SerializationUtil.OFFER_TIMEOUT_SIGNATURE.equals(originalMethodSignature)) {
        this.putLockVar = 8;
        this.atomicIntegerVar = 9;
        this.countVar = 7;
      } else if (SerializationUtil.QUEUE_PUT_SIGNATURE.equals(originalMethodSignature)) {
        this.putLockVar = 3;
        this.atomicIntegerVar = 4;
        this.countVar = 2;
      }
    }
    
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new PutMethodAdapter(visitOriginal(classVisitor), SerializationUtil.QUEUE_PUT_SIGNATURE, this.putLockVar, this.atomicIntegerVar, this.countVar);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class ClearAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new ClearMethodAdapter(visitOriginal(classVisitor), SerializationUtil.CLEAR_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class RemoveFirstNAdapter extends AbstractMethodAdapter {
    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new RemoveFirstNMethodAdapter(visitOriginal(classVisitor), SerializationUtil.REMOVE_FIRST_N_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  public static class TakeAdapter extends AbstractMethodAdapter {

    public MethodVisitor adapt(ClassVisitor classVisitor) {
      return new TakeMethodAdapter(visitOriginal(classVisitor), SerializationUtil.TAKE_SIGNATURE);
    }

    public boolean doesOriginalNeedAdapting() {
      return false;
    }
  }

  private static class ClearMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;

    public ClearMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "next".equals(name)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(0));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class RemoveFirstNMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;

    public RemoveFirstNMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      super.visitFieldInsn(opcode, owner, name, desc);
      if (PUTFIELD == opcode && "next".equals(name) && "Ljava/util/concurrent/LinkedBlockingQueue$Node;".equals(desc)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(1));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");

      int count = 0;
      mv.visitInsn(DUP);
      mv.visitLdcInsn(new Integer(count++));
      mv.visitTypeInsn(NEW, "java/lang/Integer");
      mv.visitInsn(DUP);
      mv.visitVarInsn(ILOAD, 3);
      mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(I)V");
      mv.visitInsn(AASTORE);

      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvoke",
                         "(Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static class PutMethodAdapter extends MethodAdapter implements Opcodes {
    private final String invokeMethodSignature;
    private boolean dropStoreStatement = false;
    
    private int putLockVar;
    private int atomicIntegerVar;
    private int countVar;

    public PutMethodAdapter(MethodVisitor mv, String invokeMethodSignature, int putLockVar, int atomicIntegerVar, int countVar) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
      
      this.putLockVar = putLockVar;
      this.atomicIntegerVar = atomicIntegerVar;
      this.countVar = countVar;
    }
    
    /**
     * Changing the while (count.get() == capacity) condition to
     * while (count.get() >= capacity) due to the non-blocking version of put().
     */
    public void visitJumpInsn(int opcode, Label label) {
      if (IF_ICMPEQ == opcode) {
        opcode = IF_ICMPGE;
      }
      super.visitJumpInsn(opcode, label);
    }
    
    /*
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("insert".equals(name) && "(Ljava/lang/Object;)V".equals(desc)) {
        addLogicalInvokeMethodCall();
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "putLock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");

      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(1));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      int count = 0;
      mv.visitLdcInsn(new Integer(count++));
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                         "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
    */
    
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ("getAndIncrement".equals(name) && "()I".equals(desc)) {
        visitInsn(POP);
        addLogicalInvokeMethodCall();
        dropStoreStatement = true;
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

    
    // This is really a hack for jdk1.5 LinkedBlockingQueue implementation.
    public void visitVarInsn(int opcode, int var) {
      if (ISTORE == opcode && var == this.countVar) {
        if (!dropStoreStatement) {
          super.visitVarInsn(opcode, var);
        } else {
          dropStoreStatement = true;
        }
      } else {
        super.visitVarInsn(opcode, var);
      }
    }
    
    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      Label l12 = new Label();
      mv.visitLabel(l12);
      mv.visitLineNumber(308, l12);
      mv.visitVarInsn(ALOAD, this.atomicIntegerVar);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "get", "()I");
      mv.visitVarInsn(ISTORE, this.countVar);
      Label l13 = new Label();
      mv.visitLabel(l13);
      mv.visitLineNumber(309, l13);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, this.putLockVar);
      mv.visitLdcInsn("put(Ljava/lang/Object;)V");
      mv.visitInsn(ICONST_1);
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitInsn(DUP);
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitInsn(AASTORE);
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      Label l14 = new Label();
      mv.visitLabel(l14);
      mv.visitLineNumber(310, l14);
      mv.visitVarInsn(ALOAD, this.atomicIntegerVar);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
      mv.visitInsn(POP);
      Label l15 = new Label();
      mv.visitJumpInsn(GOTO, l15);
      mv.visitLabel(notManaged);
      mv.visitVarInsn(ALOAD, this.atomicIntegerVar);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicInteger", "getAndIncrement", "()I");
      mv.visitVarInsn(ISTORE, this.countVar);
      mv.visitLabel(l15);
    }
  }

  private static class TakeMethodAdapter extends MethodAdapter implements Opcodes {
    private boolean      visitExtract = false;
    private final String invokeMethodSignature;

    public TakeMethodAdapter(MethodVisitor mv, String invokeMethodSignature) {
      super(mv);
      this.invokeMethodSignature = invokeMethodSignature;
    }
    
    public void visitJumpInsn(int opcode, Label label) {
      if (IFEQ == opcode) {
        opcode = IFLE;
      }
      super.visitJumpInsn(opcode, label);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      super.visitMethodInsn(opcode, owner, name, desc);
      if ("extract".equals(name) && "()Ljava/lang/Object;".equals(desc)) {
        visitExtract = true;
      }
    }

    public void visitVarInsn(int opcode, int var) {
      if (ASTORE == opcode && visitExtract) {
        super.visitVarInsn(opcode, var);
        addLogicalInvokeMethodCall();
        visitExtract = false;
      } else {
        super.visitVarInsn(opcode, var);
      }
    }

    private void addLogicalInvokeMethodCall() {
      Label notManaged = new Label();
      addCheckedManagedCode(mv, notManaged);
      ByteCodeUtil.pushThis(mv);
      ByteCodeUtil.pushThis(mv);
      mv.visitFieldInsn(GETFIELD, "java/util/concurrent/LinkedBlockingQueue", "takeLock",
                        "Ljava/util/concurrent/locks/ReentrantLock;");
      mv.visitLdcInsn(invokeMethodSignature);

      mv.visitLdcInsn(new Integer(0));
      mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
      mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "logicalInvokeWithTransaction",
                         "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/String;[Ljava/lang/Object;)V");
      mv.visitLabel(notManaged);
    }
  }

  private static void addCheckedManagedCode(MethodVisitor mv, Label notManaged) {
    ByteCodeUtil.pushThis(mv);
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/bytecode/ManagerUtil", "isManaged", "(Ljava/lang/Object;)Z");
    mv.visitJumpInsn(IFEQ, notManaged);
  }
}