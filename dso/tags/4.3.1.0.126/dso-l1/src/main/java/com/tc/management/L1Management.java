/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.management;

import com.sun.jmx.remote.opt.util.EnvHelp;
import com.tc.exception.TCRuntimeException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L1Dumper;
import com.tc.management.beans.L1MBeanNames;
import com.tc.management.beans.MBeanNames;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.TunnelingMessageConnectionServer;
import com.tc.util.concurrent.SetOnceFlag;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

public class L1Management extends TerracottaManagement {
  private static final TCLogger          logger = TCLogging.getLogger(L1Management.class);

  private final SetOnceFlag              started;
  private final TunnelingEventHandler    tunnelingHandler;
  private final Object                   mBeanServerLock;
  private MBeanServer                    mBeanServer;

  private final L1Info                   l1InfoBean;
  private final L1Dumper                 l1DumpBean;

  private JMXConnectorServer             connServer;

  private volatile boolean               stopped;

  public L1Management(final TunnelingEventHandler tunnelingHandler, final String rawConfigText, final TCClient client) {
    super();

    started = new SetOnceFlag();
    this.tunnelingHandler = tunnelingHandler;

    try {
      l1InfoBean = new L1Info(client, rawConfigText);
      l1DumpBean = new L1Dumper(client, l1InfoBean);
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException(
                                   "Unable to construct one of the L1 MBeans: this is a programming error in one of those beans",
                                   ncmbe);
    }
    mBeanServerLock = new Object();
  }

  public synchronized void stop() throws IOException {
    stopped = true;

    if (connServer != null) {
      try {
        connServer.stop();
      } finally {
        connServer = null;
      }
    }

    ManagementResources mr = new ManagementResources();
    unregisterBeans(mr.getInternalMBeanDomain());
    unregisterBeans(mr.getPublicMBeanDomain());
  }

  private void unregisterBeans(String domain) {
    MBeanServer mbs = mBeanServer;
    if (mbs != null) {
      Set<ObjectName> queryNames;
      try {
        queryNames = mbs.queryNames(new ObjectName(domain + ":*,node=" + tunnelingHandler.getUUID()), null);
      } catch (MalformedObjectNameException e1) {
        throw new RuntimeException(e1);
      }

      for (ObjectName name : queryNames) {
        try {
          mbs.unregisterMBean(name);
        } catch (Exception e) {
          logger.error("error unregistering " + name, e);
        }
      }
    }
  }

  public synchronized void start(final boolean createDedicatedMBeanServer) {
    started.set();

    Thread registrationThread = new Thread(new Runnable() {

      private static final int MAX_ATTEMPTS = 60 * 5;

      @Override
      public void run() {
        try {
          boolean registered = false;
          int attemptCounter = 0;
          while (!registered && attemptCounter++ < MAX_ATTEMPTS) {
            try {
              if (logger.isDebugEnabled()) {
                logger.debug("Attempt #" + (attemptCounter + 1) + " to find the MBeanServer and register the L1 MBeans");
              }
              attemptToRegister(createDedicatedMBeanServer);
              registered = true;
              if (logger.isDebugEnabled()) {
                logger.debug("L1 MBeans registered with the MBeanServer successfully after " + (attemptCounter + 1)
                             + " attempts");
              }
            } catch (InstanceAlreadyExistsException e) {
              logger.error("Exception while registering the L1 MBeans, they already seem to exist in the MBeanServer.",
                           e);
              return;
            } catch (Exception e) {
              // Ignore and try again after 1 second, give the VM a chance to get started
              if (logger.isDebugEnabled()) {
                logger.debug("Caught exception while trying to register L1 MBeans", e);
              }
              try {
                Thread.sleep(1000);
              } catch (InterruptedException ie) {
                new Exception("JMX registration thread interrupted, management beans will not be available", ie)
                    .printStackTrace();
              }
            }
          }
          if (registered) {
            tunnelingHandler.jmxIsReady();
          } else {
            logger.error("Aborted attempts to register management" + " beans after " + (MAX_ATTEMPTS / 60)
                         + " min of trying.");
          }
        } finally {
          if (stopped) {
            try {
              L1Management.this.stop();
            } catch (IOException e) {
              logger.error("Error stopping L1 management from registration thread");
            }
          }
        }
      }
    }, "L1Management JMX registration");
    registrationThread.setDaemon(true);
    registrationThread.start();
  }

  @Override
  public Object findMBean(final ObjectName objectName, final Class mBeanInterface) throws IOException {
    if (objectName.equals(L1MBeanNames.L1INFO_PUBLIC)) return l1InfoBean;
    else {
      synchronized (mBeanServerLock) {
        if (mBeanServer != null) { return findMBean(objectName, mBeanInterface, mBeanServer); }
      }
    }
    return null;
  }

  public L1InfoMBean findL1InfoMBean() {
    return l1InfoBean;
  }

  private void attemptToRegister(final boolean createDedicatedMBeanServer) throws InstanceAlreadyExistsException,
      MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
    synchronized (mBeanServerLock) {
      if (mBeanServer == null) {
        if (createDedicatedMBeanServer) {
          if (logger.isDebugEnabled()) {
            logger.debug("attemptToRegister(): Creating an MBeanServer since explicitly requested");
          }
          mBeanServer = MBeanServerFactory.createMBeanServer();
        } else {
          mBeanServer = getPlatformDefaultMBeanServer();
        }
        addJMXConnectors();
      }
    }

    registerMBeans();
  }

  protected void registerMBeans() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, MalformedObjectNameException {
    registerMBean(l1DumpBean, MBeanNames.L1DUMPER_INTERNAL);
    registerMBean(l1InfoBean, L1MBeanNames.L1INFO_PUBLIC);
  }

  protected void registerMBean(Object bean, ObjectName name) throws InstanceAlreadyExistsException,
      MBeanRegistrationException, NotCompliantMBeanException, MalformedObjectNameException {
    ObjectName modifiedName = TerracottaManagement.addNodeInfo(name, tunnelingHandler.getUUID());
    mBeanServer.registerMBean(bean, modifiedName);
  }

  public MBeanServer getMBeanServer() {
    return mBeanServer;
  }

  private void addJMXConnectors() {

    // This will make the JobExecutor threads in remote JMX idle timeout after 5s (instead of 5 minutes)
    try {
      Class c = Class.forName("com.sun.jmx.remote.opt.util.JobExecutor");
      Method method = c.getMethod("setWaitingTime", Long.TYPE);
      method.setAccessible(true);
      method.invoke(null, 5000L);
    } catch (Exception e) {
      logger.warn("cannot adjust job executor timeout", e);
    }

    JMXServiceURL url = null;
    try {
      // LKC-2990 and LKC-3171: Remove the JMX generic optional logging
      java.util.logging.Logger jmxLogger = java.util.logging.Logger.getLogger("javax.management.remote.generic");
      jmxLogger.setLevel(java.util.logging.Level.OFF);
    } catch (Throwable t) {
      logger.warn("Unable to disable default logging in Sun's JMX package; when Terracotta clients go"
                  + " up/down you may see stack traces printed to the log");
    }
    try {
      final Map environment = new HashMap();
      environment.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));
      ProtocolProvider.addTerracottaJmxProvider(environment);
      environment.put(TunnelingMessageConnectionServer.TUNNELING_HANDLER, tunnelingHandler);
      environment.put(EnvHelp.SERVER_CONNECTION_TIMEOUT, String.valueOf(Long.MAX_VALUE));
      url = new JMXServiceURL("terracotta", "localhost", 0);
      // Normally you should NOT do this in the client, but we have a modified version of jmxremote_optional.jar that
      // uses a daemon thread to wait for connections so we don't hang the client
      connServer = JMXConnectorServerFactory.newJMXConnectorServer(url, environment, mBeanServer);
      connServer.start();
      logger.info("Terracotta JMX connector available at[" + url + "]");
    } catch (Exception e) {
      if (url != null) {
        logger.warn("Unable to start embedded JMX connector for url[" + url + "]", e);
      } else {
        logger.warn("Unable to construct embedded JMX connector URL with params (terracotta, localhost, 0)");
      }
    }
  }

  private MBeanServer getPlatformDefaultMBeanServer() {
    return ManagementFactory.getPlatformMBeanServer();
  }
}
