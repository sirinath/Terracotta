/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.spring.aop;

import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.tc.test.TCTestCase;


// FIXME test IntroductionInterceptor
// FIXME more complex tests - chained tests etc. 

/**
 * @author Jonas Bon&#233;r
 */
public class DelegatingProxyAopProxy_Test extends TCTestCase {

  private static final String BEAN_CONFIG = "com/tctest/spring/beanfactory-fastproxy.xml";

  public DelegatingProxyAopProxy_Test(String name) {
    super(name);
    disableAllUntil("2008-09-18");  // XXX timebombed
  }

  public static void testBeforeAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    IDelegatingProxyTarget proxy = (IDelegatingProxyTarget) ctx.getBean("testBeforeAdviceDelegating");
    assertNotNull(proxy);
    proxy.doStuff("fuzzy");
    assertEquals("before args(fuzzy) this(" + proxy.getClass().getName() + ") doStuff ", Logger.log);
  }

  public static void testAfterReturningAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    IDelegatingProxyTarget proxy = (IDelegatingProxyTarget) ctx.getBean("testAfterReturningAdviceDelegating");
    assertNotNull(proxy);
    proxy.returnStuff("fuzzy");
    assertEquals("returnStuff after-returning(stuff) args(fuzzy) this(" + proxy.getClass().getName() + ") ", Logger.log);
  }

  public static void testAfterThrowingAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    IDelegatingProxyTarget proxy = (IDelegatingProxyTarget) ctx.getBean("testAfterThrowingAdviceDelegating");
    assertNotNull(proxy);
    try {
      proxy.throwStuff("fuzzy");
    } catch (ExpectedException e) {
      assertEquals("throwStuff after-throwing(expected) args(fuzzy) this(" + proxy.getClass().getName() + ") ",
          Logger.log);
      return;
    }
    fail("should have exited with an exception");
  }

  public static void testAroundAdvice() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    IDelegatingProxyTarget proxy = (IDelegatingProxyTarget) ctx.getBean("testAroundAdviceDelegating");
    assertNotNull(proxy);
    proxy.doStuff("fuzzy");
    assertEquals("before-around args(fuzzy) this(" + proxy.getClass().getName() + ") doStuff after-around ", Logger.log);
  }

  public static void testAroundAdviceChain() {
    Logger.log = "";
    ClassPathXmlApplicationContext ctx = new ClassPathXmlApplicationContext(BEAN_CONFIG);
    IDelegatingProxyTarget proxy = (IDelegatingProxyTarget) ctx.getBean("testAroundAdviceChainDelegating");
    assertNotNull(proxy);
    proxy.doStuff("fuzzy");
    assertEquals("before-around args(fuzzy) this(" + proxy.getClass().getName() + ") before-around args(fuzzy) this("
        + proxy.getClass().getName() + ") doStuff after-around after-around ", Logger.log);
  }


  // XXX use test decorator to activate AW pipeline
  public static junit.framework.Test suite() {
    return new junit.framework.TestSuite(DelegatingProxyAopProxy_Test.class);
  }
}
