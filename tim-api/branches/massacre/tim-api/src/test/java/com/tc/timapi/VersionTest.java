/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.timapi;

import java.util.Properties;

import junit.framework.TestCase;

public class VersionTest extends TestCase {

  public void testBasic() throws Exception {
    Properties props = new Properties();
    props.put("version", "1.3.0");
    Version ver = new Version(props);

    assertEquals(1, ver.getMajor());
    assertEquals(3, ver.getMinor());
    assertEquals(0, ver.getIncremental());
    assertEquals("", ver.getQualifier());
    assertFalse(ver.isSnapshot());
    assertEquals("1.3.0", ver.getFullVersionString());
  }

  public void testSnapshot() throws Exception {
    Properties props = new Properties();
    props.put("version", "2.1.3-SNAPSHOT");
    Version ver = new Version(props);

    assertEquals(2, ver.getMajor());
    assertEquals(1, ver.getMinor());
    assertEquals(3, ver.getIncremental());
    assertEquals("SNAPSHOT", ver.getQualifier());
    assertTrue(ver.isSnapshot());
    assertEquals("2.1.3-SNAPSHOT", ver.getFullVersionString());
  }

  public void testOtherQualifier() throws Exception {
    Properties props = new Properties();
    props.put("version", "4.5.6-foo");
    Version ver = new Version(props);

    assertEquals(4, ver.getMajor());
    assertEquals(5, ver.getMinor());
    assertEquals(6, ver.getIncremental());
    assertEquals("foo", ver.getQualifier());
    assertFalse(ver.isSnapshot());
    assertEquals("4.5.6-foo", ver.getFullVersionString());
  }

}
