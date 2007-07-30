package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.ehcache.commons_1_0.EhcacheTerracottaCommonsConfigurator;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public final class EhcacheTerracottaConfigurator extends EhcacheTerracottaCommonsConfigurator implements IConstants {
  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
    super.addInstrumentation(context, configHelper);
    
    ClassAdapterFactory factory = new EhcacheMemoryStoreAdapter();
    TransparencyClassSpec spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);
  }
}