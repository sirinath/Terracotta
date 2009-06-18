package com.tc.modules;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

import com.tc.bundles.BundleSpec;
import com.tc.bundles.OSGiToMaven;
import com.terracottatech.config.Module;
import com.terracottatech.config.Modules;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

public class RepositoryInfo extends ModuleInfoGroup {
  private final File          repoDir;
  private static final String BUNDLE_CATEGORY_MANIFEST_KEY = "Bundle-Category";
  private static final String TIM_CATEGORY_VALUE           = "Terracotta Integration Module";

  public RepositoryInfo(File repoDir) {
    this.repoDir = repoDir;
    if (repoDir.exists() && repoDir.isDirectory()) {
      Modules modules = com.terracottatech.config.Modules.Factory.newInstance();
      modules.setModuleArray(getModules(repoDir));
      ModuleManager.initModules(modules, this);
    }
  }

  public File getRepositoryFile() {
    return repoDir;
  }

  public static Module[] getModules(File repoDir) {
    List moduleList = new ArrayList();
    Iterator jarFileIter = FileUtils.iterateFiles(repoDir, new String[] { "jar" }, true);
    do {
      if (!jarFileIter.hasNext()) break;
      File jarFile = (File) jarFileIter.next();
      if (!jarFile.exists()) continue;
      JarInputStream jis;
      try {
        jis = new JarInputStream(new FileInputStream(jarFile));
      } catch (IOException ioe) {
        continue;
      }
      Manifest manifest = jis.getManifest();
      if (manifest != null) {
        Attributes attrs = manifest.getMainAttributes();
        if (attrs != null) {
          String bundleCategory = attrs.getValue(BUNDLE_CATEGORY_MANIFEST_KEY);
          if (StringUtils.equals(TIM_CATEGORY_VALUE, bundleCategory)) {
            String symbolicName = BundleSpec.getSymbolicName(manifest);
            if (symbolicName != null) {
              Module module = com.terracottatech.config.Module.Factory.newInstance();
              module.setName(OSGiToMaven.artifactIdFromSymbolicName(symbolicName));
              module.setVersion(BundleSpec.getVersion(manifest));
              module.setGroupId(OSGiToMaven.groupIdFromSymbolicName(symbolicName));
              moduleList.add(module);
            }
          }
        }
      }
      IOUtils.closeQuietly(jis);
    } while (true);
    return (Module[]) moduleList.toArray(new Module[0]);
  }
}
