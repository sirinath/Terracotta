package org.terracotta.modules.ehcache_1_3;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.ehcache.commons_1_0.EhcacheTerracottaCommonsConfigurator;

import com.tc.object.config.StandardDSOClientConfigHelper;

public class EhcacheTerracottaConfigurator extends EhcacheTerracottaCommonsConfigurator {
	protected void addInstrumentation(BundleContext context, StandardDSOClientConfigHelper configHelper) {
	    super.addInstrumentation(context, configHelper);
	  }
}
