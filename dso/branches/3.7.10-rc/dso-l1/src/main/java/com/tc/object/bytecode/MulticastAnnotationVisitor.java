/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Opcodes;

/**
 * AnnotationVisitor that is able to delegate all the calls to an array of
 * other annotation visitors.
 */
public class MulticastAnnotationVisitor extends AnnotationVisitor {
  
  private final AnnotationVisitor[] visitors;
  
  public MulticastAnnotationVisitor(AnnotationVisitor[] visitors) {
    super(Opcodes.ASM5);
    this.visitors = visitors;
  }

  @Override
  public void visit(String name, Object value) {
    for (AnnotationVisitor visitor : visitors) {
      visitor.visit(name, value);
    }
  }

  @Override
  public AnnotationVisitor visitAnnotation(String name, String desc) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitAnnotation(name, desc);
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  @Override
  public AnnotationVisitor visitArray(String name) {
    AnnotationVisitor[] annotationVisitors = new AnnotationVisitor[visitors.length];
    for (int i = 0; i < visitors.length; i++) {
      annotationVisitors[i] = visitors[i].visitArray(name);
    }
    
    return new MulticastAnnotationVisitor(annotationVisitors);
  }

  @Override
  public void visitEnd() {
    for (AnnotationVisitor visitor : visitors) {
      visitor.visitEnd();
    }
  }

  @Override
  public void visitEnum(String name, String desc, String value) {
    for (AnnotationVisitor visitor : visitors) {
      visitor.visitEnum(name, desc, value);
    }
  }
}