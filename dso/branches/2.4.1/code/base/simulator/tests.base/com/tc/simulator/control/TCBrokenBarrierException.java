/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.control;

import EDU.oswego.cs.dl.util.concurrent.BrokenBarrierException;

public class TCBrokenBarrierException extends Exception {

  public TCBrokenBarrierException(BrokenBarrierException e) {
    super(e);
  }

}
