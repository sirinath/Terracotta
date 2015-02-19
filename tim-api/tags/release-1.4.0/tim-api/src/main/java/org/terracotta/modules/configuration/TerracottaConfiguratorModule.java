/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.configuration;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;

import com.tc.bundles.BundleSpecUtil;
import com.tc.logging.TCLogger;
import com.tc.object.config.ClassReplacementTest;
import com.tc.object.config.LockDefinition;
import com.tc.object.config.StandardDSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.properties.TCProperties;

import java.net.MalformedURLException;
import java.net.URL;

public abstract class TerracottaConfiguratorModule implements BundleActivator {

  protected StandardDSOClientConfigHelper configHelper;
  private TCLogger                        logger;
  private Bundle                          thisBundle;
  private TCProperties                    tcProps;

  protected ServiceReference getConfigHelperReference(final BundleContext context) throws Exception {
    final String CONFIGHELPER_CLASS_NAME = StandardDSOClientConfigHelper.class.getName();
    final ServiceReference configHelperRef = context.getServiceReference(CONFIGHELPER_CLASS_NAME);
    if (configHelperRef == null) { throw new BundleException("Expected the " + CONFIGHELPER_CLASS_NAME
                                                             + " service to be registered, was unable to find it"); }
    return configHelperRef;
  }

  public final void start(final BundleContext context) throws Exception {
    thisBundle = context.getBundle();

    logger = (TCLogger) context.getService(context.getServiceReference(TCLogger.class.getName()));
    if (logger == null) { throw new BundleException("missing logger reference for " + thisBundle.getSymbolicName()); }

    tcProps = (TCProperties) context.getService(context.getServiceReference(TCProperties.class.getName()));
    if (tcProps == null) { throw new BundleException("missing tc-properties reference for "
                                                     + thisBundle.getSymbolicName()); }

    final ServiceReference configHelperRef = getConfigHelperReference(context);
    configHelper = (StandardDSOClientConfigHelper) context.getService(configHelperRef);
    if (configHelper == null) { throw new AssertionError("configHelper is null"); }
    addInstrumentation(context);
    context.ungetService(configHelperRef);
    registerModuleSpec(context);
    if (!Boolean.getBoolean("tc.bootjar.creation")) {
      registerMBeanSpec(context);
      registerSRASpec(context);
    }
  }

  protected Bundle getThisBundle() {
    return thisBundle;
  }

  protected TCProperties getTcProps() {
    return tcProps;
  }

  protected TCLogger getLogger() {
    return logger;
  }

  public void stop(final BundleContext context) throws Exception {
    // Ignore this, we don't need to stop anything
  }

  protected void addInstrumentation(final BundleContext context) {
    // default empty body
  }

  protected void registerModuleSpec(final BundleContext context) {
    // default empty body
  }

  protected void registerMBeanSpec(final BundleContext context) {
    // default empty body
  }

  protected void registerSRASpec(final BundleContext context) {
    // default empty body
  }

  protected final void addClassReplacement(final Bundle bundle, final String originalClassName,
                                           final String replacementClassName) {
    addClassReplacement(bundle, originalClassName, replacementClassName, null);
  }

  protected final void addClassReplacement(final Bundle bundle, final String originalClassName,
                                           final String replacementClassName, final ClassReplacementTest test) {
    URL resource = getBundleResourceURL(bundle, classNameToFileName(replacementClassName));
    configHelper.addClassReplacement(originalClassName, replacementClassName, resource, test);
  }

  private URL getBundleResourceURL(final Bundle bundle, final String resourceName) {
    URL bundleURL = configHelper.getBundleURL(bundle);
    if (bundleURL == null) { throw new RuntimeException(bundle.getLocation() + " was not loaded with this config"); }

    try {
      if (bundleURL.getProtocol().equals("file") && bundleURL.getPath().endsWith(".jar")) {
        // this is the normal case where TIMs are regular file based jar URLs
        return new URL("jar:" + bundleURL.toExternalForm() + "!/" + resourceName);
      }

      String base = bundleURL.toExternalForm();
      if (!base.endsWith("/")) {
        base = base + "/";
      }

      return new URL(bundleURL, base + resourceName);
    } catch (MalformedURLException e) {
      throw new RuntimeException("Cannot create URL for " + resourceName, e);
    }
  }

  /**
   * Export the given class that normally resides in a config bundle (aka. integration module) to all classloaders that
   * might try to load it. This is sort of like creating a jar containing the one given class and appending into the
   * lookup path of every classloader NOTE: The export will only work for class loads that pass through
   * java.lang.ClassLoader.loadClassInternal(). Specifically if the loadClass() method is directly being invoked from
   * code someplace, the class export will not function. Code that does a "new <exported class name>", or that uses
   * java.lang.Class.forName(..) will work though
   * 
   * @param classname the bundle class name to export
   * @param targetSystemLoaderOnly True if only the system classloader should have visibility to this exported class
   */
  protected final void addExportedBundleClass(final Bundle bundle, final String classname,
                                              final boolean targetSystemLoaderOnly) {
    URL url = getBundleResourceURL(bundle, classNameToFileName(classname));
    configHelper.addClassResource(classname, url, targetSystemLoaderOnly);
  }

  protected final void addExportedBundleClass(final Bundle bundle, final String classname) {
    addExportedBundleClass(bundle, classname, false);
  }

  /**
   * Export the given class that normally resides in tc.jar to all classloaders that might try to load it. This is sort
   * of like creating a jar containing the one given class and appending into the lookup path of every classloader NOTE:
   * The export will only work for class loads that pass through java.lang.ClassLoader.loadClassInternal(). Specifically
   * if the loadClass() method is directly being invoked from code someplace, the class export will not function. Code
   * that does a "new <exported class name>", or that uses java.lang.Class.forName(..) will work though
   * 
   * @param classname the tc.jar class name to export
   */
  protected final void addExportedTcJarClass(final String classname) {
    URL resource = TerracottaConfiguratorModule.class.getClassLoader().getResource(classNameToFileName(classname));

    if (resource == null) { throw new RuntimeException("Exported TC jar class " + classname + " does not exist."); }

    configHelper.addClassResource(classname, resource, false);
  }

  protected TransparencyClassSpec getOrCreateSpec(final String expr, final boolean markAsPreInstrumented) {
    final TransparencyClassSpec spec = configHelper.getOrCreateSpec(expr);
    if (markAsPreInstrumented) spec.markPreInstrumented();
    return spec;
  }

  protected TransparencyClassSpec getOrCreateSpec(final String expr) {
    return getOrCreateSpec(expr, true);
  }

  protected void addLock(final String expr, final LockDefinition ld) {
    configHelper.addLock(expr, ld);
  }

  protected Bundle getExportedBundle(final BundleContext context, final String targetBundleName) {
    // find the bundle that contains the replacement classes
    for (Bundle bundle : context.getBundles()) {
      if (BundleSpecUtil.isMatchingSymbolicName(targetBundleName, bundle.getSymbolicName())) {
        //
        return bundle;
      }
    }
    return null;
  }

  private static final String classNameToFileName(final String className) {
    return className.replace('.', '/') + ".class";
  }

}
