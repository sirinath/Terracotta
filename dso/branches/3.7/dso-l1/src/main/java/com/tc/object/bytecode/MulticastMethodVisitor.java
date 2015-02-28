/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.HashMap;
import java.util.Map;

/**
 * MethodVisitor that is able to delegate all the calls to an array of other
 * method visitors. Labels are properly created for each individual visitor
 * and a simple mapping is maintained to be able to retrieve which label
 * belongs to which visitor.
 */
public class MulticastMethodVisitor extends MethodVisitor {

  private final MethodVisitor[] visitors;
  
  private final Map labelsMapping = new HashMap();
  
  public MulticastMethodVisitor(MethodVisitor[] visitors) {
    super(Opcodes.ASM5);
    this.visitors = visitors;
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotation(desc, visible);
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  @Override
  public AnnotationVisitor visitAnnotationDefault() {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotationDefault();
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  @Override
  public void visitAttribute(Attribute attr) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitAttribute(attr);
    }
  }

  @Override
  public void visitCode() {
    for (MethodVisitor visitor : visitors) {
      visitor.visitCode();
    }
  }

  @Override
  public void visitEnd() {
    for (MethodVisitor visitor : visitors) {
      visitor.visitEnd();
    }
  }

  @Override
  public void visitFieldInsn(int opcode, String owner, String name, String desc) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitFieldInsn(opcode, owner, name, desc);
    }
  }

  @Override
  public void visitFrame(int type, int local, Object[] local2, int stack, Object[] stack2) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitFrame(type, local, local2, stack, stack2);
    }
  }

  @Override
  public void visitIincInsn(int var, int increment) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitIincInsn(var, increment);
    }
  }

  @Override
  public void visitInsn(int opcode) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitInsn(opcode);
    }
  }

  @Override
  public void visitIntInsn(int opcode, int operand) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitIntInsn(opcode, operand);
    }
  }

  @Override
  public void visitJumpInsn(int opcode, Label label) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitJumpInsn(opcode, getMappedLabel(label, i));
    }
  }

  @Override
  public void visitLabel(Label label) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLabel(getMappedLabel(label, i));
    }
  }

  @Override
  public void visitLdcInsn(Object cst) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitLdcInsn(cst);
    }
  }

  @Override
  public void visitLineNumber(int line, Label start) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLineNumber(line, getMappedLabel(start, i));
    }
  }

  @Override
  public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLocalVariable(name, desc, signature, getMappedLabel(start, i), getMappedLabel(end, i), index);
    }
  }

  @Override
  public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitLookupSwitchInsn(getMappedLabel(dflt, i), keys, getMappedLabels(labels, i));
    }
  }

  @Override
  public void visitMaxs(int maxStack, int maxLocals) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitMaxs(maxStack, maxLocals);
    }
  }

  @Override
  public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitMethodInsn(opcode, owner, name, desc, itf);
    }
  }

  @Override
  public void visitMultiANewArrayInsn(String desc, int dims) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitMultiANewArrayInsn(desc, dims);
    }
  }

  @Override
  public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitParameterAnnotation(parameter, desc, visible);
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  @Override
  public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitTableSwitchInsn(min, max, getMappedLabel(dflt, i), getMappedLabels(labels, i));
    }
  }

  @Override
  public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
    for (int i = 0; i < visitors.length; i++) {
      visitors[i].visitTryCatchBlock(getMappedLabel(start, i), getMappedLabel(end, i), getMappedLabel(handler, i), type);
    }
  }

  @Override
  public void visitTypeInsn(int opcode, String desc) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitTypeInsn(opcode, desc);
    }
  }

  @Override
  public void visitVarInsn(int opcode, int var) {
    for (MethodVisitor visitor : visitors) {
      visitor.visitVarInsn(opcode, var);
    }
  }
  
  private Label getMappedLabel(Label original, int visitorIndex) {
    if (null == original) return null;
    return getMappedLabels(original)[visitorIndex];
  }
  
  private Label[] getMappedLabels(Label[] originals, int visitorIndex) {
    if (null == originals) return null;
    
    Label[] result = new Label[originals.length];
    for (int i = 0; i < result.length; i++) {
      result[i] = getMappedLabel(originals[i], visitorIndex);
    }
    return result;
  }

  private Label[] getMappedLabels(Label original) {
    Label[] labels = (Label[])labelsMapping.get(original);
    if (null == labels) {
      labels = new Label[visitors.length];
      for (int i = 0; i < visitors.length; i++) {
        labels[i] = new Label();
      }
      labelsMapping.put(original, labels);
    }
    return labels;
  }
}