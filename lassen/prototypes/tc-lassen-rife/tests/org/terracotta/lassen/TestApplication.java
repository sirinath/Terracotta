package org.terracotta.lassen;

import com.uwyn.rife.rep.Rep;
import junit.framework.Test;
import junit.framework.TestSuite;

public class TestApplication extends TestSuite {
	public static Test suite() {
		// initialize the repository with all required participants
		try {
			Rep.initialize("rep/participants.xml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		// setup the test suite
		TestSuite suite = new TestSuite("Lassen tests");
		suite.addTestSuite(TestElements.class);
		
		return suite;
	}
}
