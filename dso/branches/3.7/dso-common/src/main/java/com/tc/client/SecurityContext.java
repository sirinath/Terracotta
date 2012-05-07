/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.client;

import com.tc.config.schema.SecurityConfig;
import com.tc.net.core.BufferManagerFactory;

import java.lang.reflect.InvocationTargetException;

/**
 * @author Ludovic Orban
 */
public class SecurityContext {

  private static final String SSL_BUFFER_MANAGER_CLASS_NAME = "com.tc.net.core.ssl.SSLBufferManagerFactory";

  private final BufferManagerFactory bufferManagerFactory;

  public SecurityContext(SecurityConfig security, String keyPairAlias) throws Exception {
    this.bufferManagerFactory = createBufferManagerFactory(security, keyPairAlias);
  }

  private BufferManagerFactory createBufferManagerFactory(SecurityConfig security, String keyPairAlias) throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
    Class<?> bufferManagerFactoryClass = Class.forName(SSL_BUFFER_MANAGER_CLASS_NAME);
    Object bufferManagerFactory = bufferManagerFactoryClass.getConstructor(SecurityConfig.class, String.class).newInstance(security, keyPairAlias);
    return (BufferManagerFactory)bufferManagerFactory;
  }

  public BufferManagerFactory getBufferManagerFactory() {
    return bufferManagerFactory;
  }
}
