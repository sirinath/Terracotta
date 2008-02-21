/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Types;

public class StatisticData implements Serializable {
  private static final long serialVersionUID = -3387790670840965825L;

  private String sessionId;
  private String agentIp;
  private String agentDifferentiator;
  private Date moment;
  private String name;
  private String element;
  private Object data;

  public StatisticData() {
  }
  
  public StatisticData(String name, Date moment, Object value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, Object value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }
  
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticData sessionId(String sessionId) {
    setSessionId(sessionId);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public void setAgentIp(String agentIp) {
    this.agentIp = agentIp;
  }

  public StatisticData agentIp(String agentIp) {
    setAgentIp(agentIp);
    return this;
  }

  public String getAgentDifferentiator() {
    return agentDifferentiator;
  }

  public StatisticData agentDifferentiator(String agentDifferentiator) {
    setAgentDifferentiator(agentDifferentiator);
    return this;
  }

  public void setAgentDifferentiator(String agentDifferentiator) {
    this.agentDifferentiator = agentDifferentiator;
  }

  public void setMoment(Date moment) {
    this.moment = moment;
  }

  public StatisticData moment(Date moment) {
    setMoment(moment);
    return this;
  }

  public Date getMoment() {
    return moment;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public StatisticData name(String name) {
    setName(name);
    return this;
  }

  public String getElement() {
    return element;
  }

  public void setElement(String element) {
    this.element = element;
  }

  public StatisticData element(String element) {
    setElement(element);
    return this;
  }

  public Object getData() {
    return data;
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
      .agentDifferentiator(agentDifferentiator)
      .moment(moment)
      .name(name)
      .element(element)
      .data(data);
  }

  public String toString() {
    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
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
           + "agentDifferentiator = " + agentDifferentiator + "; "
           + "moment = " + (null == moment ? String.valueOf(moment): format.format(moment)) + "; "
           + "name = " + name + "; "
           + "element = " + element + "; "
           + "data = " + data_formatted + ""
           + "]";
  }

  private static String escapeDoubleQuotes(final String value) {
    return StringUtils.replace(value, "\"", "\"\"");
  }

  private static void addCsvField(final StringBuffer result, final Object field, final boolean separator) {
    if (null == field) {
      if (separator) {
        result.append(",");
      }
    } else {
      result.append("\"");
      result.append(escapeDoubleQuotes(String.valueOf(field)));
      result.append("\"");
      if (separator) {
        result.append(",");
      }
    }
  }

  public String toCsv() {
    DateFormat format = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");

    StringBuffer result = new StringBuffer();
    addCsvField(result, sessionId, true);
    addCsvField(result, agentIp, true);
    addCsvField(result, agentDifferentiator, true);
    addCsvField(result, (null == moment ? null : format.format(moment)), true);
    addCsvField(result, name, true);
    addCsvField(result, element, true);
    if (null == data) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof BigDecimal) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, data, false);
    } else if (data instanceof Number) {
      addCsvField(result, data, true);
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof CharSequence) {
      addCsvField(result, null, true);
      addCsvField(result, data, true);
      addCsvField(result, null, true);
      addCsvField(result, null, false);
    } else if (data instanceof Date) {
      addCsvField(result, null, true);
      addCsvField(result, null, true);
      addCsvField(result, (null == data ? null : format.format(data)), true);
      addCsvField(result, null, false);
    }
    result.append("\n");
    return result.toString();
  }
}