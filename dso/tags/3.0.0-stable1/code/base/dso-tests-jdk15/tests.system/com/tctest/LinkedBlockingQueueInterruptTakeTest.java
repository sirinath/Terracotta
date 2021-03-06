/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest;

import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;

import java.util.Date;

public class LinkedBlockingQueueInterruptTakeTest extends TransparentTestBase {

  private static final int NODE_COUNT = 3;

  public LinkedBlockingQueueInterruptTakeTest() {
    // MNK-527
    if (Os.isSolaris() && !Vm.isJDK16Compliant()) {
      disableAllUntil(new Date(Long.MAX_VALUE));
    }
  }

  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return LinkedBlockingQueueInterruptTakeTestApp.class;
  }

}