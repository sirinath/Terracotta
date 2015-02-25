/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.tools;

import com.tc.asm.AnnotationVisitor;
import com.tc.asm.Attribute;
import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.Label;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;

import java.util.Map;
import java.util.Set;

public class BootJarClassDependencyVisitor extends ClassVisitor {

  private final Set bootJarClassNames;
  private final Map offendingClasses;
  private String currentClassName = "";
//  private String currentMethodName = "";
//  private int currentLineNumber = 0;

  private static final String classSlashNameToDotName(final String name) {
    return name.replace('/', '.');
  }

  private final boolean check(final String name, String desc) {
    if (name.startsWith("com/tc/") || name.startsWith("com/tcclient/")) {
      boolean exists = bootJarClassNames.contains(BootJarClassDependencyVisitor.classSlashNameToDotName(name));
      if (!exists) {
        this.offendingClasses.put(classSlashNameToDotName(name), desc + " from " + this.currentClassName);
      }
      return exists;
    } else {
      return true;
    }
  }

  public BootJarClassDependencyVisitor(Set bootJarClassNames, Map offendingClasses) {
    super(Opcodes.ASM4);
    this.bootJarClassNames = bootJarClassNames;
    this.offendingClasses  = offendingClasses;
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
    this.currentClassName = BootJarClassDependencyVisitor.classSlashNameToDotName(name);
    
    // check the referenced class
    check(name, "reference to the class itself");

    // check it's superclass
    check(superName, "reference to 'extend' class declaration");
    
    // check it's interfaces
    for (String interface1 : interfaces) {
      check(interface1, "reference to 'implements' interface declaration");
    }
  }

  @Override
  public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
    if (visible) {
      check(desc, "reference to class annotation");
    }
    return null;
  }

  @Override
  public void visitAttribute(Attribute attr) {
    check(attr.type, "reference to a non-standard attribute of class '" + this.currentClassName + "'");
  }

  @Override
  public void visitEnd() {
    // nothing to do here
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    check(desc, "reference to a declared class field");
    return null;
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    check(name, "reference to inner-class");
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
//    this.currentMethodName = name;
    if (exceptions != null) {
      for (String exception : exceptions) {
        check(exception, "reference to a declared exception");
      }
    }
    MethodVisitor mv = new BootJarClassDependencyMethodVisitor();
    return mv;
  }

  @Override
  public void visitOuterClass(String owner, String name, String desc) {
    check(owner, "reference to outer-class");
  }

  @Override
  public void visitSource(String source, String debug) {
    // nothing to do here
  }
    
  private class BootJarClassDependencyMethodVisitor extends MethodVisitor implements Opcodes {

    public BootJarClassDependencyMethodVisitor() {
      super(Opcodes.ASM4);
    }

    @Override
    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
      if (visible) {
        check(desc, "reference to method annotation for method");
      }
      return null;
    }

    @Override
    public AnnotationVisitor visitAnnotationDefault() {
      return null;
    }

    @Override
    public void visitAttribute(Attribute attr) {
      check(attr.type, "reference to a non-standard attrbute of method");
    }

    @Override
    public void visitCode() {
      // nothing to do here
    }

    @Override
    public void visitEnd() {
      // nothing to do here
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
      check(owner, "reference in field get or put");
    }

    @Override
    public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
      // nothing to do here
    }

    @Override
    public void visitIincInsn(int var, int increment) {
      // nothing to do here
    }

    @Override
    public void visitInsn(int opcode) {
      // nothing to do here
    }

    @Override
    public void visitIntInsn(int opcode, int operand) {
      // nothing to do here
    }

    @Override
    public void visitJumpInsn(int opcode, Label label) {
      // nothing to do here
    }

    @Override
    public void visitLabel(Label label) {
      // nothing to do here
    }

    @Override
    public void visitLdcInsn(Object cst) {
      // nothing to do here
    }

    @Override
    public void visitLineNumber(int line, Label start) {
//      currentLineNumber = line;
    }

    @Override
    public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
      check(desc, "reference in local variable declaration");
    }

    @Override
    public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
      // nothing to do here
    }

    @Override
    public void visitMaxs(int maxStack, int maxLocals) {
      // nothing to do here
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc) {
      check(owner, "reference in either virtual, interface, constructor, or static invocation");
    }

    @Override
    public void visitMultiANewArrayInsn(String desc, int dims) {
      check(desc, "reference in mutli-array type declaration");
    }

    @Override
    public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
      if (visible) {
        check(desc, "reference to method annotation");
      }
      return null;
    }

    @Override
    public void visitTableSwitchInsn(int min, int max, Label dflt, Label[] labels) {
      // nothing to do here
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
      if (type != null) {
        check(type, "reference in try-catch block");
      }
    }

    @Override
    public void visitTypeInsn(int opcode, String desc) {
      check(desc, "reference in type-cast, class instantiation, type-check, or type-array declaration");
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
      // nothing to do here
    }
  }
}
