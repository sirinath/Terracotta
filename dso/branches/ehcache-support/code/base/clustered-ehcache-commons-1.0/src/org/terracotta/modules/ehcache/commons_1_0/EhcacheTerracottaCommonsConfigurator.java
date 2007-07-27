package org.terracotta.modules.ehcache.commons_1_0;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public class EhcacheTerracottaCommonsConfigurator extends TerracottaConfiguratorModule {
	protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
		super.addInstrumentation(context, configHelper);

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
