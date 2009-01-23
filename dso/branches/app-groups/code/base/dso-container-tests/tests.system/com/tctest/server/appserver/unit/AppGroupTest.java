/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.test.server.appserver.deployment.AbstractTwoServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.externall1.StandardClasspathDummyClass;
import com.tctest.webapp.servlets.RootSharingServletA;
import com.tctest.webapp.servlets.RootSharingServletB;
import com.tctest.webapp.servlets.SharedCache;

import junit.framework.Test;

public class AppGroupTest extends AbstractTwoServerDeploymentTest {
  private static final String   CONFIG_FILE_FOR_TEST = "/tc-config-files/nosession-tc-config.xml";
  protected static final String CONTEXT0             = "contexta";
  protected static final String CONTEXT1             = "contextb";
  protected static final String SERVLET0             = "RootSharingServletA";
  protected static final String SERVLET1             = "RootSharingServletB";
  private static final Class SERVLET0_CLASS          = RootSharingServletA.class;
  private static final Class SERVLET1_CLASS          = RootSharingServletB.class;

  public static Test suite() {
    return new AppGroupTestSetup();
  }

  public void testSharing() throws Exception {
    WebConversation conversation = new WebConversation();
    
    WebResponse response1 = request(0, "cmd=set", conversation);
    assertEquals("OK", response1.getText().trim());
    
    WebResponse response2 = request(1, "cmd=get", conversation);
    assertEquals("OK", response2.getText().trim());
  }
  
  private WebResponse request(int iServer, String params, WebConversation con) throws Exception {
    String context;
    String servlet;
    WebApplicationServer server;
    if (iServer == 0) {
      server = server0;
      context = CONTEXT0;
      servlet = SERVLET0;
    } else {
      server = server1;
      context = CONTEXT1;
      servlet = SERVLET1;
    }
    return server.ping("/" + context + "/" + servlet + "?" + params, con);
  }

  /** ****** test setup ********* */
  protected static class AppGroupTestSetup extends TwoContextTestSetup {

    public AppGroupTestSetup() {
      this(AppGroupTest.class, CONFIG_FILE_FOR_TEST, CONTEXT0, CONTEXT1);
    }

    public AppGroupTestSetup(Class testClass, String configFileForTest, String context0, String context1) {
      super(testClass, configFileForTest, context0, context1);
    }

    @Override
    protected void setUp() throws Exception {
      // To debug servlets:
      // System.setProperty("com.tc.test.server.appserver.deployment.GenericServer.ENABLE_DEBUGGER", "true");
      
      super.setUp();
    }

    @Override
    protected void configureWar(int server, DeploymentBuilder builder) {
      builder.addDirectoryOrJARContainingClass(StandardClasspathDummyClass.class);
      builder.addDirectoryOrJARContainingClass(SharedCache.class);
      if (server == 0) {
        builder.addServlet(SERVLET0, "/" + SERVLET0 + "/*", SERVLET0_CLASS, null, true);
      } else {
        builder.addServlet(SERVLET1, "/" + SERVLET1 + "/*", SERVLET1_CLASS, null, true);
      }
    }
    
    @Override
    protected void configureTcConfig(TcConfigBuilder clientConfig) {
      clientConfig.addInstrumentedClass(StandardClasspathDummyClass.class.getName());
      clientConfig.addInstrumentedClass(SharedCache.class.getName());
      clientConfig.addRoot(SharedCache.class.getName() + ".objects", "objects");
    }
  }

}


