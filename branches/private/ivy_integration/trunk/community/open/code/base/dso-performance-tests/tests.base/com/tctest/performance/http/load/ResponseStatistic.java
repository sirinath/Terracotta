/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.performance.http.load;

import java.io.Serializable;

final class ResponseStatistic implements Serializable {

  private final long   startTime;
  private final long   endTime;
  private final String url;
  private final int    statusCode;

  public ResponseStatistic(long startTime, long endTime, String url, int statusCode) {
    this.startTime = startTime;
    this.endTime = endTime;
    this.url = url;
    this.statusCode = statusCode;
  }

  public long duration() {
    return endTime - startTime;
  }

  public String url() {
    return url;
  }

  public int statusCode() {
    return statusCode;
  }

  public long startTime() {
    return startTime;
  }

  public long endTime() {
    return endTime;
  }
}
