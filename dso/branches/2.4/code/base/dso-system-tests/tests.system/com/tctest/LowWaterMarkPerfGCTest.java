/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import java.util.Date;

public class LowWaterMarkPerfGCTest extends GCTestBase {
  private final int     NODE_COUNT                  = 2;
  private final boolean GC_ENABLED                  = false;


  public LowWaterMarkPerfGCTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }
  
  protected Class getApplicationClass() {
    return LowWaterMarkPerfGCTestApp.class;
  }
  
  protected boolean getGCEnabled() {
    return GC_ENABLED;
  }


  protected int getNodeCount() {
    return NODE_COUNT;
  }
}
