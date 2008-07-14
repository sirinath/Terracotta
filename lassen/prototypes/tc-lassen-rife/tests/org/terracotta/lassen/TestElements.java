package org.terracotta.lassen;

import junit.framework.TestCase;

import com.uwyn.rife.engine.Site;
import com.uwyn.rife.test.MockConversation;
import com.uwyn.rife.test.MockResponse;
import com.uwyn.rife.test.ParsedHtml;

public class TestElements extends TestCase {
	public TestElements(String name) {
		super(name);
	}
	
	public void testHome() throws Throwable {
		MockConversation conversation = new MockConversation(Site.getRepInstance());
		MockResponse response = conversation.doRequest("/");
		ParsedHtml parsed = response.getParsedHtml();
		
		// check that the home page is present at the root URL
		assertEquals("Lassen - Welcome", parsed.getTitle());
	}
}
