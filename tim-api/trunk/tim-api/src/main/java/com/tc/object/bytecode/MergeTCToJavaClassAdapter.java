/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.asm.ClassVisitor;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.asm.commons.AdviceAdapter;
import com.tc.asm.tree.ClassNode;
import com.tc.asm.tree.FieldNode;
import com.tc.asm.tree.InnerClassNode;
import com.tc.asm.tree.MethodNode;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MergeTCToJavaClassAdapter extends ChangeClassNameHierarchyAdapter implements Opcodes {
  private static final String                TC_INIT          = ByteCodeUtil.TC_METHOD_PREFIX + "$$_init_$$";

  private final List<String> jInnerClassNames = new ArrayList<String>();
  private final ClassNode    tcClassNode;
  private final String       jFullClassSlashes;
  private final String       tcFullClassSlashes;
  private final Map          instrumentedContext;
  private final Set          visitedMethods;
  private final String       methodPrefix;
  private final boolean      insertTCinit;
  private String             superName;
  private final ClassVisitor dsoAdapter;

  public MergeTCToJavaClassAdapter(ClassVisitor cv, ClassVisitor dsoAdapter, String jFullClassDots,
                                   String tcFullClassDots, ClassNode tcClassNode, Map instrumentedContext,
                                   String methodPrefix, boolean insertTCinit) {
    super(cv);
    this.insertTCinit = insertTCinit;

    if (insertTCinit) {
      createTCInit(tcClassNode);
    }

    this.tcClassNode = tcClassNode;

    this.jFullClassSlashes = jFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    this.tcFullClassSlashes = tcFullClassDots.replace(DOT_DELIMITER, SLASH_DELIMITER);

    addNewContextIfNotExist(tcFullClassDots, jFullClassDots, instrumentedContext);
    this.instrumentedContext = instrumentedContext;
    this.visitedMethods = new HashSet();
    this.dsoAdapter = dsoAdapter;
    this.methodPrefix = methodPrefix;
  }

  private static void createTCInit(ClassNode tcClassNode) {
    // For now, we only allow the "TC" class to contain 1 constructor at most.
    // This constructor body will be woven into all of the constructors present
    // in the original class

    List cstrs = new ArrayList();

    for (Iterator i = tcClassNode.methods.iterator(); i.hasNext();) {
      MethodNode mn = (MethodNode) i.next();

      if (isInitMethod(mn.name)) {
        cstrs.add(mn);
      }
    }

    if (cstrs.size() > 1) {
      //
      throw new IllegalArgumentException(tcClassNode.name + " contains " + cstrs.size()
                                         + " constructors, but only 1 is allowed");
    }

    MethodNode cstr = (MethodNode) cstrs.get(0);

    if (!cstr.exceptions.isEmpty()) {
      //
      throw new IllegalArgumentException("constructor in TC class not allowed to throw checked exceptions: "
                                         + cstr.exceptions);
    }

    MethodNode processed = new MethodNode();
    cstr.accept(new TransformConstructorAdapter(processed, cstr.access, cstr.name, cstr.desc));

    cstr.instructions = processed.instructions;
    cstr.access = ACC_PRIVATE | ACC_SYNTHETIC;
    cstr.name = TC_INIT;
  }

  public MergeTCToJavaClassAdapter(ClassVisitor cv, ClassVisitor dsoAdapter, String jFullClassDots,
                                   String tcFullClassDots, ClassNode tcClassNode, Map instrumentedContext) {
    this(cv, dsoAdapter, jFullClassDots, tcFullClassDots, tcClassNode, instrumentedContext,
         ByteCodeUtil.TC_METHOD_PREFIX, true);
  }

  @Override
  public void visit(int version, int access, String name, String signature, String superClassName, String[] interfaces) {
    this.superName = superClassName;

    List tcInterfaces = tcClassNode.interfaces;
    List jInterfaces = new ArrayList();
    for (int i = 0; i < interfaces.length; i++) {
      if (!tcInterfaces.contains(interfaces[i])) {
        jInterfaces.add(interfaces[i]);
      }
    }
    for (Iterator i = tcInterfaces.iterator(); i.hasNext();) {
      String intf = (String) i.next();
      jInterfaces.add(intf);
    }
    interfaces = new String[jInterfaces.size()];
    jInterfaces.toArray(interfaces);

    if (dsoAdapter == null) {
      super.visit(version, access, name, signature, superName, interfaces);
    } else {
      dsoAdapter.visit(version, access, name, signature, superClassName, interfaces);
    }
  }

  private String getNewName(String methodName) {
    if (isInitMethod(methodName)) { return methodName; }
    return this.methodPrefix + methodName;
  }

  @Override
  public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
    String methodDesc = name + desc;
    this.visitedMethods.add(methodDesc);

    if (!isInitMethod(name)) {
      for (MethodNode replacement : tcClassNode.methods) {
        if (0 == (replacement.access & ACC_ABSTRACT) && replacement.name.equals(name) && replacement.desc.equals(desc)) {
          final String remappedName = getNewName(name);
          boolean remappedIsDefined = false;
          for (MethodNode remapped : tcClassNode.methods) {
            if (0 == (remapped.access & ACC_ABSTRACT) && remapped.name.equals(remappedName)
                && remapped.desc.equals(desc)) {
              remappedIsDefined = true;
              break;
            }
          }
          if (remappedIsDefined) {
            return null;
          } else {
            replacement.signature = signature;
            return super.visitMethod(ACC_PRIVATE, remappedName, desc, signature, exceptions);
          }
        }
      }
    }

    MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
    if (LogicalClassSerializationAdapter.WRITE_OBJECT_SIGNATURE.equals(methodDesc)
        || LogicalClassSerializationAdapter.READ_OBJECT_SIGNATURE.equals(methodDesc)) { //
      return new LogicalClassSerializationAdapter.LogicalClassSerializationMethodAdapter(mv, jFullClassSlashes);
    }

    if (insertTCinit && isInitMethod(name)) {
      mv = new AddTCInitCallAdapter(jFullClassSlashes, mv, access, name, desc);
    }

    return mv;
  }

  @Override
  public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
    List tcFields = tcClassNode.fields;
    for (Iterator i = tcFields.iterator(); i.hasNext();) {
      FieldNode fieldNode = (FieldNode) i.next();
      if (name.equals(fieldNode.name) && desc.equals(fieldNode.desc)) {
        i.remove();
        break;
      }
    }

    // hack for now
    if (("java/util/LinkedHashMap".equals(jFullClassSlashes) && "accessOrder".equals(name))
        || ("java/util/concurrent/locks/ReentrantReadWriteLock".equals(jFullClassSlashes) && ("sync".equals(name)
                                                                                              || "readerLock"
                                                                                                  .equals(name) || "writerLock"
            .equals(name)))) {
      access = ~Modifier.FINAL & access;
    }

    return super.visitField(access, name, desc, signature, value);
  }

  @Override
  public void visitInnerClass(String name, String outerName, String innerName, int access) {
    jInnerClassNames.add(name);
    super.visitInnerClass(name, outerName, innerName, access);
  }
  
  @Override
  public void visitEnd() {
    addTCFields();
    addTCMethods();
    addTCInnerClasses();
    super.visitEnd();
  }

  private void addTCMethods() {
    List tcMethods = tcClassNode.methods;
    for (Iterator i = tcMethods.iterator(); i.hasNext();) {
      MethodNode mNode = (MethodNode) i.next();
      if (((mNode.access & ACC_ABSTRACT) != 0)
          || (isInitMethod(mNode.name) && (visitedMethods.contains(mNode.name + mNode.desc)))) {
        continue;
      }
      mNode.accept(new TCSuperClassAdapter(cv));
    }
    LogicalClassSerializationAdapter.addCheckSerializationOverrideMethod(cv, false);
  }

  private static boolean isInitMethod(String methodName) {
    return "<init>".equals(methodName);
  }

  private void addTCFields() {
    List tcFields = tcClassNode.fields;
    for (Iterator i = tcFields.iterator(); i.hasNext();) {
      FieldNode fNode = (FieldNode) i.next();
      fNode.accept(cv);
    }
  }

  private void addTCInnerClasses() {
    List tcInnerClasses = tcClassNode.innerClasses;
    for (Iterator i = tcInnerClasses.iterator(); i.hasNext();) {
      InnerClassNode innerClass = (InnerClassNode) i.next();
      if (!tcInnerClassExistInJavaClass(innerClass)) {
        innerClass.accept(new TCSuperClassAdapter(cv));
      }
    }
  }

  private boolean tcInnerClassExistInJavaClass(InnerClassNode tcInnerClass) {
    return jInnerClassNames.contains(replaceClassName(tcInnerClass.name));
  }

  private String replaceClassName(String classNameDots) {
    return replaceClassName(classNameDots, tcFullClassSlashes, jFullClassSlashes);
  }

  private String replaceClassName(String classNameDots, String srcClassNameDots, String targetClassNameDots) {
    if (classNameDots == null || classNameDots.length() == 0) { return classNameDots; }

    classNameDots = classNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    srcClassNameDots = srcClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);
    targetClassNameDots = targetClassNameDots.replace(DOT_DELIMITER, SLASH_DELIMITER);

    int index = classNameDots.indexOf(srcClassNameDots);
    if (index == -1) { return classNameDots; }

    StringBuffer newClassName = new StringBuffer();
    while (index != -1) {
      if (index > 0) {
        newClassName.append(classNameDots.substring(0, index));
      }
      newClassName.append(targetClassNameDots);
      classNameDots = classNameDots.substring(index + srcClassNameDots.length());
      index = classNameDots.indexOf(srcClassNameDots);
    }
    newClassName.append(classNameDots);
    return newClassName.toString();
  }

  private class TCSuperClassAdapter extends ClassVisitor implements Opcodes {
    public TCSuperClassAdapter(ClassVisitor cv) {
      super(Opcodes.ASM4, cv);
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superClassName, String[] interfaces) {
      name = replaceClassName(name);
      superClassName = replaceClassName(superClassName);
      super.visit(version, access & ~ACC_ABSTRACT, name, signature, superClassName, interfaces);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
      desc = replaceClassName(desc);
      signature = replaceClassName(signature);
      if (dsoAdapter == null) {
        return new TCSuperMethodAdapter(super.visitMethod(access, name, desc, signature, exceptions));
      } else {
	  return new TCSuperMethodAdapter(((TransparencyClassAdapterHack) dsoAdapter).basicVisitMethodHack(access, name, desc, signature, exceptions));
      }
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
      super.visitInnerClass(replaceClassName(name), replaceClassName(outerName), replaceClassName(innerName), access);
    }
    
    private class TCSuperMethodAdapter extends MethodVisitor implements Opcodes {
      public TCSuperMethodAdapter(MethodVisitor mv) {
        super(Opcodes.ASM4, mv);
      }

      @Override
      public void visitMethodInsn(int opcode, String owner, String name, String desc) {
        int index = owner.indexOf(tcFullClassSlashes);
        if (opcode == INVOKESPECIAL && index == -1) {
          if (!visitedMethods.contains(name + desc) && !isInitMethod(name)) {
            owner = superName;
          } else {
            ChangeContext context = (ChangeContext) instrumentedContext.get(owner);
            if (context != null) {
              owner = context.convertedClassNameSlashes;
            }
            name = getNewName(name);
          }
          super.visitMethodInsn(opcode, owner, name, desc);
        } else {
          owner = replaceClassName(owner);
          desc = replaceClassName(desc);
          super.visitMethodInsn(opcode, owner, name, desc);
        }
      }

      @Override
      public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        owner = replaceClassName(owner);
        desc = replaceClassName(desc);
        super.visitFieldInsn(opcode, owner, name, desc);
      }

      @Override
      public void visitTypeInsn(int opcode, String desc) {
        ChangeContext context = (ChangeContext) instrumentedContext.get(desc);
        if (context != null) {
          desc = context.convertedClassNameSlashes;
        } else {
          desc = replaceClassName(desc);
        }
        super.visitTypeInsn(opcode, desc);
      }

    }
  }

  private static class TransformConstructorAdapter extends AdviceAdapter {
    private final MethodNode target;

    public TransformConstructorAdapter(MethodNode mv, int access, String name, String desc) {
      super(Opcodes.ASM4, mv, access, name, desc);
      this.target = mv;
    }

    @Override
    protected void onMethodEnter() {
      target.instructions.clear();
    }

    @Override
    protected void onMethodExit(int opcode) {
      //
    }
  }

  private static class AddTCInitCallAdapter extends AdviceAdapter implements Opcodes {

    private final String owner;

    public AddTCInitCallAdapter(String owner, MethodVisitor mv, int access, String name, String desc) {
      super(Opcodes.ASM4, mv, access, name, desc);
      this.owner = owner;
    }

    @Override
    protected void onMethodEnter() {
      //
    }

    @Override
    protected void onMethodExit(int opcode) {
      if (RETURN == opcode) {
        super.visitVarInsn(ALOAD, 0);
        super.visitMethodInsn(INVOKESPECIAL, owner, TC_INIT, "()V");
      }
    }

  }

}
