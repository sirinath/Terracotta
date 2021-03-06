/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.webapp.servletfilters;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

public class SimpleFilter implements Filter {
  public void doFilter(final ServletRequest request, final ServletResponse response, FilterChain chain)
      throws IOException, ServletException {

    System.out.println("Entering Filter");
    HttpSession session = ((HttpServletRequest) request).getSession(true);
    System.out.println("SESSION ID: " + session.getId());
    chain.doFilter(request, response);
    System.out.println("Exiting Filter");
  }

  public void init(FilterConfig config) {
    System.out.println("Filter Initialized");
  }

  public void destroy() {
    System.out.println("Filter Destroyed");
  }
}
