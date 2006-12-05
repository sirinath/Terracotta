/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package demoapp;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.*;
import java.util.Set;
import java.util.HashSet;


public class SimpleSession extends HttpServlet { 
  
  private Set sessions = new HashSet();
  
  public void doGet (HttpServletRequest req, HttpServletResponse res)
       throws ServletException, IOException
  {
    //Get the session object
    HttpSession session = req.getSession(true);

    // Local variables
    PrintWriter out = res.getWriter();
    Integer ival;

    // set content type and other response header fields first
    res.setContentType("text/html");
    
    // Retrieve the count value from the session
    ival = (Integer) session.getAttribute("sessiontest.counter");
    if (ival==null) ival = new Integer(1);
    else ival = new Integer(ival.intValue() + 1);
    
    session.setAttribute("sessiontest.counter", ival);

    try {
      // then write the data of the response

      out.println("You have hit this page <b>" + ival + "</b> times.<p>");
      synchronized(sessions) {
        sessions.add(new SomeStuff(session.getId()));        
        out.println(sessions);
      }
      // when the user clicks on the link in the next line,
      // the SessionServlet is called again,
      // but now URL rewriting is turned on
    } catch (Exception ex) {
      out.println("<p><b>!! Example Failed !!<br><br> The following exception occurred:</b><br><br><pre>");
      ex.printStackTrace(new PrintWriter(out));
      ex.printStackTrace();
      out.println("</pre>");
    }
  }

  public static class SomeStuff {
    private String sessionID;
    public SomeStuff(String sessionID){
      this.sessionID = sessionID;
    }

    public String toString(){
	return "SomeStuff("+sessionID+")";
    }

  }
}
