/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;
import com.tctest.GCTestBase;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;

public class LinkedBlockingQueueGCTest extends GCTestBase implements TestConfigurator {

  public LinkedBlockingQueueGCTest() {
    if (Os.isWindows() && Vm.isJDK15()) {
      disableTest();
    }
  }

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setAttribute(LinkedBlockingQueueTestApp.GC_TEST_KEY, "true");
    super.doSetUp(t);
  }

  @Override
  protected Class getApplicationClass() {
    return LinkedBlockingQueueTestApp.class;
  }

}
