/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.aspectwerkz.intercept;

import com.tc.aspectwerkz.joinpoint.JoinPoint;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bon�r </a>
 */
public interface AfterReturningAdvice extends Advice {

  /**
   * @param jp
   * @throws Throwable
   */
  void invoke(JoinPoint jp, Object returnValue) throws Throwable;
}
