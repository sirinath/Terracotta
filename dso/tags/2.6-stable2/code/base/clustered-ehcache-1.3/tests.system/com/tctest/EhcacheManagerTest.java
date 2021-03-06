package com.tctest;

import java.util.Date;

public class EhcacheManagerTest extends TransparentTestBase {

  public EhcacheManagerTest() {
    disableAllUntil(new Date(Long.MAX_VALUE));
  }

  public void doSetUp(final TransparentTestIface tt) throws Exception {
    tt.getTransparentAppConfig().setClientCount(EhcacheManagerTestApp.EXPECTED_THREAD_COUNT);
    tt.initializeTestRunner();
  }

  protected Class getApplicationClass() {
    return EhcacheManagerTestApp.class;
  }

}
