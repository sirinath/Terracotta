package org.terracotta.plugins.instrumentation.struts;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.bytecode.struts.IncludeTagAdapter;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public final class StrutsTerracottaInstrumentation implements BundleActivator {

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
    if (configHelperRef != null) {
      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
          .getService(configHelperRef);
      addStrutsInstrumentation(configHelper);
      context.ungetService(configHelperRef);
    } else {
      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
          + " service to be registered, was unable to find it");
    }
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  private void addStrutsInstrumentation(final StandardDSOClientConfigHelper configHelper) {
    // Hack for honoring transient in Struts action classes
    TransparencyClassSpec spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionForm");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionMappings");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.ActionServletWrapper");
    spec.setHonorTransient(true);
    spec = configHelper.getOrCreateSpec("org.apache.struts.action.DynaActionFormClass");
    spec.setHonorTransient(true);

    // Hack for Struts <bean:include> tag
    configHelper.addCustomAdapter("org.apache.struts.taglib.bean.IncludeTag", IncludeTagAdapter.class);
  }

}
