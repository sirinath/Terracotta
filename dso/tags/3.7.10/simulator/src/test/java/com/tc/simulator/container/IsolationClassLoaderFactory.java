/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.simulator.container;

import com.tc.client.AbstractClientFactory;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.TestClientConfigHelperFactory;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.app.ApplicationConfig;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class IsolationClassLoaderFactory {

  private final static Lock lock = new ReentrantLock();

  private final TestClientConfigHelperFactory  factory;
  private final Class                          applicationClass;
  private final Map                            optionalAttributes;
  private final ConfigVisitor                  configVisitor;
  private final L1ConfigurationSetupManager    l1ConfigManager;
  private final Map                            adapterMap;

  public IsolationClassLoaderFactory(TestClientConfigHelperFactory factory, Class applicationClass,
                                     Map optionalAttributes, L1ConfigurationSetupManager l1ConfigManager,
                                     Map adapterMap) {
    this.factory = factory;
    this.applicationClass = applicationClass;
    this.optionalAttributes = optionalAttributes;
    this.configVisitor = new ConfigVisitor();
    this.l1ConfigManager = l1ConfigManager;
    this.adapterMap = adapterMap;
  }

  public IsolationClassLoader createIsolationClassLoader(String applicationId, ApplicationConfig applicationConfig)
      throws ConfigurationSetupException {
    boolean usesAdapters = false;

    final TCSecurityManager clientSecurityManager;
    if (Boolean.getBoolean("tc.test.runSecure")) {
      if (System.getProperty("SecretProvider.secret") == null) {
        System.setProperty("SecretProvider.secret", "lala");
      }
      lock.lock(); // Stupid hack to let one VM create multiple SecurityManagers...
      try {
        clientSecurityManager = AbstractClientFactory.getFactory().createClientSecurityManager(null);
      } finally {
        lock.unlock();
      }
    } else {
      clientSecurityManager = null;
    }
    IsolationClassLoader classloader = new IsolationClassLoader(
                                                                createClientConfigHelper(),
                                                                new PreparedComponentsFromL2Connection(
                                                                                                       this.l1ConfigManager, clientSecurityManager), clientSecurityManager);
    if (adapterMap != null) {
      if (adapterMap.size() > 0) {
        usesAdapters = true;
      }
      for (Iterator iter = adapterMap.keySet().iterator(); iter.hasNext();) {
        String adapteeName = (String) iter.next();
        Class adapterClass = (Class) adapterMap.get(adapteeName);
        classloader.addAdapter(adapteeName, adapterClass);
      }
    }

    // so the app instance can tell whether it has been adapted
    applicationConfig.setAttribute(applicationId + ApplicationConfig.ADAPTED_KEY, usesAdapters + "");

    return classloader;
  }

  protected DSOClientConfigHelper createClientConfigHelper() throws ConfigurationSetupException {
    DSOClientConfigHelper configHelper = factory.createClientConfigHelper();
    if (optionalAttributes != null && optionalAttributes.size() > 0) {
      this.configVisitor.visit(configHelper, this.applicationClass, this.optionalAttributes);
    } else {
      this.configVisitor.visit(configHelper, this.applicationClass);
    }

    return configHelper;
  }
}