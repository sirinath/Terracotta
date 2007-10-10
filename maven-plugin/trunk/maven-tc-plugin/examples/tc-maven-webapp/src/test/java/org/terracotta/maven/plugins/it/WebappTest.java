/*
 * 
 */

package org.terracotta.maven.plugins.it;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import junit.framework.TestCase;

import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebResponse;

/**
 * @author Eugene Kuleshov
 */
public class WebappTest extends TestCase {

  public void testCallIndexPage() throws Exception {
    WebConversation conversation = new WebConversation();

    WebResponse response1 = conversation.getResponse("http://localhost:8080/tc-maven-webapp");
    int counter1 = getCounter(response1.getText());
    assertEquals("counter1: " + counter1, 0, counter1);

    WebResponse response2 = conversation.getResponse("http://localhost:8081/tc-maven-webapp");
    int counter2 = getCounter(response2.getText());
    assertTrue("counter2: " + counter2, counter2 == (counter1 + 1));
    
    WebResponse response3 = conversation.getResponse("http://localhost:8081/tc-maven-webapp");
    int counter3 = getCounter(response3.getText());
    assertTrue("counter2: " + counter3, counter3 == (counter2 + 1));
  }

  private int getCounter(String text) throws IOException {
    BufferedReader r = new BufferedReader(new StringReader(text));
    String line;
    Pattern p = Pattern.compile("&nbsp; counter : (\\d)+<br/>");
    while ((line = r.readLine()) != null) {
      Matcher m = p.matcher(line);
      if (m.find()) {
        return Integer.parseInt(m.group(1));
      }
    }
    return -1;
  }

}
