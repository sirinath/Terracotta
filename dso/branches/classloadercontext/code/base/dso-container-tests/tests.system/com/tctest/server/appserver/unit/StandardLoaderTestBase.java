/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.deployment.AbstractOneServerDeploymentTest;
import com.tc.test.server.appserver.deployment.DeploymentBuilder;
import com.tc.test.server.appserver.deployment.JARBuilder;
import com.tc.test.server.appserver.deployment.WebApplicationServer;
import com.tc.test.server.util.TcConfigBuilder;
import com.tctest.externall1.StandardClasspathDummyClass;
import com.tctest.externall1.StandardLoaderApp;
import com.tctest.webapp.servlets.StandardLoaderServlet;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

public abstract class StandardLoaderTestBase extends AbstractOneServerDeploymentTest {

  // These strings may be shared by vanilla apps (e.g, StandardLoaderApp), servlets
  // (e.g., StandardLoaderServlet), and test code (e.g., RenameStandardLoaderTest).
  public static final String TEST_TYPE = "com.tc.testType";
  public static final String ROOT_BASED_LOADER_TEST = "fallbackLoaderTest";
  public static final String RENAME_LOADER_TEST = "renameLoaderTest";
  private static final String CONTEXT = "simple";

  /**
     * ***** test setup *********
     */
    protected static class StandardLoaderTestSetup extends OneServerTestSetup {
  
      public StandardLoaderTestSetup(Class testClass) {
        super(testClass, CONTEXT);
        setStart(false);
      }
  
      protected void configureWar(DeploymentBuilder builder) {
        builder.addServlet("StandardLoaderServlet", "/" + CONTEXT + "/*", StandardLoaderServlet.class, null, false);
      }
  
      protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
        String rootName = "sharedMap";
        String fieldName = StandardLoaderServlet.class.getName() + ".sharedMap";
        tcConfigBuilder.addRoot(fieldName, rootName);
  
        String methodExpression = "* " + StandardLoaderServlet.class.getName() + ".*(..)";
        tcConfigBuilder.addAutoLock(methodExpression, "write");
  
        tcConfigBuilder.addInstrumentedClass(StandardLoaderServlet.class.getName() + "$Inner", false);
        tcConfigBuilder.addInstrumentedClass(StandardClasspathDummyClass.class.getName(), false);
  
        fieldName = StandardLoaderApp.class.getName() + ".sharedMap";
        tcConfigBuilder.addRoot(fieldName, rootName);
        methodExpression = "* " + StandardLoaderApp.class.getName() + ".*(..)";
        tcConfigBuilder.addAutoLock(methodExpression, "write");
  
        tcConfigBuilder.addInstrumentedClass(StandardLoaderApp.class.getName() + "$AppInnerClass", false);
     }
    }

  private File buildTestJar() throws Exception {
    File jarFile = getTempFile("myclass.jar");
    JARBuilder jar = new JARBuilder(jarFile.getName(), jarFile.getParentFile());
    jar.addResource("/com/tctest/externall1", "StandardClasspathDummyClass.class", "com/tctest/externall1");
    jar.finish();
    return jarFile;
  }

  protected void setUp() throws Exception {
    super.setUp();
    TestConfigObject.getInstance().addToAppServerClassPath(buildTestJar().getAbsolutePath());
    server0.start();
  }

  protected int spawnExtraL1(List vmArgs) throws Exception {
    
    // if true, debug spawned extra L1 on port 8001, and also echo its output to JUnit test console
    boolean debug = Boolean.getBoolean("com.tc.test.server.appserver.deployment.GenericServer.ENABLE_DEBUGGER");
    
    if (debug) {
      vmArgs.add("-Xdebug");
      vmArgs.add("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8001");
    }
  
    ExtraL1ProcessControl client = 
      new ExtraL1ProcessControl(getServerManager().getServerTcConfig().getDsoHost(),
                                getServerManager().getServerTcConfig().getDsoPort(),
                                StandardLoaderApp.class, server0.getTcConfigFile()
                                .getAbsolutePath(), 
                                new String[] {}, 
                                server0.getWorkingDirectory(), 
                                vmArgs, 
                                false);
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    client.writeOutputTo(outputStream);
    client.start();
  
    final int exitCode = client.waitFor();
    if (debug || exitCode != 0) {
      System.out.println("ExtraL1 client reported exit code " + exitCode + " - full stdout was:");
      System.out.println(outputStream.toString());
      System.out.println("==================================");
    }
  
    BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(outputStream.toByteArray())));
    assertEquals(outputStream.toString(), "OK", getLastLine(br).trim());
  
    return exitCode;
  }

  private String getLastLine(BufferedReader br) throws IOException {
    String line = br.readLine();
    String lastLine = line;
    while ((line = br.readLine()) != null)
      lastLine = line;
    return lastLine;
  }

  protected WebResponse request(WebApplicationServer server, String params, WebConversation con) throws Exception {
    return server.ping("/" + CONTEXT + "/" + CONTEXT + "?" + params, con);
  }

  public StandardLoaderTestBase() {
    super();
  }

}
