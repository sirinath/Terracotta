/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.faulting;

public class DualQueueFault8Node extends DualQueueFaultBase {

  protected int nodeCount() {
    return 9; // + 1 for writer node
  }
}
