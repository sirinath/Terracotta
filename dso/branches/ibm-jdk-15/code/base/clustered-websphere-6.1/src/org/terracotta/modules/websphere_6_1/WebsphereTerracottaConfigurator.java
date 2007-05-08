package org.terracotta.modules.websphere_6_1;

import org.osgi.framework.BundleContext;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.config.StandardDSOClientConfigHelper;

public final class WebsphereTerracottaConfigurator extends TerracottaConfiguratorModule {
	protected final void addInstrumentation(final BundleContext context, final StandardDSOClientConfigHelper configHelper) {
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.WebAppFilterManager", new WebAppFilterManagerClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.webapp.WebApp", new WebAppClassAdapter());
    configHelper.addCustomAdapter("com.ibm.ws.webcontainer.filter.FilterInstanceWrapper", new FilterInstanceWrapperClassAdapter());
	}

	protected final void registerModuleSpec(final BundleContext context) {
	}
}
