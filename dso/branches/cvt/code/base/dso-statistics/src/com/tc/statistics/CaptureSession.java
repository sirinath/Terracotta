/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.retrieval.StatisticsRetriever;

public class CaptureSession {
  private long id;
  private StatisticsRetriever retriever;

  public CaptureSession(long id, StatisticsRetriever retriever) {
    this.id = id;
    this.retriever = retriever;
  }

  public long getId() {
    return id;
  }

  public StatisticsRetriever getRetriever() {
    return retriever;
  }
}
