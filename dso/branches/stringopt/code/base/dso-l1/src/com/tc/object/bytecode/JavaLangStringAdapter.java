/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassAdapter;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodAdapter;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.object.bytecode.hook.impl.JavaLangArrayHelpers;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;
import com.tc.util.runtime.VmVersion;

public class JavaLangStringAdapter extends ClassAdapter implements Opcodes {

  private static final String GET_VALUE_METHOD      = ByteCodeUtil.fieldGetterMethod("value");
  private static final String INTERN_FIELD_NAME     = ByteCodeUtil.TC_FIELD_PREFIX + "interned";
  private static final String COMPRESSED_FIELD_NAME = ByteCodeUtil.TC_FIELD_PREFIX + "compressed";

  private final VmVersion     vmVersion;
  private final boolean       portableStringBuffer;
  private final boolean       isAzul;

  public JavaLangStringAdapter(ClassVisitor cv, VmVersion vmVersion, boolean portableStringBuffer, boolean isAzul) {
    super(cv);
    this.vmVersion = vmVersion;
    this.portableStringBuffer = portableStringBuffer;
    this.isAzul = isAzul;
  }

  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    interfaces = ByteCodeUtil.addInterfaces(interfaces, new String[] { "com/tc/object/bytecode/JavaLangStringIntern" });
    super.visit(version, access, name, signature, superName, interfaces);
  }

  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);

    if ("getBytes".equals(name) && "(II[BI)V".equals(desc)) {
      mv = rewriteGetBytes(mv);
    } else if ("<init>".equals(name) && "(Ljava/lang/StringBuffer;)V".equals(desc)) {
      if (vmVersion.isJDK14() && portableStringBuffer) {
        mv = rewriteStringBufferConstructor(mv);
      }
    } else if ("getChars".equals(name) && "(II[CI)V".equals(desc)) {
      // make formatter sane
      mv = new GetCharsAdapter(mv);
    } else if ("getChars".equals(name) && "([CI)V".equals(desc)) {
      // This method is in the 1.5 Sun impl of String
      mv = new GetCharsAdapter(mv);
    } else {
      mv = new RewriteGetCharsCallsAdapter(mv);
    }

    if (mv != null) {
      mv = new DecompressCharsAdapter(mv);
    }

    return mv;
  }

  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    if ("value".equals(name)) {
      // Remove final modifier and add volatile on char[] value
      return super.visitField(ACC_PRIVATE + ACC_VOLATILE, "value", "[C", null, null);
    } else {
      return super.visitField(access, name, desc, signature, value);
    }
  }

  public void visitEnd() {
    addCompressionField();
    addCompressedConstructor();

    addGetValueMethod();
    addFastGetChars();

    addStringInternTCNature();
    super.visitEnd();
  }

  private void addStringInternTCNature() {
    // private boolean $__tc_interned;
    super.visitField(ACC_PRIVATE, INTERN_FIELD_NAME, "Z", null, null);

    // public String __tc_intern(String) - TC version of String.intern()
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "intern", "()Ljava/lang/String;",
                                         null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "intern", "()Ljava/lang/String;");
    mv.visitVarInsn(ASTORE, 1);
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ICONST_1);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitInsn(ARETURN);
    mv.visitMaxs(2, 2);
    mv.visitEnd();

    // public Boolean __tc_isInterned() - implementation of JavaLangStringIntern Interface
    mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.TC_METHOD_PREFIX + "isInterned", "()Z", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    mv.visitInsn(IRETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private void addCompressionField() {
    // private volatile boolean $__tc_compressed = false;
    super.visitField(ACC_PRIVATE + ACC_VOLATILE + ACC_TRANSIENT, COMPRESSED_FIELD_NAME, "Z", null, null);
  }

  private void addCompressedConstructor() {
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, "<init>", "(Z[CII)V", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    Label l1 = new Label();
    mv.visitLabel(l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hash", "I");
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "hash", "I");
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 3);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "count", "I");
    Label l5 = new Label();
    mv.visitLabel(l5);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", COMPRESSED_FIELD_NAME, "Z");
    Label l6 = new Label();
    mv.visitLabel(l6);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", INTERN_FIELD_NAME, "Z");
    Label l7 = new Label();
    mv.visitLabel(l7);
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitInsn(ICONST_0);
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "offset", "I");
    }    
    Label l8 = new Label();
    mv.visitLabel(l8);
    mv.visitInsn(RETURN);
    Label l9 = new Label();
    mv.visitLabel(l9);
    mv.visitLocalVariable("this", "Ljava/lang/String;", null, l0, l9, 0);
    mv.visitLocalVariable("compressed", "Z", null, l0, l9, 1);
    mv.visitLocalVariable("compressedData", "[C", null, l0, l9, 2);
    mv.visitLocalVariable("uncompressedLength", "I", null, l0, l9, 3);
    mv.visitLocalVariable("hashCode", "I", null, l0, l9, 4);
    mv.visitMaxs(2, 5);
    mv.visitEnd();
  }

  private void addGetValueMethod() {
    // ACC_PRIVATE?
    MethodVisitor mv = super.visitMethod(ACC_PUBLIC, ByteCodeUtil.fieldGetterMethod("value"), "()[C", null, null);
    //mv = cw.visitMethod(ACC_PRIVATE, "__tc_getvalue", "()[C", null, null);
    mv.visitCode();
    Label l0 = new Label();
    mv.visitLabel(l0);
    mv.visitLineNumber(39, l0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "$__tc_compressed", "Z");
    Label l1 = new Label();
    mv.visitJumpInsn(IFEQ, l1);
    Label l2 = new Label();
    mv.visitLabel(l2);
    mv.visitLineNumber(41, l2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
    mv.visitMethodInsn(INVOKESTATIC, "com/tc/object/StringCompressionUtil", "decompressCompressedChars", "([CI)[C");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    Label l3 = new Label();
    mv.visitLabel(l3);
    mv.visitLineNumber(42, l3);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitInsn(ICONST_0);
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "$__tc_compressed", "Z");
    mv.visitLabel(l1);
    mv.visitLineNumber(44, l1);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitInsn(ARETURN);
    Label l4 = new Label();
    mv.visitLabel(l4);
    mv.visitLocalVariable("this", "Ljava/lang/String;", null, l0, l4, 0);
    mv.visitMaxs(3, 1);
    mv.visitEnd();
  }

  private void addFastGetChars() {
    // Called by the unmanaged paths of StringBuffer, StringBuilder, etc. Also called it strategic places where the
    // target char[] is known (or assumed) to be non-shared
    MethodVisitor mv = super.visitMethod(ACC_SYNTHETIC | ACC_PUBLIC, "getCharsFast", "(II[CI)V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    }
    mv.visitVarInsn(ILOAD, 1);
    if (!isAzul) {
      mv.visitInsn(IADD);
    }
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ILOAD, 1);
    mv.visitInsn(ISUB);
    mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();

    // Called from (Abstract)StringBuilder.insert|replace()
    mv = super.visitMethod(ACC_SYNTHETIC, "getCharsFast", "([CI)V", null, null);
    mv.visitCode();
    if (isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
      // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
      mv.visitInsn(ICONST_0);
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitVarInsn(ALOAD, 0);
      // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
      mv.visitInsn(ARRAYLENGTH);
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      mv.visitInsn(RETURN);
    } else {
      mv.visitVarInsn(ALOAD, 0);
      // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
      mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
      mv.visitVarInsn(ALOAD, 1);
      mv.visitVarInsn(ILOAD, 2);
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
      mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "arraycopy", "(Ljava/lang/Object;ILjava/lang/Object;II)V");
      mv.visitInsn(RETURN);
    }
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  private MethodVisitor rewriteStringBufferConstructor(MethodVisitor mv) {
    // move the sync into StringBuffer.toString() where it belongs
    Assert.assertTrue(Vm.isJDK14());
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    mv.visitVarInsn(ALOAD, 1);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuffer", "toString", "()Ljava/lang/String;");
    mv.visitVarInsn(ASTORE, 2);
    mv.visitVarInsn(ALOAD, 0);
    mv.visitVarInsn(ALOAD, 2);
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitFieldInsn(PUTFIELD, "java/lang/String", "value", "[C");
    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "count", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitVarInsn(ALOAD, 2);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
      mv.visitFieldInsn(PUTFIELD, "java/lang/String", "offset", "I");
    }
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    return null;
  }

  private MethodVisitor rewriteGetBytes(MethodVisitor mv) {
    mv.visitCode();
    mv.visitVarInsn(ILOAD, 1);
    mv.visitVarInsn(ILOAD, 2);
    mv.visitVarInsn(ALOAD, 3);
    mv.visitVarInsn(ILOAD, 4);

    if (!isAzul) {
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "count", "I");
      mv.visitVarInsn(ALOAD, 0);
      mv.visitFieldInsn(GETFIELD, "java/lang/String", "offset", "I");
    }

    mv.visitVarInsn(ALOAD, 0);
    // mv.visitFieldInsn(GETFIELD, "java/lang/String", "value", "[C");
    mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, "()[C");
    mv.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "javaLangStringGetBytes", isAzul ? "(II[BI[C)V"
        : "(II[BIII[C)V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
    return null;
  }

  private static class RewriteGetCharsCallsAdapter extends MethodAdapter {

    public RewriteGetCharsCallsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((INVOKEVIRTUAL == opcode) && ("java/lang/String".equals(owner) && "getChars".equals(name))) {
        super.visitMethodInsn(opcode, owner, "getCharsFast", desc);
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

  private static class GetCharsAdapter extends MethodAdapter {

    public GetCharsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      if ((opcode == INVOKESTATIC) && "java/lang/System".equals(owner) && "arraycopy".equals(name)) {
        super.visitMethodInsn(INVOKESTATIC, JavaLangArrayHelpers.CLASS, "charArrayCopy", "([CI[CII)V");
      } else {
        super.visitMethodInsn(opcode, owner, name, desc);
      }
    }

  }

  private static class DecompressCharsAdapter extends MethodAdapter {
    public DecompressCharsAdapter(MethodVisitor mv) {
      super(mv);
    }

    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      if (opcode == GETFIELD && "java/lang/String".equals(owner) && "value".equals(name)) {
        String gDesc = "()" + desc;
        super.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", GET_VALUE_METHOD, gDesc);
      } else {
        super.visitFieldInsn(opcode, owner, name, desc);
      }
    }
  }

}
