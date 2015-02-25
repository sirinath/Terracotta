/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.ClassVisitor;
import com.tc.asm.Handle;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.Type;
import com.tc.asm.TypePath;

import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class DuplicateMethodAdapter extends ClassVisitor implements Opcodes {

  public static final String MANAGED_PREFIX   = "_managed_";
  public static final String UNMANAGED_PREFIX = ByteCodeUtil.TC_METHOD_PREFIX + "unmanaged_";

  private final Set          dontDupe;
  private String             ownerSlashes;
  private String             superClass;

  public DuplicateMethodAdapter(ClassVisitor cv) {
    this(cv, Collections.EMPTY_SET);
  }

  public DuplicateMethodAdapter(ClassVisitor cv, Set dontDupe) {
    super(Opcodes.ASM4, cv);
    this.dontDupe = new HashSet(dontDupe);
    this.dontDupe.add("readObject(Ljava/io/ObjectInputStream;)V");
    this.dontDupe.add("writeObject(Ljava/io/ObjectOutputStream;)V");
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    super.visit(version, access, name, signature, superName, interfaces);
    this.ownerSlashes = name;
    this.superClass = superName;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    if (name.startsWith(MANAGED_PREFIX) || name.startsWith(UNMANAGED_PREFIX)) {
      // make formatter sane
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if ("<init>".equals(name) || "<clinit>".equals(name)) {
      // don't need any special indirection on initializers
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if (Modifier.isStatic(access) || Modifier.isNative(access) || Modifier.isAbstract(access)) {
      // make formatter sane
      return super.visitMethod(access, name, desc, signature, exceptions);
    }

    if (dontDupe.contains(name + desc)) { return super.visitMethod(access, name, desc, signature, exceptions); }

    createSwitchMethod(access, name, desc, signature, exceptions);

    MethodVisitor managed = new RewriteSelfTypeCalls(super.visitMethod(access, MANAGED_PREFIX + name, desc, signature,
                                                                       exceptions), new String[] { ownerSlashes,
        superClass }, MANAGED_PREFIX);
    MethodVisitor unmanaged = new RewriteSelfTypeCalls(super.visitMethod(access, UNMANAGED_PREFIX + name, desc,
                                                                         signature, exceptions), new String[] {
        ownerSlashes, superClass }, UNMANAGED_PREFIX);

    return new MulticastMethodVisitor(new MethodVisitor[] { managed, unmanaged });
  }

  private void createSwitchMethod(int access, String name, String desc, String signature, String[] exceptions) {
    Type returnType = Type.getReturnType(desc);
    boolean isVoid = returnType.equals(Type.VOID_TYPE);
    MethodVisitor mv = super.visitMethod(access & (~ACC_SYNCHRONIZED), name, desc, signature, exceptions);
    Label notManaged = new Label();
    Label end = new Label();
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, ownerSlashes, ClassAdapterBase.MANAGED_METHOD, "()Lcom/tc/object/TCObject;");
    mv.visitJumpInsn(IFNULL, notManaged);
    ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, MANAGED_PREFIX + name, desc);
    if (!isVoid) {
      mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
    } else {
      mv.visitJumpInsn(GOTO, end);
    }
    mv.visitLabel(notManaged);
    ByteCodeUtil.prepareStackForMethodCall(access, desc, mv);
    mv.visitMethodInsn(INVOKESPECIAL, ownerSlashes, UNMANAGED_PREFIX + name, desc);
    if (!isVoid) {
      mv.visitInsn(Type.getReturnType(desc).getOpcode(IRETURN));
    } else {
      mv.visitLabel(end);
      mv.visitInsn(RETURN);
    }

    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private class RewriteSelfTypeCalls extends MethodVisitor implements Opcodes {

    private final String[] types;
    private final String   prefix;

    public RewriteSelfTypeCalls(MethodVisitor mv, String[] types, String prefix) {
      super(Opcodes.ASM4, mv);
      this.types = types;
      this.prefix = prefix;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ("<init>".equals(name)) {
        super.visitMethodInsn(opcode, owner, name, desc);
        return;
      }

      if (dontDupe.contains(name + desc)) {
        super.visitMethodInsn(opcode, owner, name, desc);
        return;
      }

      if (opcode != INVOKESTATIC) {
        boolean rewrite = false;
        for (String type : types) {
          if (type.equals(owner)) {
            rewrite = true;
            break;
          }
        }

        if (rewrite) {
          name = prefix + name;
        }
      }

      super.visitMethodInsn(opcode, owner, name, desc);
    }
  }

  private static class MulticastMethodVisitor extends MethodVisitor {

    private final MethodVisitor[] targets;

    MulticastMethodVisitor(MethodVisitor targets[]) {
      super(Opcodes.ASM4);
      this.targets = targets;
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      AnnotationVisitor rv = null;
      for (MethodVisitor target : targets) {
        rv = target.visitAnnotation(desc, visible);
      }
      return rv;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      AnnotationVisitor rv = null;
      for (MethodVisitor target : targets) {
        rv = target.visitAnnotationDefault();
      }
      return rv;
    }

    @Override
    public void visitAttribute(Attribute attr) {
      for (MethodVisitor target : targets) {
        target.visitAttribute(attr);
      }
    }

    @Override
    public void visitCode() {
      for (MethodVisitor target : targets) {
        target.visitCode();
      }
    }

    @Override
    public void visitEnd() {
      for (MethodVisitor target : targets) {
        target.visitEnd();
      }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      for (MethodVisitor target : targets) {
        target.visitFieldInsn(opcode, owner, name, desc);
      }
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
      for (MethodVisitor target : targets) {
        target.visitFrame(type, nLocal, local, nStack, stack);
      }
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      for (MethodVisitor target : targets) {
        target.visitIincInsn(var, increment);
      }
    }

    @Override
    public void visitInsn(int opcode) {
      for (MethodVisitor target : targets) {
        target.visitInsn(opcode);
      }
    }

    @Override
    public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
      AnnotationVisitor rv = null;
      for (MethodVisitor target : targets) {
        rv = target.visitInsnAnnotation(typeRef, typePath, desc, visible);
      }
      return rv;
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      for (MethodVisitor target : targets) {
        target.visitIntInsn(opcode, operand);
      }
    }

    @Override
    public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
      for (MethodVisitor target : targets) {
        target.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
      }
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      for (MethodVisitor target : targets) {
        target.visitJumpInsn(opcode, label);
      }
    }

    @Override
    public void visitLabel(Label label) {
      for (MethodVisitor target : targets) {
        target.visitLabel(label);
      }
    }

    @Override
    public void visitLdcInsn(Object cst) {
      for (MethodVisitor target : targets) {
        target.visitLdcInsn(cst);
      }
    }

    @Override
    public void visitLineNumber(int line, Label start) {
      for (MethodVisitor target : targets) {
        target.visitLineNumber(line, start);
      }
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      for (MethodVisitor target : targets) {
        target.visitLocalVariable(name, desc, signature, start, end, index);
      }
    }

    @Override
    public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end,
                                                          int[] index, String desc, boolean visible) {
      AnnotationVisitor rv = null;
      for (MethodVisitor target : targets) {
        rv = target.visitLocalVariableAnnotation(typeRef, typePath, start, end, index, desc, visible);
      }
      return rv;
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      for (MethodVisitor target : targets) {
        target.visitLookupSwitchInsn(dflt, keys, labels);
      }
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      for (MethodVisitor target : targets) {
        target.visitMaxs(maxStack, maxLocals);
      }
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
      for (MethodVisitor target : targets) {
        target.visitMethodInsn(opcode, owner, name, desc, itf);
      }
    }

    @Override
    public void visitMethodInsn(int arg0, String arg1, String arg2, String arg3) {
      for (MethodVisitor target : targets) {
        target.visitMethodInsn(arg0, arg1, arg2, arg3);
      }
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      for (MethodVisitor target : targets) {
        target.visitMultiANewArrayInsn(desc, dims);
      }
    }

    @Override
    public void visitParameter(String name, int access) {
      for (MethodVisitor target : targets) {
        target.visitParameter(name, access);
      }
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      AnnotationVisitor rv = null;
      for (MethodVisitor target : targets) {
        rv = target.visitParameterAnnotation(parameter, desc, visible);
      }
      return rv;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
      for (MethodVisitor target : targets) {
        target.visitTableSwitchInsn(min, max, dflt, labels);
      }
    }
  }
}
