package com.tctest;

public class EhcacheManagerTest extends TransparentTestBase {

	public EhcacheManagerTest() {
		//xisableAllUntil("2008-06-01");
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
