/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.lang.reflect.Modifier;

public class JavaUtilTreeMapAdapter extends ClassAdapter implements Opcodes {

  public JavaUtilTreeMapAdapter(ClassVisitor cv) {
    super(cv);
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { Clearable.class.getName().replace('.', '/') });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if ("writeObject".equals(name) && Modifier.isPrivate(access)) {
      mv = new WriteObjectAdapter(mv);
    }
    if ("get".equals(name)) {
      mv = new GetAdapter(mv);
    }

    return new EntryAdapter(mv);
  }

  public void visitEnd() {
    addRemoveEntryForKey();
    addClearableMethods();
    super.visitEnd();
  }

  private void addClearableMethods() {
    MethodVisitor mv;

    mv = super.visitMethod(ACC_PUBLIC, "setEvictionEnabled", "(Z)V", null, null);
    mv.visitCode();
    mv.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
    mv.visitInsn(DUP);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "()V");
    mv.visitInsn(ATHROW);
    mv.visitMaxs(2, 2);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC, "isEvictionEnabled", "()Z", null, null);
    mv.visitCode();
    mv.visitInsn(ICONST_1);
    mv.visitInsn(IRETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    mv = super.visitMethod(ACC_PUBLIC, "__tc_clearReferences", "(I)I", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap", "__tc_entrySet", "()Ljava/util/Set;");
    mv.visitVarInsn(ILOAD, 1);
    mv.visitMethodInsn(INVOKESTATIC, "java/util/TreeMapEntryWrapper", "clearReferences", "(Ljava/util/Set;I)I");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();
  }

  private void addRemoveEntryForKey() {
    MethodVisitor mv = super.visitMethod(ACC_SYNTHETIC, "removeEntryForKey",
                                         "(Ljava/lang/Object;)Ljava/util/TreeMap$Entry;", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap", "getEntry", "(Ljava/lang/Object;)Ljava/util/TreeMap$Entry;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 2);
    Label entryNotNull = new Label();
    mv.visitJumpInsn(IFNONNULL, entryNotNull);
    mv.visitInsn(ACONST_NULL);
    mv.visitInsn(ARETURN);
    mv.visitLabel(entryNotNull);
    mv.visitTypeInsn(NEW, "java/util/TreeMap$Entry");
    mv.visitInsn(DUP);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getKey", "()Ljava/lang/Object;");
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getValue", "()Ljava/lang/Object;");
    mv.visitInsn(ACONST_NULL);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap$Entry", "<init>",
                       "(Ljava/lang/Object;Ljava/lang/Object;Ljava/util/TreeMap$Entry;)V");
    mv.visitVarInsn(ASTORE, 3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKESPECIAL, "java/util/TreeMap", "deleteEntry", "(Ljava/util/TreeMap$Entry;)V");
    mv.visitVarInsn(ALOAD, 3);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private static class WriteObjectAdapter extends MethodAdapter implements Opcodes {

    public WriteObjectAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == GETFIELD) {
        if ("java/util/TreeMap$Entry".equals(owner)) {
          if ("key".equals(name)) {
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getKey", "()Ljava/lang/Object;");
          } else if ("value".equals(name)) {
            mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map$Entry", "getValue", "()Ljava/lang/Object;");
          } else {
            throw new AssertionError("unknown field name: " + name);
          }
          return;
        }
      }

      super.visitFieldInsn(opcode, owner, name, desc);
    }

    public void visitTypeInsn(int opcode, String desc) {
      if (CHECKCAST == opcode) {
        if ("java/util/TreeMap$Entry".equals(desc)) {
          super.visitTypeInsn(opcode, "java/util/Map$Entry");
          return;
        }
      }

      super.visitTypeInsn(opcode, desc);
    }

  }

  private static class EntryAdapter extends MethodAdapter implements Opcodes {

    public EntryAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (GETFIELD == opcode && "java/util/TreeMap$Entry".equals(owner) && "value".equals(name)) {
        super.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap$Entry", "getValue", "()Ljava/lang/Object;");
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((opcode == INVOKESPECIAL) && "java/util/TreeMap$Entry".equals(owner) && "<init>".equals(name)) {
        owner = "java/util/TreeMapEntryWrapper";
      }
      super.visitMethodInsn(opcode, owner, name, desc);
    }

    public void visitTypeInsn(int opcode, String type) {
      if ((NEW == opcode) && ("java/util/TreeMap$Entry".equals(type))) {
        type = "java/util/TreeMapEntryWrapper";
      }
      super.visitTypeInsn(opcode, type);
    }

  }

  private static class GetAdapter extends MethodAdapter {

    public GetAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitCode() {
      super.visitCode();
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap", "__tc_isManaged", "()Z");
      Label l1 = new Label();
      mv.visitJumpInsn(IFEQ, l1);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/TreeMap", "__tc_managed", "()Lcom/tc/object/TCObject;");
      mv.visitMethodInsn(INVOKEINTERFACE, "com/tc/object/TCObject", "markAccessed", "()V");
      mv.visitLabel(l1);
    }
  }

}
