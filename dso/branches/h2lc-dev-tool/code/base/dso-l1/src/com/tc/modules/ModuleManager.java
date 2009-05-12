/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.modules;

import org.apache.commons.io.IOUtils;
import org.apache.xmlbeans.XmlError;
import org.apache.xmlbeans.XmlOptions;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleException;

import com.tc.bundles.EmbeddedOSGiEventHandler;
import com.tc.bundles.EmbeddedOSGiRuntime;
import com.tc.bundles.Resolver;
import com.tc.bundles.ResolverUtils;
import com.tc.object.util.JarResourceLoader;
import com.tc.plugins.ModulesLoader;
import com.terracottatech.config.DsoApplication;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Dictionary;
import java.util.Hashtable;

public class ModuleManager {
  private final File              fInstallRoot;
  private final RepositoryInfo    fDefaultRepositoryInfo;

  private static final Module[]   EMPTY_MODULES_ARRAY   = {};
  private static final String     CONFIG_SERVICE_NAME   = "com.tc.object.config.StandardDSOClientConfigHelper";
  private static final Object     CONFIG_SERVICE_OBJECT = new FakeDSOClientConfigHelper();
  private static final Dictionary CONFIG_SERVICE_PROPS  = new Hashtable();

  public ModuleManager() {
    this(new File(System.getProperty("tc.install-root")));
  }

  public ModuleManager(File installRoot) {
    fInstallRoot = installRoot;
    fDefaultRepositoryInfo = new RepositoryInfo(getDefaultRepoLocation());
  }

  public RepositoryInfo getDefaultRepositoryInfo() {
    return fDefaultRepositoryInfo;
  }

  public File getDefaultRepoLocation() {
    return new File(fInstallRoot, "modules");
  }

  public static void initModules(final Modules modules, final ModuleInfoGroup moduleInfoGroup) {
    EmbeddedOSGiRuntime osgiRuntime = null;

    try {
      for (Module module : modules.getModuleArray()) {
        Modules tmpModules = (Modules) modules.copy();
        tmpModules.setModuleArray(new Module[] { module });
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(tmpModules);
        final Resolver resolver = new Resolver(ResolverUtils.urlsToStrings(osgiRuntime.getRepositories()));
        ModuleInfo moduleInfo = moduleInfoGroup.getOrAdd(module);

        // force default modules to be resolved
        resolver.resolve(EMPTY_MODULES_ARRAY);

        try {
          moduleInfo.setLocation(resolver.resolve(module));
          final File[] locations = resolver.getResolvedFiles();
          for (File location : locations) {
            osgiRuntime.installBundle(location.toURL());
          }
        } catch (BundleException be) {
          moduleInfo.setError(be);
        }

        osgiRuntime.shutdown();
      }

      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modules);
      final Resolver resolver = new Resolver(ResolverUtils.urlsToStrings(osgiRuntime.getRepositories()));

      resolver.resolve(modules.getModuleArray());

      final File[] locations = resolver.getResolvedFiles();
      for (File location : locations) {
        try {
          osgiRuntime.installBundle(location.toURL());
        } catch (BundleException be) {
          be.printStackTrace();
        }
      }

      osgiRuntime.registerService(CONFIG_SERVICE_NAME, CONFIG_SERVICE_OBJECT, CONFIG_SERVICE_PROPS);

      URL[] urls = new URL[locations.length];
      for (int i = 0; i < locations.length; i++) {
        urls[i] = locations[i].toURL();
      }
      osgiRuntime.startBundles(urls, new EmbeddedOSGiEventHandler() {
        public void callback(final Object payload) throws BundleException {
          if (payload instanceof Bundle) {
            loadModuleConfiguration((Bundle) payload, moduleInfoGroup);
          }
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
    } finally {
      if (osgiRuntime != null) {
        osgiRuntime.shutdown();
      }
    }
  }

  private static void loadModuleConfiguration(final Bundle bundle, final ModuleInfoGroup moduleInfoGroup)
      throws BundleException {
    ModuleInfo moduleInfo = moduleInfoGroup.associateBundle(bundle);
    if (moduleInfo == null) return;

    final String[] paths = ModulesLoader.getConfigPath(bundle);
    for (int i = 0; i < paths.length; i++) {
      final String configPath = paths[i];
      InputStream is = null;

      try {
        is = JarResourceLoader.getJarResource(new URL(bundle.getLocation()), configPath);
      } catch (MalformedURLException murle) {
        moduleInfo.setError(new BundleException("Unable to create URL from: " + bundle.getLocation(), murle));
      } catch (IOException ioe) {
        moduleInfo.setError(new BundleException("Unable to extract " + configPath + " from URL: "
                                                + bundle.getLocation(), ioe));
      }

      if (is == null) {
        continue;
      }

      try {
        final DsoApplication application = DsoApplication.Factory.parse(is);
        if (application != null) {
          ArrayList errors = new ArrayList();
          XmlOptions opts = new XmlOptions();
          opts.setErrorListener(errors);
          if (!application.validate(opts)) {
            StringBuffer sb = new StringBuffer("Bundle XML fragment invalid");
            if (errors.size() > 0) {
              sb.append(": ");
              Object error = errors.get(0);
              if (error instanceof XmlError) {
                sb.append(((XmlError) error).getMessage());
              } else {
                sb.append(error.toString());
              }
              if (errors.size() > 1) {
                int remainingErrors = errors.size() - 1;
                sb.append(MessageFormat.format(", ({0} more)", remainingErrors));
              }
            }
            moduleInfo.setError(new BundleException(sb.toString()));
          }
          moduleInfoGroup.setModuleApplication(moduleInfo, application);
        }
      } catch (Exception e) {
        moduleInfo.setError(new BundleException("Failed to parse bundle XML fragment", e));
      } finally {
        IOUtils.closeQuietly(is);
      }
    }
  }

}
