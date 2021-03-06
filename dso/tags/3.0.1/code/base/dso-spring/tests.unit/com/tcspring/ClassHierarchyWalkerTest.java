/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.jmock.Mock;
import org.jmock.MockObjectTestCase;

import com.tc.asm.ClassWriter;
import com.tc.asm.FieldVisitor;
import com.tc.asm.MethodVisitor;
import com.tc.asm.Opcodes;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.object.bytecode.hook.DSOContext;

public class ClassHierarchyWalkerTest extends MockObjectTestCase implements Opcodes {

  private DSOContext           dsoContext;
  private Mock                 dsoContextMock;
  private ClassHierarchyWalker walker;

  protected void setUp() {
    dsoContextMock = mock(DSOContext.class);
    dsoContext = (DSOContext) dsoContextMock.proxy();

    walker = new ClassHierarchyWalker("id", dsoContext);
  }

  public void testWalkThroughClassHierarchy() {
    String[] classNames = {
        "com.tcspring.beans.SimpleBean", 
        "com.tcspring.beans.SimpleBean1", 
        "com.tcspring.beans.SimpleBean2",
        "com.tcspring.beans.SimpleParentBean", 
        "com.tcspring.beans.SimplePropertyBean" };

    for (int i = 0; i < classNames.length; i++) {
      String className = classNames[i];
      dsoContextMock.expects(once()).method("addInclude").with(eq(className), ANYTHING, eq("* " + className + ".*(..)"), ANYTHING);
    }

    walker.walkClass(classNames[0], getClass().getClassLoader());
  }
  
  public void testWalkWithGenerics() throws Exception {
    String[] classNames = { 
        "com.tcspring.beans.SimpleBeanWithGenerics", 
        "com.tcspring.beans.SimpleBean1",
        "com.tcspring.beans.SimpleBean2", 
        "java.util.ArrayList", 
        "java.util.AbstractList",
        "java.util.AbstractCollection", 
        "com.tcspring.beans.SimplePropertyBean", 
        "com.tcspring.beans.SimpleBean",
        "com.tcspring.beans.SimpleParentBean" };

    for (int i = 0; i < classNames.length; i++) {
      String className = classNames[i];
      dsoContextMock.expects(once()).method("addInclude").with(eq(className), ANYTHING, eq("* " + className + ".*(..)"), ANYTHING);
    }
    
    /* 
     * constructing ClassInfo from the generated bytecode to retain 1.4 compatibility
     * 
     * public class SimpleBeanWithGenerics {
     *   public Map<SimpleBean1, SimpleBean2[]> map1;
     *   public Map<SimpleBean1, ArrayList<SimplePropertyBean>> map2;
     * }
     */
    ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);

    cw.visit(V1_5, ACC_PUBLIC + ACC_SUPER, "com/tcspring/beans/SimpleBeanWithGenerics", null, "java/lang/Object", null);

    cw.visitSource("SimpleBeanWithGenerics.java", null);

    FieldVisitor fv1 = cw.visitField(ACC_PUBLIC, "map1", "Ljava/util/Map;",  // 
         "Ljava/util/Map<Lcom/tcspring/beans/SimpleBean1;[Lcom/tcspring/beans/SimpleBean2;>;", null);
    fv1.visitEnd();

    FieldVisitor fv2 = cw.visitField(ACC_PUBLIC,"map2","Ljava/util/Map;",  //
        "Ljava/util/Map<Lcom/tcspring/beans/SimpleBean1;Ljava/util/ArrayList<Lcom/tcspring/beans/SimplePropertyBean;>;>;", null);
    fv2.visitEnd();

    MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
    mv.visitCode();
    mv.visitVarInsn(ALOAD, 0);
    mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
    mv.visitInsn(RETURN);
    mv.visitMaxs(1, 1);
    mv.visitEnd();

    cw.visitEnd();

    byte[] bytecode = cw.toByteArray();

    ClassInfo classInfo = AsmClassInfo.newClassInfo(bytecode, getClass().getClassLoader()); 
    
    walker.walkClass(classInfo);
  }
}

