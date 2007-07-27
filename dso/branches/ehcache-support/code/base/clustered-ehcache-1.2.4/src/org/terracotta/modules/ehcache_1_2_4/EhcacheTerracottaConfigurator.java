package org.terracotta.modules.ehcache_1_2_4;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.ehcache.commons_1_0.EhcacheTerracottaCommonsConfigurator;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public final class EhcacheTerracottaConfigurator extends EhcacheTerracottaCommonsConfigurator implements IConstants {

//  public void start(final BundleContext context) throws Exception {
//    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
//    if (configHelperRef != null) {
//      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
//          .getService(configHelperRef);
//      addEhcacheInstrumentation(configHelper);
//      context.ungetService(configHelperRef);
//    } else {
//      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
//                                + " service to be registered, was unable to find it");
//    }
//  }
  
  protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
    super.addInstrumentation(context, configHelper);
    
    ClassAdapterFactory factory = new EhcacheMemoryStoreAdapter();
    TransparencyClassSpec spec = configHelper.getOrCreateSpec(MEMORYSTORE_CLASS_NAME_DOTS);
    spec.setCustomClassAdapter(factory);

    configHelper.addClassReplacement(CACHE_CLASS_NAME_DOTS, CACHETC_CLASS_NAME_DOTS);
    configHelper.addClassReplacement(MEMORYSTOREEVICTIONPOLICY_CLASS_NAME_DOTS,
                                     MEMORYSTOREEVICTIONPOLICYTC_CLASS_NAME_DOTS);

  }
}