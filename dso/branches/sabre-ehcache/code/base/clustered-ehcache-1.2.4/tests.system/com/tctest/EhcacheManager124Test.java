package com.tctest;

public class EhcacheManager124Test extends TransparentTestBase {
	public EhcacheManager124Test() {
		// disableAllUntil("2007-08-01");
	}

	public void doSetUp(final TransparentTestIface tt) throws Exception {
		tt.getTransparentAppConfig().setClientCount(
				EhcacheManagerTestApp.EXPECTED_THREAD_COUNT);
		tt.initializeTestRunner();
	}

	protected Class getApplicationClass() {
		return EhcacheManagerTestApp.class;
	}
}
