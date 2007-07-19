package org.terracotta.modules.ehcache_1_3;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.terracotta.modules.configuration.TerracottaConfiguratorModule;

import com.tc.object.bytecode.ClassAdapterFactory;
import com.tc.object.config.IStandardDSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public class EhcacheTerracottaConfigurator extends TerracottaConfiguratorModule
		implements IConstants {

	protected void addInstrumentation(final BundleContext context,
			final IStandardDSOClientConfigHelper configHelper) {
		ClassAdapterFactory factory = new EhcacheLruMemoryStoreAdapter();
		TransparencyClassSpec spec = ((StandardDSOClientConfigHelper)configHelper)
				.getOrCreateSpec(LRUMEMORYSTORE_CLASS_NAME_DOTS);
		spec.setCallConstructorOnLoad(true);
		spec.setCustomClassAdapter(factory);
	}
	
}
