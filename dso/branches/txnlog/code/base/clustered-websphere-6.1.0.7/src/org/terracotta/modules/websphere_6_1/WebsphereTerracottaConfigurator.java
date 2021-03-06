package org.terracotta.modules.websphere_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;


public final class WebsphereTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context) {
  	// Without this exclude, WAS 6.1.0.0 will fail to start with StackOverflowError (CDV-429)
    configHelper.addPermanentExcludePattern("org.eclipse.osgi.framework.internal.core.FrameworkSecurityManager");

    configHelper.addCustomAdapter("com.ibm.ws.classloader.JarClassLoader", new JarClassLoaderAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.classloader.ClassGraph", new ClassGraphAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.WebAppFilterManager",
                                  new WebAppFilterManagerClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.facade.ServletContextFacade",
                                  new ServletContextFacadeAdapater());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.httpsession.SessionContext", new SessionContextAdapter());
  }

}
