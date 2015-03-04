/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tc.util.runtime.Os;
import com.tc.util.runtime.Vm;
import com.tctest.TestConfigurator;
import com.tctest.TransparentTestIface;
import com.tctest.YoungGCTestBase;

public class LinkedBlockingQueueYoungGenGCTest extends YoungGCTestBase implements TestConfigurator {

  public LinkedBlockingQueueYoungGenGCTest() {
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
