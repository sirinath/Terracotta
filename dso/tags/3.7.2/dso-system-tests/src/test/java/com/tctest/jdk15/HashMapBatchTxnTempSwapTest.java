/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.jdk15;

import com.tctest.TransparentTestBase;
import com.tctest.TransparentTestIface;

import java.util.ArrayList;

/*
 * Test case for CDV-253
 */

public class HashMapBatchTxnTempSwapTest extends TransparentTestBase {

  private static final int NODE_COUNT = 2;

  @Override
  public void doSetUp(TransparentTestIface t) throws Exception {
    t.getTransparentAppConfig().setClientCount(NODE_COUNT);
    t.initializeTestRunner();
  }

  @Override
  protected void setExtraJvmArgs(ArrayList jvmArgs) {
    super.setExtraJvmArgs(jvmArgs);
    jvmArgs.add("-Dcom.tc.l2.db.factory.name=com.tc.objectserver.storage.derby.DerbyDBFactory");
    jvmArgs.add("-Xmx256m");
  }

  @Override
  protected Class getApplicationClass() {
    return HashMapBatchTxnTestApp.class;
  }

  @Override
  protected boolean useExternalProcess() {
    return true;
  }

}
