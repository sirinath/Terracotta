/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;

import java.math.BigDecimal;
import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;

import junit.framework.TestCase;

public class StatisticsDataTest extends TestCase {
  public void testDefaultInstantiation() throws Exception {
    StatisticData data = new StatisticData();
    assertNull(data.getSessionId());
    assertNull(data.getAgentIp());
    assertNull(data.getMoment());
    assertNull(data.getName());
    assertNull(data.getElement());
    assertNull(data.getData());
  }

  public void testDefaultToString() throws Exception {
    StatisticData data = new StatisticData();
    assertEquals("[sessionId = null; agentIp = null; agentDifferentiator = null; moment = null; name = null; element = null; data = null]", data.toString());
  }

  public void testFluentInterface() throws Exception {
    Date moment = new Date();
    StatisticData data = new StatisticData()
      .sessionId("3984693")
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .agentDifferentiator("blurb")
      .moment(moment)
      .name("statname")
      .element("first")
      .data(new Long(987983343L));

    assertEquals("3984693", data.getSessionId());
    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertEquals("blurb", data.getAgentDifferentiator());
    assertEquals(moment, data.getMoment());
    assertEquals("statname", data.getName());
    assertEquals("first", data.getElement());
    assertEquals(new Long(987983343L), data.getData());

    data.data("datastring");
    assertEquals("datastring", data.getData());

    Date dataDate = new Date();
    data.data(dataDate);
    assertEquals(dataDate, data.getData());
  }

  public void testSetters() throws Exception {
    Date moment = new Date();
    StatisticData data = new StatisticData();
    data.setSessionId("3984693");
    data.setAgentIp(InetAddress.getLocalHost().getHostAddress());
    data.setAgentDifferentiator("blurb");
    data.setMoment(moment);
    data.setName("statname");
    data.setElement("first");
    data.setData(new Long(987983343L));

    assertEquals("3984693", data.getSessionId());
    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertEquals("blurb", data.getAgentDifferentiator());
    assertEquals(moment, data.getMoment());
    assertEquals("statname", data.getName());
    assertEquals("first", data.getElement());
    assertEquals(new Long(987983343L), data.getData());

    data.setData("datastring");
    assertEquals("datastring", data.getData());

    Date dataDate = new Date();
    data.setData(dataDate);
    assertEquals(dataDate, data.getData());

    data.setData(new BigDecimal("343.1778"));
    assertEquals(new BigDecimal("343.1778"), data.getData());
  }

  public void testToString() throws Exception {
    Calendar moment = Calendar.getInstance();
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);
    StatisticData data = new StatisticData()
      .sessionId("3984693")
      .agentIp("192.168.1.18")
      .agentDifferentiator("7826")
      .moment(moment.getTime())
      .name("statname")
      .element("first")
      .data(new Long(987983343L));
    assertEquals("[sessionId = 3984693; agentIp = 192.168.1.18; agentDifferentiator = 7826; moment = 2008-01-09 16:25:52 000; name = statname; element = first; data = 987983343]", data.toString());
  }
}
