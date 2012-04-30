/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.net.core.BufferManagerFactory;

/**
 * @author Ludovic Orban
 */
public class SecurityContext {
  public SecurityContext() {
  }

  public BufferManagerFactory createBufferManagerFactory() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    Class<?> bufferManagerFactoryClass = Class.forName("com.tc.net.core.ssl.SSLBufferManagerFactory");
    Object bufferManagerFactory = bufferManagerFactoryClass.newInstance();
    return (BufferManagerFactory)bufferManagerFactory;
  }

}
