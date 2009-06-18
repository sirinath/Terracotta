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
import com.tc.util.Assert;
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
import java.util.Properties;

public class ModuleManager {
  private final File           fInstallRoot;
  private final RepositoryInfo fDefaultRepositoryInfo;

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
      Modules modulesCopy = (Modules) modules.copy();
      Module[] origModules = modulesCopy.getModuleArray();

      for (Module origModule : origModules) {
        Modules tmpModules = (Modules) modulesCopy.copy();
        tmpModules.setModuleArray(new Module[] { origModule });
        osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(tmpModules);
        String[] repositories = ResolverUtils.urlsToStrings(osgiRuntime.getRepositories());
        final Resolver resolver = new Resolver(repositories);
        Module[] allModules = tmpModules.getModuleArray();
        ModuleInfo origModuleInfo = moduleInfoGroup.getOrAdd(origModule);

        for (Module module : allModules) {
          ModuleInfo moduleInfo = moduleInfoGroup.getOrAdd(module);
          try {
            moduleInfo.setLocation(resolver.resolve(module));
            final File[] locations = resolver.getResolvedFiles();
            for (File location : locations) {
              osgiRuntime.installBundle(location.toURL());
            }
          } catch (BundleException be) {
            moduleInfo.setError(be);
            origModuleInfo.setError(be);
          }
        }
        osgiRuntime.shutdown();
      }

      osgiRuntime = EmbeddedOSGiRuntime.Factory.createOSGiRuntime(modulesCopy);
      final Resolver resolver = new Resolver(ResolverUtils.urlsToStrings(osgiRuntime.getRepositories()));
      Module[] allModules = modulesCopy.getModuleArray();

      for (Module module : allModules) {
        try {
          resolver.resolve(module);
        } catch (BundleException be) {
          /**/
        }
      }

      final File[] locations = resolver.getResolvedFiles();
      for (File location : locations) {
        try {
          osgiRuntime.installBundle(location.toURL());
        } catch (BundleException be) {
          /**/
        }
      }

      osgiRuntime.registerService("com.tc.object.config.StandardDSOClientConfigHelper",
                                  new FakeDSOClientConfigHelper(), new Properties());

      URL[] urls = new URL[locations.length];
      for (int i = 0; i < locations.length; i++) {
        urls[i] = locations[i].toURL();
      }
      osgiRuntime.startBundles(urls, new EmbeddedOSGiEventHandler() {
        public void callback(final Object payload) throws BundleException {
          Assert.assertTrue(payload instanceof Bundle);
          Bundle bundle = (Bundle) payload;
          if (bundle != null) {
            ModuleManager.loadModuleConfiguration(bundle, moduleInfoGroup);
          }
        }
      });
    } catch (Exception e) {
      /**/
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

    for (String configPath : ModulesLoader.getConfigPath(bundle)) {
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
