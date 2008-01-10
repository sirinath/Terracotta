/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.retrieval;

import EDU.oswego.cs.dl.util.concurrent.CopyOnWriteArraySet;

import com.tc.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;

public class StatisticType {
  private final static Set TYPES = new CopyOnWriteArraySet();

  public final static StatisticType STARTUP = new StatisticType("STARTUP");
  public final static StatisticType SNAPSHOT = new StatisticType("SNAPSHOT");
  
  private final String identifier;
  
  private StatisticType(String identifier) {
    Assert.assertNotNull("identifier", identifier);
    this.identifier = identifier;
    TYPES.add(this);
  }
  
  public static Collection getAllTypes() {
    return Collections.unmodifiableCollection(TYPES);
  }
  
  public String toString() {
    return identifier.toString();
  }
  
  public int hashCode() {
    return identifier.hashCode();
  }
  
  public boolean equals(Object object) {
    if (null == object) {
      return false;
    }
    
    if (object instanceof StatisticType) {
      return ((StatisticType)object).identifier.equals(identifier);
    }
    
    return false;
  }
}
