/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servlets;

import com.tc.util.Assert;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public final class RootSharingServletA extends HttpServlet {
  
  private final String SHARED_CLASS_NAME = "com.tctest.externall1.StandardClasspathDummyClass";
  
  // SharedCache contains a root
  private SharedCache cache = new SharedCache();

  public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    response.setContentType("text/html");
    String cmd = request.getParameter("cmd");
    
    try {
      if ("set".equals(cmd)) {
        Class clazz = Class.forName(SHARED_CLASS_NAME);
        cache.put(clazz, "A");
        response.getWriter().println("OK");
      } else if ("get".equals(cmd)) {
        Assert.assertEquals(1, cache.size());
        Class clazz = (Class)cache.keys().next();
        Assert.assertEquals(SHARED_CLASS_NAME, clazz.getName());
        Class clazzRef = Class.forName(SHARED_CLASS_NAME);
        Assert.assertEquals(clazzRef, clazz);
        response.getWriter().println("OK");
      } else {
        response.getWriter().println("UNRECOGNIZED COMMAND: " + cmd);
      }
    } catch (Exception e) {
      PrintWriter writer = response.getWriter();
      writer.println("ERROR: " + e.getMessage());
      e.printStackTrace(writer);
    }
    
  }
}