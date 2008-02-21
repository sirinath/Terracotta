/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.math.BigDecimal;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StatisticData implements Serializable {
  private final static long serialVersionUID = -3387790670840965825L;

  private final static Pattern CSV_LINE_PATTERN = Pattern.compile(
    "^" +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" + "," +
    "\\s*((?:\\\".*(?:(?<=\\\\\\\\)|(?<!\\\\))\\\")|)\\s*" +
    "$");
  private final static Pattern CSV_ESCAPED_DOUBLE_QUOTES = Pattern.compile("(?:(?<=\\\\\\\\)|(?<!\\\\))\\\\\"");
  private final static Pattern CSV_ESCAPED_NEWLINE = Pattern.compile("(?:(?<=\\\\\\\\)|(?<!\\\\))\\\\n");

  private String sessionId;
  private String agentIp;
  private String agentDifferentiator;
  private Date moment;
  private String name;
  private String element;
  private Object data;

  public StatisticData() {
  }
  
  public StatisticData(String name, Date moment, Long value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, String value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, Date value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, BigDecimal value) {
    setName(name);
    setMoment(moment);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, Long value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, String value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, Date value) {
    setName(name);
    setMoment(moment);
    setElement(element);
    setData(value);
  }

  public StatisticData(String name, Date moment, String element, BigDecimal value) {
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
    DateFormat format = newDateFormatInstance();
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

  private static String escapeForCsv(final String value) {
    String result = value;
    result = StringUtils.replace(result, "\\", "\\\\");
    result = StringUtils.replace(result, "\"", "\\\"");
    result = StringUtils.replace(result, "\n", "\\n");
    return result;
  }

  private static void addCsvField(final StringBuffer result, final Object field, final boolean separator) {
    if (null == field) {
      if (separator) {
        result.append(",");
      }
    } else {
      result.append("\"");
      result.append(escapeForCsv(String.valueOf(field)));
      result.append("\"");
      if (separator) {
        result.append(",");
      }
    }
  }

  public String toCsv() {
    DateFormat format = newDateFormatInstance();

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

  private static SimpleDateFormat newDateFormatInstance() {
    return new SimpleDateFormat("MM/dd/yyyy HH:mm:ss SSS");
  }

  private static String csvGroup(final Matcher matcher, final int group) {
    String result = matcher.group(group);
    if (result.length() < 2) {
      return null;
    }
    if (!result.startsWith("\"") || !result.endsWith("\"")) {
      return null;
    }

    result = result.substring(1, result.length() - 1);
    result = CSV_ESCAPED_DOUBLE_QUOTES.matcher(result).replaceAll("\"");
    result = CSV_ESCAPED_NEWLINE.matcher(result).replaceAll("\n");
    result = StringUtils.replace(result, "\\\\", "\\");
    return result;
  }

  public static StatisticData newInstanceFromCsvLine(final String line) throws ParseException {
    Matcher matcher = CSV_LINE_PATTERN.matcher(line);
    if (!matcher.matches()) {
      return null;
    }

    String moment_raw = csvGroup(matcher, 4);
    DateFormat date_format = newDateFormatInstance();
    Date moment = null;
    if (moment_raw != null) {
      moment = date_format.parse(moment_raw);
    }

    StatisticData data = new StatisticData();
    data.setSessionId(csvGroup(matcher, 1));
    data.setAgentIp(csvGroup(matcher, 2));
    data.setAgentDifferentiator(csvGroup(matcher, 3));
    data.setMoment(moment);
    data.setName(csvGroup(matcher, 5));
    data.setElement(csvGroup(matcher, 6));

    String data_number = csvGroup(matcher, 7);
    String data_text = csvGroup(matcher, 8);
    String data_date = csvGroup(matcher, 9);
    String data_bigdecimal = csvGroup(matcher, 10);

    if (data_number != null) {
      data.setData(new Long(Long.parseLong(data_number)));
    } else if (data_text != null) {
      data.setData(data_text);
    } else if (data_date != null) {
      Date date = date_format.parse(moment_raw);
      data.setData(date);
    } else if (data_bigdecimal != null) {
      data.setData(new BigDecimal(data_bigdecimal));
    }

    return data;
  }
}