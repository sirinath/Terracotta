/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.support.AbstractBeanFactory;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;


/**
 * Virtualize <code>AbstractBeanFactory.getBean()</code>.
 * 
 * @author Eugene Kuleshov
 */
public class GetBeanProtocol {
  
  /**
   * Invoked after constructor of the <code>AbstractBeanFactory</code>
   * 
   * @see org.springframework.beans.factory.support.AbstractBeanFactory#AbstractBeanFactory()
   */
  public void registerBeanPostProcessor(StaticJoinPoint jp, AbstractBeanFactory factory) {
    if(factory instanceof DistributableBeanFactory) {
      factory.addBeanPostProcessor(new DistributableBeanPostProcessor((DistributableBeanFactory) factory));
    }
  }
  
}

