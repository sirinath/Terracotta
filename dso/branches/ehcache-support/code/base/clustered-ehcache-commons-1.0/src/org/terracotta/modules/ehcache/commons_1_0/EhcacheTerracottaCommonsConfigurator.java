package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ByteCodeUtil;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

import java.net.MalformedURLException;
import java.net.URL;

public class EhcacheTerracottaCommonsConfigurator extends TerracottaConfiguratorModule {
  static final String CACHE_CLASS_NAME_DOTS = "net.sf.ehcache.Cache";
  static final String CACHETC_CLASS_NAME_DOTS = "net.sf.ehcache.CacheTC";
  static final String MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS = "net.sf.ehcache.store.MemoryStoreEvictionPolicy";
  static final String MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS = "net.sf.ehcache.store.MemoryStoreEvictionPolicyTC";
  
  static final String BUNDLE_NAME = "org.terracotta.modules.clustered_ehcache_commons_1.0";

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
    String bundleJarUrl = "jar:"+bundle.getLocation()+"!/";
    String bundleUrl = "";
    try {
      bundleUrl = bundleJarUrl+ByteCodeUtil.classNameToFileName(CACHETC_CLASS_NAME_DOTS);
      configHelper.addClassReplacement(CACHE_CLASS_NAME_DOTS, CACHETC_CLASS_NAME_DOTS, new URL(bundleUrl));
      bundleUrl = bundleJarUrl+ByteCodeUtil.classNameToFileName(MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS);
      configHelper.addClassReplacement(MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS,
                                       MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS, new URL(bundleUrl));
    } catch (MalformedURLException e) {
      throw new RuntimeException("Unexpected error while constructing the URL '"+bundleUrl+"' during the instrumentation configuration of the bunde '"+context.getBundle().getSymbolicName()+"'.", e);
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
}