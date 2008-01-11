/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.retrieval.actions;

import com.tc.statistics.StatisticData;
import com.tc.statistics.retrieval.StatisticRetrievalAction;
import com.tc.statistics.retrieval.actions.SRASystemProperties;

import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.util.Date;
import java.util.Properties;

import junit.framework.TestCase;

public class SRASystemPropertiesTest extends TestCase {
  public void testRetrieval() throws Exception {
    StatisticRetrievalAction action = new SRASystemProperties();
    Date before = new Date();
    StatisticData data = action.retrieveStatisticData()[0];
    Date after = new Date();
    assertEquals("SRASystemProperties", data.getName());
    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertTrue(before.compareTo(data.getMoment()) <= 0);
    assertTrue(after.compareTo(data.getMoment()) >= 0);

    assertNull(data.getElement());
    Properties props = new Properties();
    props.load(new ByteArrayInputStream(((String)data.getData()).getBytes("ISO-8859-1")));

    Properties sysprops = System.getProperties();
    assertEquals(props, sysprops);
  }
}