/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test;

import junit.framework.TestCase;

public class AppServerInfoTest extends TestCase {

  public final void testParse() {
    AppServerInfo appServer = AppServerInfo.parse("tomcat-5.5.26");
    assertEquals("tomcat", appServer.getName());
    assertEquals("5", appServer.getMajor());
    assertEquals("5.26", appServer.getMinor());
    assertEquals(AppServerInfo.TOMCAT, appServer.getId());

    appServer = AppServerInfo.parse("weblogic-9.2.mp2");
    assertEquals("weblogic", appServer.getName());
    assertEquals("9", appServer.getMajor());
    assertEquals("2.mp2", appServer.getMinor());
    assertEquals(AppServerInfo.WEBLOGIC, appServer.getId());

    appServer = AppServerInfo.parse("jboss-eap-6.1.0");
    assertEquals("jboss-eap", appServer.getName());
    assertEquals("6", appServer.getMajor());
    assertEquals("1.0", appServer.getMinor());
    assertEquals(AppServerInfo.JBOSS, appServer.getId());

    IllegalArgumentException exception = null;
    try {
      AppServerInfo.parse("invalid-appserver-specification.1");
    }
    catch (IllegalArgumentException e) {
      exception = e;
    }
    assertNotNull(exception);
  }

  public final void testToString() {
    String nameAndVersion = "jboss-eap-6.1.0";
    assertEquals(nameAndVersion, AppServerInfo.parse(nameAndVersion).toString());
  }

}
