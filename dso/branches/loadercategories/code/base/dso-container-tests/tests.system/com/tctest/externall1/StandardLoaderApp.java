/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.externall1;

import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.tools.BootJarTool;
import com.tc.util.Assert;
import com.tctest.server.appserver.unit.StandardLoaderTestBase;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.util.HashMap;
import java.util.Map;

public class StandardLoaderApp {

  private final Map sharedMap = new HashMap();

  public static void main(String[] args) {
    int rc = 0;
    
    try {
      String testType = System.getProperty(StandardLoaderTestBase.TEST_TYPE);
      
      // Tests in this block are only valid for RENAME_LOADER_TEST
      if (testType != null && StandardLoaderTestBase.RENAME_LOADER_TEST.equals(testType)) {
        checkStandardLoaderName();
        checkSetLoaderName();
      }

      final StandardLoaderApp app = new StandardLoaderApp();

      rc = app.doTest();

      if (rc == 0) {
        // Put an object into the map that is created with our classloader.
        // This object will eventually be faulted back into the servlet.
        synchronized (app.sharedMap) {
          Object o = new AppInnerClass();
          NamedClassLoader loader = (NamedClassLoader)o.getClass().getClassLoader();
          String name = loader.__tc_getClassLoaderName();
          System.out.println("Adding " + o.getClass().getName() + " from classloader " + name);
          app.sharedMap.put("2", o);
        }

        // Report success to the servlet
        System.out.println("OK");
      } else {
        // Report failure
        System.out.println("FAILED: " + rc);
      }

    } catch (Exception e) {
      rc = 1;
      e.printStackTrace();
    }
    
    System.exit(rc);
  }

  private static void checkStandardLoaderName() {
    NamedClassLoader loader = (NamedClassLoader) ClassLoader.getSystemClassLoader();
    String expectedLoaderName = System.getProperty(BootJarTool.SYSTEM_CLASSLOADER_NAME_PROPERTY);
    Assert.assertNotNull("Expected Sytem class loader name", expectedLoaderName);
    Assert.assertEquals(expectedLoaderName, loader.__tc_getClassLoaderName());
  }

  private static void checkSetLoaderName() {
    try {
      NamedClassLoader loader = (NamedClassLoader) ClassLoader.getSystemClassLoader();
      loader.__tc_setClassLoaderName("someName");
      Assert.fail("__tc_setClassLoaderName() should throw Assertion error.");
    } catch (AssertionError e) {
      // ok
    }
  }

  private int doTest() {
    synchronized (sharedMap) {
      Object obj = sharedMap.get("1");
      if (!(obj instanceof StandardLoaderServlet.Inner)) {
        return 2;
      }
      // assert that the object's class loader is the system class loader (with a different name)
      Assert.assertEquals(ClassLoader.getSystemClassLoader(), obj.getClass().getClassLoader());

      obj = sharedMap.get("3");
      if (!(obj instanceof StandardClasspathDummyClass)) {
        return 3;
      }

      // assert that the object's class loader is the system class loader
      Assert.assertEquals(ClassLoader.getSystemClassLoader(), obj.getClass().getClassLoader());
    }
    return 0;
  }

  public static class AppInnerClass {
    //
  }
}
