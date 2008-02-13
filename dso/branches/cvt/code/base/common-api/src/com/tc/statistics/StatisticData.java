/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.exception.TCRuntimeException;

import java.io.Serializable;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

public class StatisticData implements Serializable {
  private static final long serialVersionUID = -5767295895871812119L;

  private Long sessionId;
  private String agentIp;
  private Date moment;
  private String name;
  private String element;
  private Object data;

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, Long value) {
    return _buildInstanceForLocalhost(name, moment, null, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, String value) {
    return _buildInstanceForLocalhost(name, moment, null, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, Date value) {
    return _buildInstanceForLocalhost(name, moment, null, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, BigDecimal value) {
    return _buildInstanceForLocalhost(name, moment, null, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, String element, Long value) {
    return _buildInstanceForLocalhost(name, moment, element, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, String element, String value) {
    return _buildInstanceForLocalhost(name, moment, element, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, String element, Date value) {
    return _buildInstanceForLocalhost(name, moment, element, value);
  }

  public static StatisticData buildInstanceForLocalhost(String name, Date moment, String element, BigDecimal value) {
    return _buildInstanceForLocalhost(name, moment, element, value);
  }

  private static StatisticData _buildInstanceForLocalhost(String name, Date moment, String element, Object value) {
    try {
      return new StatisticData()
        .moment(moment)
        .name(name)
        .agentIp(InetAddress.getLocalHost().getHostAddress())
        .element(element)
        .data(value);
    } catch (UnknownHostException e) {
      throw new TCRuntimeException(e);
    }
  }

  public Long getSessionId() {
    return sessionId;
  }

  public void setSessionId(Long sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticData sessionId(Long sessionId) {
    setSessionId(sessionId);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public Date getMoment() {
    return moment;
  }

  public String getName() {
    return name;
  }

  public String getElement() {
    return element;
  }

  public Object getData() {
    return data;
  }

  public void setAgentIp(String agentIp) {
    this.agentIp = agentIp;
  }

  public StatisticData agentIp(String agentIp) {
    setAgentIp(agentIp);
    return this;
  }

  public void setMoment(Date moment) {
    this.moment = moment;
  }

  public StatisticData moment(Date moment) {
    setMoment(moment);
    return this;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StatisticData name(String name) {
    setName(name);
    return this;
  }

  public void setElement(String element) {
    this.element = element;
  }

  public StatisticData element(String element) {
    setElement(element);
    return this;
  }

  private void setData(Object data) {
    this.data = data;
  }

  private StatisticData data(Object data) {
    setData(data);
    return this;
  }

  public void setData(Long data) {
    setData((Object)data);
  }

  public StatisticData data(Long data) {
    return data((Object)data);
  }

  public void setData(String data) {
    setData((Object)data);
  }

  public StatisticData data(String data) {
    return data((Object)data);
  }

  public void setData(Date data) {
    setData((Object)data);
  }

  public StatisticData data(Date data) {
    return data((Object)data);
  }

  public void setData(BigDecimal data) {
    setData((Object)data);
  }

  public StatisticData data(BigDecimal data) {
    return data((Object)data);
  }

  public Object clone() {
    return new StatisticData()
      .sessionId(sessionId)
      .agentIp(agentIp)
      .moment(moment)
      .name(name)
      .element(element)
      .data(data);
  }

  public String toString() {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
    String data_formatted;
    if (data != null &&
        data instanceof Date) {
      data_formatted = format.format(data);
    } else {
      data_formatted = String.valueOf(data);
    }
    return "["
           + "sessionId = " + sessionId + "; "
           + "agentIp = " + agentIp + "; "
           + "moment = " + (null == moment ? String.valueOf(moment): format.format(moment)) + "; "
           + "name = " + name + "; "
           + "element = " + element + "; "
           + "data = " + data_formatted + ""
           + "]";
  }
}