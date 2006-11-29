/**
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tcspring;

import org.springframework.beans.factory.support.AbstractAutowireCapableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import com.tc.aspectwerkz.joinpoint.StaticJoinPoint;

/**
 * Version for Spring 2.x
 */
public interface VirtualizeSingletonBeanJoinPoint2 extends StaticJoinPoint {

  Object proceed(String beanName, Object bean, RootBeanDefinition mergedBeanDefinition,
                 AbstractAutowireCapableBeanFactory beanFactory);

}
