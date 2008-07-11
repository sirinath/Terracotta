/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;

import java.util.SortedSet;

public class GCResultContext implements EventContext {

  private final int       gcIteration;
  private final SortedSet gcedOids;
  private long  deleteStartMillis;

  public GCResultContext(int gcIteration, SortedSet gcedOids, long deleteStartMillis) {
    this.gcIteration = gcIteration;
    this.gcedOids = gcedOids;
    this.deleteStartMillis = deleteStartMillis;
  }

  public int getGCIterationCount() {
    return gcIteration;
  }

  public SortedSet getGCedObjectIDs() {
    return gcedOids;
  }
  
  public long getDeleteStartMillis() {
    return deleteStartMillis;
  }

  public String toString() {
    return "GCResultContext [ " + gcIteration + " , " + gcedOids.size() + " ]";
  }
}
