/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.AppServerInfo;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import junit.framework.Test;

/**
 * Verify that when the system is configured to fall back to the classloader that
 * created the named root object, classes created by the app server VM
 * can be faulted on the vanilla VM and vice versa.
 */
public class RootBasedLoaderTest extends StandardLoaderTestBase {
  
  protected static class RootBasedLoaderTestSetup extends StandardLoaderTestSetup {

    public RootBasedLoaderTestSetup(Class testClass) {
      super(testClass);
    }
    
    /**
     * Add &lt;classloader-compatibility&gt; with named-root strategy.
     * That's what makes this a RootBasedLoader test.
     */
    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      super.configureTcConfig(tcConfigBuilder);
      String rootName = "sharedMap";
      tcConfigBuilder.setNamedRootLoaderStrategy(rootName);
    }
  }

  public RootBasedLoaderTest() {
    // DEV-1817
    if (appServerInfo().getId() == AppServerInfo.WEBSPHERE) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public static Test suite() {
    return new RootBasedLoaderTestSetup(RootBasedLoaderTest.class);
  }

  public void testRootBasedClassLoader() throws Exception {

    WebConversation conversation = new WebConversation();
    WebResponse response1 = request(server0, "cmd=" + StandardLoaderServlet.GET_CLASS_LOADER_NAME, conversation);
    String classLoaderName = response1.getText().trim();
    System.out.println("Class Loader Name: " + classLoaderName);

    WebConversation conversation2 = new WebConversation();
    WebResponse response2 = request(server0, "cmd=" + StandardLoaderServlet.PUT_INNER_INSTANCE, conversation2);
    assertEquals("OK", response2.getText().trim());

    WebConversation conversation4 = new WebConversation();
    WebResponse response4 = request(server0, "cmd=" + StandardLoaderServlet.PUT_STANDARD_LOADER_OBJECT_INSTANCE,
                                    conversation4);
    assertEquals("OK", response4.getText().trim());

    List vmArgs = new ArrayList();
    vmArgs.add("-D" + TEST_TYPE + "=" + ROOT_BASED_LOADER_TEST);
    int exitCode = spawnExtraL1(vmArgs);
    assertEquals(0, exitCode);

    WebConversation conversation3 = new WebConversation();
    WebResponse response3 = request(server0, "cmd=" + StandardLoaderServlet.CHECK_APP_INNER_INSTANCE, conversation3);
    assertEquals("OK", response3.getText().trim());

  }

}
