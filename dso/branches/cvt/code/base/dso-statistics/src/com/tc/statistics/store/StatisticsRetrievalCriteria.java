/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.store;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class StatisticsRetrievalCriteria {
  private String sessionId = null;
  private Date start = null;
  private Date stop = null;
  private String agentIp = null;
  private String agentDifferentiator = null;
  private Set names = null;
  private Set elements = null;
  
  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(final String sessionId) {
    this.sessionId = sessionId;
  }

  public StatisticsRetrievalCriteria sessionId(final String sessionId) {
    setSessionId(sessionId);
    return this;
  }

  public Date getStart() {
    return start;
  }

  public void setStart(final Date start) {
    this.start = start;
  }

  public StatisticsRetrievalCriteria start(final Date start) {
    setStart(start);
    return this;
  }

  public Date getStop() {
    return stop;
  }

  public void setStop(final Date stop) {
    this.stop = stop;
  }

  public StatisticsRetrievalCriteria stop(final Date stop) {
    setStop(stop);
    return this;
  }

  public String getAgentIp() {
    return agentIp;
  }

  public void setAgentIp(final String agentip) {
    this.agentIp = agentip;
  }

  public StatisticsRetrievalCriteria agentIp(final String agentIp) {
    setAgentIp(agentIp);
    return this;
  }

  public String getAgentDifferentiator() {
    return agentDifferentiator;
  }

  public void setAgentDifferentiator(final String agentDifferentiator) {
    this.agentDifferentiator = agentDifferentiator;
  }

  public StatisticsRetrievalCriteria agentDifferentiator(final String agentDifferentiator) {
    setAgentDifferentiator(agentDifferentiator);
    return this;
  }

  public Collection getNames() {
    if (null == names) {
      return Collections.EMPTY_SET;
    }

    return Collections.unmodifiableSet(names);
  }

  public StatisticsRetrievalCriteria addName(final String name) {
    if (null == names) {
      names = new LinkedHashSet();
    }
    names.add(name);

    return this;
  }

  public Collection getElements() {
    if (null == elements) {
      return Collections.EMPTY_SET;
    }

    return Collections.unmodifiableSet(elements);
  }

  public StatisticsRetrievalCriteria addElement(final String element) {
    if (null == elements) {
      elements = new LinkedHashSet();
    }
    elements.add(element);

    return this;
  }

  public String toString() {
    DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss SSS");
    StringBuffer out = new StringBuffer("[");

    out.append("agentip = ");
    out.append(agentIp);
    out.append("; ");

    out.append("sessionId = ");
    out.append(sessionId);
    out.append("; ");

    out.append("start = ");
    if (null == start) {
      out.append(String.valueOf(start));
    } else {
      out.append(format.format(start));
    }
    out.append("; ");

    out.append("stop = ");
    if (null == stop) {
      out.append(String.valueOf(stop));
    } else {
      out.append(format.format(stop));
    }
    out.append("; ");

    out.append("names = [");
    if (names != null && names.size() > 0) {
      boolean first = true;
      Iterator it = names.iterator();
      while (it.hasNext()) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }
        out.append(it.next());
      }
    }
    out.append("]; ");

    out.append("elements = [");
    if (elements != null && elements.size() > 0) {
      boolean first = true;
      Iterator it = elements.iterator();
      while (it.hasNext()) {
        if (first) {
          first = false;
        } else {
          out.append(", ");
        }
        out.append(it.next());
      }
    }
    out.append("]");

    out.append("]");

    return out.toString();
  }
}
