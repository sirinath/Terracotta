/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.aspectwerkz.intercept;

import com.tc.aspectwerkz.joinpoint.JoinPoint;

/**
 * @author <a href="mailto:jboner@codehaus.org">Jonas Bonr </a>
 */
public interface AfterThrowingAdvice extends Advice {

  /**
   * @param jp
   * @throws Throwable
   */
  void invoke(JoinPoint jp, Throwable exception) throws Throwable;
}
