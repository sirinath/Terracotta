/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.simulator.app;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.loaders.IsolationClassLoader;
import com.tc.simulator.listener.ListenerProvider;

import java.lang.reflect.Constructor;

public class DSOApplicationBuilder implements ApplicationBuilder {

  private final ApplicationConfig    applicationConfig;
  private final ClassLoader classloader;

  private Class                      applicationClass;
  private Constructor                applicationConstructor;

  public DSOApplicationBuilder(DSOClientConfigHelper config, ApplicationConfig applicationConfig,
                               PreparedComponentsFromL2Connection components) {

    this(applicationConfig, new IsolationClassLoader(config, components));
  }

  public DSOApplicationBuilder(ApplicationConfig applicationConfig,
                               ClassLoader classloader) {
    this.applicationConfig = applicationConfig;
    this.classloader = classloader;
  }
  
  // XXX:: Adding more debugs to figure out the OOME in Primitive ArrayTest.
  TCLogger logger = TCLogging.getLogger(DSOApplicationBuilder.class);
  public Application newApplication(String applicationId, ListenerProvider listenerProvider)
      throws ApplicationInstantiationException {
    try {
      logger.info("Before initializing Class Loader...");
      initializeClassLoader();
      logger.info("After initializing Class Loader...");
      Class applicationConfigClass = classloader.loadClass(ApplicationConfig.class.getName());
      Class listenerProviderClass = classloader.loadClass(ListenerProvider.class.getName());
      this.applicationClass = this.classloader.loadClass(this.applicationConfig.getApplicationClassname());
      this.applicationConstructor = this.applicationClass.getConstructor(new Class[] { String.class,
          applicationConfigClass, listenerProviderClass });
      logger.info("Before new Instance is created...");
      return (Application) this.applicationConstructor.newInstance(new Object[] { applicationId,
          this.applicationConfig, listenerProvider });
    } catch (Throwable t) {
      t.printStackTrace();
      throw new ApplicationInstantiationException(t);
    }

  }

  private void initializeClassLoader() {
    if (this.classloader instanceof IsolationClassLoader) {
      ((IsolationClassLoader)this.classloader).init();
    }
  }

  public ClassLoader getContextClassLoader() {
    return classloader;
  }

}
