/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.springframework.web.context.request;

import javax.servlet.http.HttpServletRequest;

/**
 * Helper class to expose the  <code>ServletRequestAttributes.getRequest()</code>.
 * 
 * @author Liyu Yi
 */
public class HttpRequestAccessor {
  public static HttpServletRequest getRequest(ServletRequestAttributes attributes) {
    return attributes.getRequest();
  }
}
