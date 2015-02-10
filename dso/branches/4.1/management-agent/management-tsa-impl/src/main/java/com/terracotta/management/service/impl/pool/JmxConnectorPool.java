/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl.pool;

import javax.management.MalformedObjectNameException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ludovic Orban
 */
public class JmxConnectorPool {

  private final Map<String, JMXConnector> connectorsMap = new ConcurrentHashMap<String, JMXConnector>();
  private final Object connectorsLock = new Object();
  private final String urlPattern;

  public JmxConnectorPool(String urlPattern) {
    this.urlPattern = urlPattern;
  }

  public JMXConnector getConnector(String host, int port) throws IOException, InterruptedException, MalformedObjectNameException {
    String url = MessageFormat.format(urlPattern, host, String.valueOf(port));
    Map<String, Object> env = createJmxConnectorEnv(host, port);
    return getConnector(url, env);
  }

  private JMXConnector getConnector(String url, Map<String, Object> env) throws IOException, InterruptedException, MalformedObjectNameException {
    JMXConnector jmxConnector = connectorsMap.get(url);

    if (jmxConnector == null) {
      synchronized (connectorsLock) {
        jmxConnector = connectorsMap.get(url);
        if (jmxConnector == null) {
          try {
            jmxConnector = JMXConnectorFactory.connect(new JMXServiceURL(url), env);
            connectorsMap.put(url, jmxConnector);
          } catch (IOException ioe) {
            // cannot create connector, server is down
            throw ioe;
          }
        }
      }
    }

    try {
      // test connector
      jmxConnector.getMBeanServerConnection().getMBeanCount();
      return new JMXConnectorHolder(jmxConnector);
    } catch (IOException ioe) {
      // broken connector, remove it from the pool
      synchronized (connectorsLock) {
        JMXConnector removed = connectorsMap.remove(url);
        if (removed != null) {
          try {
            removed.close();
          } catch (IOException ioe2) {
            // ignore
          }
        }
      }
      throw ioe;
    }
  }

  protected Map<String, Object> createJmxConnectorEnv(String host, int port) {
    return null;
  }

  public void shutdown() {
    synchronized (connectorsLock) {
      Collection<JMXConnector> values = connectorsMap.values();
      for (JMXConnector pooledJmxConnector : values) {
        try {
          pooledJmxConnector.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
      connectorsMap.clear();
    }
  }

}
