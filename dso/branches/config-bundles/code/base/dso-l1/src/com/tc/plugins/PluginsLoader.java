/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.plugins;

import org.apache.xmlbeans.XmlException;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.config.ConfigLoader;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.PluginSpec;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Plugin;
import com.terracottatech.config.Plugins;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Dictionary;
import java.util.Hashtable;

public class PluginsLoader {
  private static final TCLogger logger = TCLogging.getLogger(PluginsLoader.class);

  private PluginsLoader() {
    // cannot be instantiated
  }

  public static void initPlugins(final DSOClientConfigHelper configHelper, final boolean forBootJar) {
    EmbeddedOSGiRuntime osgiRuntime;

    final Plugins plugins = configHelper.getPluginsForInitialization();
    if (plugins != null && plugins.sizeOfPluginArray() > 0) {
      try {
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(plugins);
      } catch (Exception e) {
        throw new RuntimeException("Unable to create runtime for plugins", e);
      }
      try {
        initPlugins(osgiRuntime, configHelper, plugins.getPluginArray());
        if (!forBootJar) {
          getPluginsCustomApplicatorSpecs(osgiRuntime, configHelper);
        }
      } catch (BundleException be1) {
        try {
          osgiRuntime.shutdown();
        } catch (BundleException be2) {
          logger.error("Error shutting down plugin runtime", be2);
        }
        throw new RuntimeException("Exception initializing plugins", be1);
      } catch (InvalidSyntaxException be1) {
        try {
          osgiRuntime.shutdown();
        } catch (BundleException be2) {
          logger.error("Error shutting down plugin runtime", be2);
        }
        throw new RuntimeException("Exception initializing plugins", be1);
      } finally {
        if (forBootJar) {
          try {
            osgiRuntime.shutdown();
          } catch (BundleException be2) {
            logger.error("Error shutting down plugin runtime", be2);
          }
        }
      }
    } else {
      osgiRuntime = null;
    }
  }

  private static void initPlugins(final EmbeddedOSGiRuntime osgiRuntime, final DSOClientConfigHelper configHelper,
                                  final Plugin[] plugins) throws BundleException {
    for (int pos = 0; pos < plugins.length; ++pos) {
      String bundle = plugins[pos].getName() + "-" + plugins[pos].getVersion();
      logger.info("Installing OSGI bundle " + bundle);
      osgiRuntime.installBundle(plugins[pos].getName(), plugins[pos].getVersion());
      logger.info("Installation of OSGI bundle " + bundle + " successful");
    }
    if (configHelper instanceof StandardDSOClientConfigHelper) {
      final Dictionary serviceProps = new Hashtable();
      serviceProps.put(Constants.SERVICE_VENDOR, "Terracotta, Inc.");
      serviceProps.put(Constants.SERVICE_DESCRIPTION, "Main point of entry for programmatic access to"
                                                      + " the Terracotta bytecode instrumentation");
      osgiRuntime.registerService(configHelper, serviceProps);
    }
    for (int pos = 0; pos < plugins.length; ++pos) {
      String name = plugins[pos].getName();
      String version = plugins[pos].getVersion();

      osgiRuntime.startBundle(name, version);

      Bundle bundle = osgiRuntime.getBundle(name, version);
      if (bundle != null) {
        loadConfiguration(configHelper, bundle);
      }
    }
  }

  private static void getPluginsCustomApplicatorSpecs(final EmbeddedOSGiRuntime osgiRuntime,
                                                      final DSOClientConfigHelper configHelper)
      throws InvalidSyntaxException {
    ServiceReference[] serviceReferences = osgiRuntime.getAllServiceReferences(PluginSpec.class.getName(),
                                                                               null);
    if (serviceReferences == null) { return; }
    PluginSpec[] pluginSpecs = new PluginSpec[serviceReferences.length];
    for (int i = 0; i < serviceReferences.length; i++) {
      pluginSpecs[i] = (PluginSpec) osgiRuntime.getService(serviceReferences[i]);
      osgiRuntime.ungetService(serviceReferences[i]);
    }
    configHelper.setPluginSpecs(pluginSpecs);
  }

  private static void loadConfiguration(final DSOClientConfigHelper configHelper, final Bundle bundle) {
    String config = (String) bundle.getHeaders().get("Terracotta-Configuration");
    if (config == null) {
      config = "terracotta.xml";
    }

    URL configUrl = bundle.getEntry(config);
    if (configUrl == null) { return; }

    InputStream is = null;
    try {
      is = configUrl.openStream();
      DsoApplication application = DsoApplication.Factory.parse(is);
      if (application != null) {
        ConfigLoader loader = new ConfigLoader(configHelper, logger);
        loader.loadDsoConfig(application);
        // loader.loadSpringConfig(application.getSpring());
      }
    } catch (IOException e) {
      logger.warn("Unable to read configuration from " + configUrl, e);
    } catch (XmlException e) {
      logger.warn("Unable to parse configuration from " + configUrl, e);
    } catch (ConfigurationSetupException e) {
      logger.warn("Unable to load configuration from " + configUrl, e);
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException ex) {
          // ignore
        }
      }
    }
  }

}
