/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.runner;

public class DistributedTestRunnerConfig {

  private long startTimeout     = 30 * 1000;
  private long executionTimeout = 13 * 60 * 1000;

  public long executionTimeout() {
    return this.executionTimeout;
  }

  public long startTimeout() {
    return this.startTimeout;
  }

  public void setExecutionTimeout(long executionTimeout) {
    this.executionTimeout = executionTimeout;
  }

}
