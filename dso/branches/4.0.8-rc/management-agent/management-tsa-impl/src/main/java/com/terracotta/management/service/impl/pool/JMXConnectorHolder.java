/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.pool;

import java.io.IOException;
import java.util.Map;

import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnector;
import javax.security.auth.Subject;

/**
 * @author Ludovic Orban
 */
class JMXConnectorHolder implements JMXConnector {

  private final JMXConnector delegate;
  private final Object lock = new Object();

  private boolean closed = false;

  JMXConnectorHolder(JMXConnector delegate) {
    this.delegate = delegate;
  }

  @Override
  public void connect() throws IOException {
    synchronized (lock) {
      assertNotClosed();
      delegate.connect();
    }
  }

  @Override
  public void connect(Map<String, ?> env) throws IOException {
    synchronized (lock) {
      assertNotClosed();
      delegate.connect(env);
    }
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection() throws IOException {
    synchronized (lock) {
      assertNotClosed();
      return delegate.getMBeanServerConnection();
    }
  }

  @Override
  public MBeanServerConnection getMBeanServerConnection(Subject delegationSubject) throws IOException {
    synchronized (lock) {
      assertNotClosed();
      return delegate.getMBeanServerConnection(delegationSubject);
    }
  }

  @Override
  public void close() throws IOException {
    synchronized (lock) {
      closed = true;
    }
  }

  @Override
  public void addConnectionNotificationListener(NotificationListener listener, NotificationFilter filter, Object handback) {
    synchronized (lock) {
      assertNotClosed();
      delegate.addConnectionNotificationListener(listener, filter, handback);
    }
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener listener) throws ListenerNotFoundException {
    synchronized (lock) {
      assertNotClosed();
      delegate.removeConnectionNotificationListener(listener);
    }
  }

  @Override
  public void removeConnectionNotificationListener(NotificationListener l, NotificationFilter f, Object handback) throws ListenerNotFoundException {
    synchronized (lock) {
      assertNotClosed();
      delegate.removeConnectionNotificationListener(l, f, handback);
    }
  }

  @Override
  public String getConnectionId() throws IOException {
    synchronized (lock) {
      assertNotClosed();
      return delegate.getConnectionId();
    }
  }

  private void assertNotClosed() {
    if (closed) {
      throw new RuntimeException("Connector is closed");
    }
  }
}
