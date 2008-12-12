/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.server.appserver.unit;

import com.tc.test.server.util.TcConfigBuilder;

import junit.framework.Test;

public class UnlockedSessionObjectWithSLTest extends UnlockedSessionObjectTestBase {

  public static Test suite() {
    return new UnlockedSessionObjectWithSLTestSetup();
  }

  public void testSesionLocking() throws Exception {
    super.testSessionLocking();
  }

  @Override
  public boolean isSessionLockingTrue() {
    return true;
  }

  private static class UnlockedSessionObjectWithSLTestSetup extends UnlockedSessionObjectTestSetup {

    public UnlockedSessionObjectWithSLTestSetup() {
      super(UnlockedSessionObjectWithSLTest.class, CONTEXT);
    }

    protected void configureTcConfig(TcConfigBuilder tcConfigBuilder) {
      super.configureTcConfig(tcConfigBuilder);
      tcConfigBuilder.addWebApplicationWithSessionLocking(CONTEXT);
    }
  }

}
