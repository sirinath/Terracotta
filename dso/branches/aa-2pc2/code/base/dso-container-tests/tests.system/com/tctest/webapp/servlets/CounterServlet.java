/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

public class CounterServlet extends HttpServlet {

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    PrintWriter out = response.getWriter();
    try {
      HttpSession session = request.getSession(true);

      Integer count = (Integer) session.getAttribute("count");
      if (count == null) {
        count = new Integer(0);
      }

      if (request.getParameter("read") != null) {
        if (session.isNew()) {
          out.println("session is new"); // this is an error condition (client will fail trying to parse this as int)
        } else {
          out.println(count.intValue());
        }
      } else {
        int newValue = count.intValue() + 1;
        session.setAttribute("count", new Integer(newValue));
        out.println(newValue);
      }
    } catch (RuntimeException e) {
      e.printStackTrace(out);
      throw e;
    }
  }
}