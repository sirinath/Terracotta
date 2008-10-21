/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import com.tc.aspectwerkz.definition.deployer.StandardAspectModuleDeployer;
import com.tc.aspectwerkz.reflect.ClassInfo;
import com.tc.aspectwerkz.reflect.impl.asm.AsmClassInfo;
import com.tc.aspectwerkz.transform.InstrumentationContext;
import com.tc.object.bytecode.hook.impl.DefaultWeavingStrategy;
import com.tc.object.tools.BootJarTool;

import junit.framework.TestCase;

public class DEV1791Test extends TestCase {
  

  private ClassLoader classLoader;

  protected void setUp() throws Exception {
    super.setUp();
    classLoader = getClass().getClassLoader();
    
    StandardAspectModuleDeployer.deploy(classLoader, "com.tc.object.config.SpringAspectModule");
  }
  
  public void testSpringDemoClassIsAdvisable() throws Exception {
//    final String className = "org.directwebremoting.faces.JsfCreator";
    final String className = "org.springframework.core.CollectionFactory";
    byte[] classBytes = BootJarTool.getBytesForClass(className, classLoader);
    final InstrumentationContext context = new InstrumentationContext(className, classBytes, classLoader);
    ClassInfo classInfo = AsmClassInfo.getClassInfo(className, context.getInitialBytecode(), classLoader);
    
    final boolean isAdvisable = DefaultWeavingStrategy.isAdvisable(classInfo, context.getDefinitions(), DefaultWeavingStrategy.getDefaultExpressionContexts(classInfo));
    assertFalse(isAdvisable);
  }

}
