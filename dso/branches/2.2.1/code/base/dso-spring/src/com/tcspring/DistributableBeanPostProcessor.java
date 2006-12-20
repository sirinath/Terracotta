/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * Post process local beans for distributing.
 * 
 * @author Eugene Kuleshov
 */
public class DistributableBeanPostProcessor implements BeanPostProcessor {

  private final DistributableBeanFactory factory;

  
  public DistributableBeanPostProcessor(DistributableBeanFactory factory) {
    this.factory = factory;
  }

  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }
  
  public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
    if(factory.isDistributedSingleton(beanName)) {
      ComplexBeanId beanId = new ComplexBeanId(beanName);
      BeanContainer container = factory.getBeanContainer(beanId);
      if(container==null) {
        factory.putBeanContainer(beanId, new BeanContainer(bean, true));
      } else {
        factory.virtualizeBean(beanId, bean, container);
        container.setInitialized(true);
      }
    }
    return bean;
  }

}

