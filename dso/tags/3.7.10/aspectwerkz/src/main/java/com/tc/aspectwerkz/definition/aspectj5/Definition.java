/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.definition.aspectj5;

import java.util.ArrayList;
import java.util.List;

/**
 * A POJO that contains raw strings from the XML (sort of XMLBean for our simple LTW DTD)
 *
 * @author <a href="mailto:alex AT gnilux DOT com">Alexandre Vasseur</a>
 */
public class Definition {

  private StringBuffer m_weaverOptions;

  private List m_dumpPatterns;

  private List m_includePatterns;

  private List m_excludePatterns;

  private List m_aspectClassNames;

  private List m_aspectExcludePatterns;

  private List m_concreteAspects;

  public Definition() {
    m_weaverOptions = new StringBuffer();
    m_dumpPatterns = new ArrayList(0);
    m_includePatterns = new ArrayList(0);
    m_excludePatterns = new ArrayList(0);
    m_aspectClassNames = new ArrayList();
    m_aspectExcludePatterns = new ArrayList(0);
    m_concreteAspects = new ArrayList(0);
  }

  public String getWeaverOptions() {
    return m_weaverOptions.toString();
  }

  public List getDumpPatterns() {
    return m_dumpPatterns;
  }

  public List getIncludePatterns() {
    return m_includePatterns;
  }

  public List getExcludePatterns() {
    return m_excludePatterns;
  }

  public List getAspectClassNames() {
    return m_aspectClassNames;
  }

  public List getAspectExcludePatterns() {
    return m_aspectExcludePatterns;
  }

  public List getConcreteAspects() {
    return m_concreteAspects;
  }

  public static class ConcreteAspect {
    String name;
    String extend;
    List pointcuts;

    public ConcreteAspect(String name, String extend) {
      this.name = name;
      this.extend = extend;
      this.pointcuts = new ArrayList();
    }
  }

  public static class Pointcut {
    String name;
    String expression;

    public Pointcut(String name, String expression) {
      this.name = name;
      this.expression = expression;
    }
  }

  public void appendWeaverOptions(String option) {
    m_weaverOptions.append(option.trim()).append(' ');
  }
}
