package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.terracotta.modules.ehcache.commons_1_0.EhcacheTerracottaCommonsConfigurator;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class EhcacheTerracottaConfigurator extends EhcacheTerracottaCommonsConfigurator {
  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
    super.addInstrumentation(context, configHelper);
    Bundle bundle = getExportedBundle(context);
    
    addClassReplacement(configHelper, bundle, MEMORYSTORE_CLASS_NAME_DOTS, MEMORYSTORETC_CLASS_NAME_DOTS);
  }
  
  protected String getExportedBundleName() {
    return "org.terracotta.modules.clustered_ehcache_1.2.4";
  }
}