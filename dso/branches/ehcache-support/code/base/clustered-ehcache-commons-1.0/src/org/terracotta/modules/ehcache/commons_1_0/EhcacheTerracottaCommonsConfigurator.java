package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.object.loaders.BytecodeProvider;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class EhcacheTerracottaCommonsConfigurator extends TerracottaConfiguratorModule implements BytecodeProvider {
  static final String CACHE_CLASS_NAME_DOTS = "net.sf.ehcache.Cache";
  static final String CACHETC_CLASS_NAME_DOTS = "net.sf.ehcache.CacheTC";
  static final String MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS = "net.sf.ehcache.store.MemoryStoreEvictionPolicy";
  static final String MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS = "net.sf.ehcache.store.MemoryStoreEvictionPolicyTC";
  
  static final String BUNDLE_NAME = "org.terracotta.modules.clustered_ehcache_commons_1.0";
  
  private Map exportedClasses;

  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
		super.addInstrumentation(context, configHelper);

    // find the bundle that contains the replacement classes
    Bundle[] bundles = context.getBundles();
    Bundle bundle = null;
    for (int i = 0; i < bundles.length; i++) {
      if (BUNDLE_NAME.equals(bundles[i].getSymbolicName())) {
        bundle = bundles[i];
        break;
      }
    }  
    if (null == bundle) {
      throw new RuntimeException("Couldn't find bundle with symbolic name '"+BUNDLE_NAME+"' during the instrumentation configuration of the bunde '"+context.getBundle().getSymbolicName()+"'.");
    }
    
    // setup the replacement classes
    addClassReplacement(configHelper, bundle, CACHE_CLASS_NAME_DOTS, CACHETC_CLASS_NAME_DOTS);
    addClassReplacement(configHelper, bundle, MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS, MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS);

    // setup up the bytecode provider
    Map exported = new HashMap();
    addExportedBundleClass(bundle, exported, "net.sf.ehcache.store.TimeExpiryMemoryStore");
    addExportedBundleClass(bundle, exported, "net.sf.ehcache.store.TimeExpiryMemoryStore$SpoolingTimeExpiryMap");
    addExportedTcJarClass(exported, "com.tcclient.ehcache.TimeExpiryMap");
    addExportedTcJarClass(exported, "com.tcclient.cache.CacheData");
    addExportedTcJarClass(exported, "com.tcclient.cache.CacheDataStore");
    addExportedTcJarClass(exported, "com.tcclient.cache.CacheDataStore$CacheEntryInvalidator");
    addExportedTcJarClass(exported, "com.tcclient.cache.Expirable");
    addExportedTcJarClass(exported, "com.tcclient.cache.Lock");
    addExportedTcJarClass(exported, "com.tcclient.cache.Timestamp");
    this.exportedClasses = Collections.unmodifiableMap(exported);
    
    Iterator exported_it = this.exportedClasses.keySet().iterator();
    while (exported_it.hasNext()) {
      configHelper.addBytecodeProvider((String)exported_it.next(), this);
    }

    // perform the rest of the configuration
    configHelper.addIncludePattern("com.tcclient.cache.*", false, false, false);
    configHelper.addIncludePattern("com.tcclient.ehcache.*", false, false, false);
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheDataStore");
    spec.setHonorTransient(true);
    spec.setCallMethodOnLoad("initialize");
    spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheData");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
	}
  
  private String getBundleJarUrl(Bundle bundle) {
    return "jar:"+bundle.getLocation()+"!/";    
  }

  private void addClassReplacement(final StandardDSOClientConfigHelper configHelper, final Bundle bundle, final String originalClassName, final String replacementClassName) {
    String url = getBundleJarUrl(bundle)+ByteCodeUtil.classNameToFileName(replacementClassName);
    try {
      configHelper.addClassReplacement(originalClassName, replacementClassName, new URL(url));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Unexpected error while constructing the URL '"+url+"'", e);
    }
  }

  private void addExportedBundleClass(final Bundle bundle, final Map exported, final String classname) {
    String url = getBundleJarUrl(bundle)+ByteCodeUtil.classNameToFileName(classname);
    try {
      exported.put(classname, new URL(url));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Unexpected error while constructing the URL '"+url+"'", e);
    }
  }

  private void addExportedTcJarClass(final Map exported, final String classname) {
    exported.put(classname, TerracottaConfiguratorModule.class.getClassLoader().getResource(ByteCodeUtil.classNameToFileName(classname)));
  }

  public byte[] __tc_getBytecodeForClass(String className) {
    URL resource = (URL)exportedClasses.get(className);
    if (null == resource) {
      return null;
    }
    try {
      return ByteCodeUtil.getBytesForInputstream(resource.openStream());
    } catch (IOException e) {
      throw new RuntimeException("Unexpected IO error while obtaining the bytes for class '"+className+"' from the resource '"+resource.toExternalForm()+"'.", e);
    }
  }
}