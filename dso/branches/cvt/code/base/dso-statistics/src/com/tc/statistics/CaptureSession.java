/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics;

import com.tc.statistics.retrieval.StatisticsRetriever;

public class CaptureSession {
  private final long id;
  private final StatisticsRetriever retriever;

  public CaptureSession(final long id, final StatisticsRetriever retriever) {
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
