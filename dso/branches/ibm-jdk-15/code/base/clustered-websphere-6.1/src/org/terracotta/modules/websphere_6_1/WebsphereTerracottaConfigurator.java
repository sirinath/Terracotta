package org.terracotta.modules.websphere_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class WebsphereTerracottaConfigurator extends TerracottaConfiguratorModule {

  protected final void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("org.eclipse.osgi.internal.baseadaptor.DefaultClassLoader",
                                  new DefaultClassLoaderAdapter());
    // configHelper.addCustomAdapter("com.ibm.ws.classloader.JarClassLoader", new JarClassLoaderAdapter());
    // configHelper.addCustomAdapter("com.ibm.ws.classloader.ClassGraph", new ClassGraphAdapter());
    configHelper.addCustomAdapter("com.ibm.wsspi.bootstrap.WSLauncher", new WSLauncherAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.httpsession.SessionContext",
                                  new SessionContextClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.WebAppFilterManager",
                                  new WebAppFilterManagerClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.webapp.WebApp", new WebAppClassAdapter());
  }

  protected final void registerModuleSpec(final BundleContext context) {
    // Nothing doing here
  }

}
