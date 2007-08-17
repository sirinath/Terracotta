package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public abstract class EhcacheTerracottaCommonsConfigurator extends TerracottaConfiguratorModule implements IConstants {

  protected void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
		super.addInstrumentation(context, configHelper);

    // find the bundle that contains the replacement classes
    Bundle bundle = getExportedBundle(context, getExportedBundleName());
    Bundle thisBundle = getExportedBundle(context, COMMON_EHCACHE_BUNDLE_NAME);
    if (null == bundle) {
      throw new RuntimeException("Couldn't find bundle with symbolic name '"+COMMON_EHCACHE_BUNDLE_NAME+"' during the instrumentation configuration of the bunde '"+context.getBundle().getSymbolicName()+"'.");
    }
    
    // setup the replacement classes
    addClassReplacement(configHelper, bundle, CACHE_CLASS_NAME_DOTS, CACHETC_CLASS_NAME_DOTS);
    addClassReplacement(configHelper, bundle, MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS, MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS);

    // setup the class resources
    addExportedBundleClass(configHelper, thisBundle, "net.sf.ehcache.store.TimeExpiryMemoryStore");
    addExportedBundleClass(configHelper, thisBundle, "net.sf.ehcache.store.TimeExpiryMemoryStore$SpoolingTimeExpiryMap");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheData");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheDataStore");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheDataStore$CacheEntryInvalidator");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Expirable");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Lock");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Timestamp");

    // perform the rest of the configuration
    configHelper.addIncludePattern("com.tcclient.cache.*", false, false, false);
    configHelper.addIncludePattern("com.tcclient.ehcache.*", false, false, false);
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheDataStore");
    spec.setHonorTransient(true);
    spec.setCallMethodOnLoad("initialize");
    spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheData");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
    
    ClassAdapterFactory factory = new EhcacheMemoryStoreAdapter();
    spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);
	}
  
  protected Bundle getExportedBundle(final BundleContext context, String targetBundleName) {
    // find the bundle that contains the replacement classes
    Bundle[] bundles = context.getBundles();
    Bundle bundle = null;
    for (int i = 0; i < bundles.length; i++) {
      if (targetBundleName.equals(bundles[i].getSymbolicName())) {
        bundle = bundles[i];
        break;
      }
    }  
    return bundle;
  }
  
  protected abstract String getExportedBundleName();
}