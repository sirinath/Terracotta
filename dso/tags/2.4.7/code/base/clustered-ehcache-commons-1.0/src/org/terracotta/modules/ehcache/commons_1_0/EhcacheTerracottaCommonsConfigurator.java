package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.ConfigLockLevel;
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
    addExportedBundleClass(configHelper, thisBundle, "org.terracotta.modules.ehcache.commons_1_0.util.Util");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$EntriesIterator");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$EntrySetWrapper");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$EntryWrapper");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$KeySetWrapper");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$KeysIterator");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$ValuesCollectionWrapper");
    addExportedTcJarClass(configHelper, "com.tcclient.ehcache.TimeExpiryMap$ValuesIterator");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheData");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheDataStore");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheDataStore$CacheEntryInvalidator");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheDataStore$CacheInvalidationTimer");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Expirable");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Lock");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.Timestamp");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.GlobalKeySet");
    addExportedTcJarClass(configHelper, "com.tcclient.cache.CacheParticipants");
    
    // explicitly excluding autolocking
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.CacheData.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.CacheDataStore.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.CacheParticipants.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.Lock.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.cache.Timestamp.*(..)");
    configHelper.addAutoLockExcludePattern("* com.tcclient.ehcache..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.TimeExpiryMemoryStore.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.TimeExpiryMemoryStore$SpoolingTimeExpiryMap.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Cache.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Ehcache.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Statistics.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.Status.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.bootstrap.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.config.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.asynchronous.*.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.blocking.*.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.concurrent.ConcurrencyUtil.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.constructs.concurrent.Sync.*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.distribution.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.event.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.hibernate.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.jcache.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.management.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.store.*..*(..)");
    configHelper.addAutoLockExcludePattern("* net.sf.ehcache.util.*..*(..)");
    
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.acquire(..)", ConfigLockLevel.WRITE);
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.attempt(..)", ConfigLockLevel.WRITE);
    configHelper.addAutolock("* net.sf.ehcache.constructs.concurrent.Mutex.release(..)", ConfigLockLevel.WRITE);

    // perform the rest of the configuration
    configHelper.addIncludePattern("com.tcclient.cache.*", false, false, false);
    configHelper.addIncludePattern("com.tcclient.ehcache.*", false, false, false);
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheDataStore");
    spec.setHonorTransient(true);
    spec.setCallMethodOnLoad("initialize");
    spec.addDistributedMethodCall("stopInvalidatorThread", "()V", false);
    spec = configHelper.getOrCreateSpec("com.tcclient.cache.CacheData");
    spec.setCallConstructorOnLoad(true);
    spec.setHonorTransient(true);
    
    ClassAdapterFactory factory = new EhcacheMemoryStoreAdapter();
    spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);
    
    // autolocking
    configHelper.addAutolock(" * com.tcclient.cache.GlobalKeySet.*(..)", ConfigLockLevel.WRITE);
    configHelper.addAutolock(" * com.tcclient.cache.GlobalKeySet.allGlobalKeys(..)", ConfigLockLevel.READ);
//    
//    configHelper.addAutolock(" * com.tcclient.cache.CacheParticipants.*(..)", ConfigLockLevel.WRITE);
//    configHelper.addAutolock(" * com.tcclient.cache.CacheParticipants.getCacheParticipants(..)", ConfigLockLevel.READ);
//    configHelper.addAutolock(" * com.tcclient.cache.CacheParticipants.getNodeId(..)", ConfigLockLevel.READ);
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