/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;

import java.net.InetAddress;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import junit.framework.TestCase;

public class StatisticsDataTest extends TestCase {
  public void testDefaultInstantiation() throws Exception {
    StatisticData data = new StatisticData();
    assertNull(data.getAgentIp());
    assertNull(data.getMoment());
    assertNull(data.getName());
    assertNull(data.getElement());
    assertNull(data.getData());
  }
  
  public void testFluentInterface() throws Exception {
    Date moment = new Date();
    StatisticData data = new StatisticData()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(moment)
      .name("statname")
      .element("first")
      .data(new Long(987983343L));

    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
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
    data.setAgentIp(InetAddress.getLocalHost().getHostAddress());
    data.setMoment(moment);
    data.setName("statname");
    data.setElement("first");
    data.setData(new Long(987983343L));

    assertEquals(InetAddress.getLocalHost().getHostAddress(), data.getAgentIp());
    assertEquals(moment, data.getMoment());
    assertEquals("statname", data.getName());
    assertEquals("first", data.getElement());
    assertEquals(new Long(987983343L), data.getData());
    
    data.setData("datastring");
    assertEquals("datastring", data.getData());
    
    Date dataDate = new Date();
    data.setData(dataDate);
    assertEquals(dataDate, data.getData());
  }
  
  public void testToString() throws Exception {
    Calendar moment = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    moment.set(2008, 0, 9, 16, 25, 52);
    moment.set(Calendar.MILLISECOND, 0);
    StatisticData data = new StatisticData()
      .agentIp("192.168.1.18")
      .moment(moment.getTime())
      .name("statname")
      .element("first")
      .data(new Long(987983343L));
    assertEquals("[agentIp = 192.168.1.18; moment = 1/9/08 5:25 PM; name = statname; element = first; data = 987983343]", data.toString());
  }
}
