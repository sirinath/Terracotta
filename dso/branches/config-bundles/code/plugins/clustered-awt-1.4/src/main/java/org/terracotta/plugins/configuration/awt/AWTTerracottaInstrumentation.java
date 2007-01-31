package org.terracotta.plugins.configuration.awt;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;

public final class AWTTerracottaInstrumentation implements BundleActivator {

  public void start(final BundleContext context) throws Exception {
    final ServiceReference configHelperRef = context.getServiceReference(StandardDSOClientConfigHelper.class.getName());
    if (configHelperRef != null) {
      final StandardDSOClientConfigHelper configHelper = (StandardDSOClientConfigHelper) context
          .getService(configHelperRef);
      addAWTInstrumentation(configHelper);
      context.ungetService(configHelperRef);
    } else {
      throw new BundleException("Expected the " + StandardDSOClientConfigHelper.class.getName()
          + " service to be registered, was unable to find it");
    }
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  private void addAWTInstrumentation(final StandardDSOClientConfigHelper configHelper) {
    configHelper.addIncludePattern("java.awt.Color", true);

    TransparencyClassSpec spec = configHelper.getOrCreateSpec("java.awt.AWTException");
    spec = configHelper.getOrCreateSpec("java.awt.Color");
    spec.addTransient("cs");

    spec = configHelper.getOrCreateSpec("java.awt.event.MouseMotionAdapter");
    spec = configHelper.getOrCreateSpec("java.awt.event.MouseAdapter");

    // java.awt.point
    spec = configHelper.getOrCreateSpec("java.awt.Point");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Point2D");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Point2D$Double");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Point2D$Float");
    // end java.awt.Point

    // java.awt.geom.Line
    spec = configHelper.getOrCreateSpec("java.awt.geom.Line2D");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Line2D$Double");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Line2D$Float");
    // end java.awt.geom.Line

    // java.awt.Rectangle
    spec = configHelper.getOrCreateSpec("java.awt.Rectangle");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Rectangle2D");
    spec = configHelper.getOrCreateSpec("java.awt.geom.RectangularShape");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Rectangle2D$Double");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Rectangle2D$Float");
    spec = configHelper.getOrCreateSpec("java.awt.geom.RoundRectangle2D");
    spec = configHelper.getOrCreateSpec("java.awt.geom.RoundRectangle2D$Double");
    spec = configHelper.getOrCreateSpec("java.awt.geom.RoundRectangle2D$Float");
    // end java.awt.Rectangle

    // java.awt.geom.Ellipse2D
    spec = configHelper.getOrCreateSpec("java.awt.geom.Ellipse2D");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Ellipse2D$Double");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Ellipse2D$Float");
    // end java.awt.geom.Ellipse2D

    // java.awt.geom.GeneralPath
    spec = configHelper.getOrCreateSpec("java.awt.geom.GeneralPath");
    // end java.awt.geom.GeneralPath

    // java.awt.BasicStroke
    spec = configHelper.getOrCreateSpec("java.awt.BasicStroke");
    // end java.awt.BasicStroke

    // java.awt.Dimension
    spec = configHelper.getOrCreateSpec("java.awt.Dimension");
    spec = configHelper.getOrCreateSpec("java.awt.geom.Dimension2D");
    // end java.awt.Dimension
  }

}
