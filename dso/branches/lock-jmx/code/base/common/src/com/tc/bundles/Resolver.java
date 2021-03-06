/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.apache.commons.io.FileUtils;
import org.osgi.framework.BundleException;

import com.tc.bundles.exception.InvalidBundleManifestException;
import com.tc.bundles.exception.MissingBundleException;
import com.tc.bundles.Version;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.terracottatech.config.Module;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Resolver {

  private static final String   BUNDLE_VERSION        = "Bundle-Version";
  private static final String   BUNDLE_SYMBOLICNAME   = "Bundle-SymbolicName";

  private static final String   TC_PROPERTIES_SECTION = "l1.modules";

  private static final String[] JAR_EXTENSIONS        = new String[] { "jar" };

  private static final TCLogger logger                = TCLogging.getLogger(Resolver.class);

  private URL[]                 repositories;
  private List                  registry              = new ArrayList();

  public Resolver(final URL[] repositories) throws BundleException {
    final List repoLocations = new ArrayList();

    try {
      injectDefaultRepositories(repoLocations);
    } catch (MalformedURLException mue) {
      throw new BundleException("Failed to inject default repositories", mue);
    }

    for (int i = 0; i < repositories.length; i++) {
      if (!repositories[i].getProtocol().equalsIgnoreCase("file")) {
        throw new BundleException(formatMessage(Message.WARN_REPOSITORY_PROTOCOL_UNSUPPORTED, new Object[] { repositories[i] }));
      } else {
        repoLocations.add(repositories[i]);
      }
    }

    if (repoLocations.isEmpty()) { throw new RuntimeException(
                                                              "No module repositories have been specified via the com.tc.l1.modules.repositories system property"); }

    this.repositories = (URL[]) repoLocations.toArray(new URL[0]);
  }

  private static final void injectDefaultRepositories(final List repoLocations) throws MalformedURLException {
    final String installRoot = System.getProperty("tc.install-root");
    if (installRoot != null) {
      final URL defaultRepository = new File(installRoot, "modules").toURL();
      consoleLogger.debug("Appending default bundle repository: '" + defaultRepository.toString() + "'");
      repoLocations.add(defaultRepository);
    }

    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String reposProp = props != null ? props.getProperty("repositories", true) : null;
    if (reposProp != null) {
      final String[] entries = reposProp.split(",");
      if (entries != null) {
        for (int i = 0; i < entries.length; i++) {
          String entry = entries[i].trim();
          if (entry != null && entry.length() > 0) {
            final URL defaultRepository = new URL(entries[i]);
            consoleLogger.debug("Prepending default bundle repository: '" + defaultRepository.toString() + "'");
            repoLocations.add(defaultRepository);
          }
        }
      }
    }
  }

  public final URL resolve(Module module) throws BundleException {
    final String name = module.getName();
    final String version = module.getVersion();
    final String groupId = module.getGroupId();
    final URL location = resolveLocation(name, version, groupId);

    if (location == null) {
      final String msg = fatal(Message.ERROR_BUNDLE_UNRESOLVED, new Object[] { module.getName(), module.getVersion(),
          module.getGroupId(), repositoriesToString() });
      throw new MissingBundleException(msg);
    }

    logger.info("Resolved module " + groupId + ":" + name + ":" + version + " from " + location);

    resolveDependencies(location);
    return location;
  }

  public final URL[] resolve(Module[] modules) throws BundleException {
    resolveDefaultModules();
    resolveAdditionalModules();

    if (modules != null) {
      for (int i = 0; i < modules.length; i++) {
        resolve(modules[i]);
      }
    }

    return getResolvedUrls();
  }

  public final URL[] getResolvedUrls() {
    int j = 0;
    final URL[] urls = new URL[registry.size()];
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      urls[j++] = entry.getLocation();
    }
    return urls;
  }

  private Collection findJars(File rootLocation, String groupId, String name, String version) {
    File groupLocation = new File(rootLocation, groupId.replace('.', File.separatorChar));
    File nameLocation = new File(groupLocation, name);
    File versionLocation = new File(nameLocation, version);
    final Collection jars = new ArrayList();

    File exactLocation = new File(versionLocation, name + "-" + version + ".jar");
    if (exactLocation.exists() && exactLocation.isFile()) {
      jars.add(exactLocation);
    }

    if (jars.isEmpty()) {
      if (versionLocation.isDirectory()) {
        jars.addAll(FileUtils.listFiles(versionLocation, JAR_EXTENSIONS, false));
      }
    } else {
      return jars;
    }

    if (jars.isEmpty()) {
      if (nameLocation.isDirectory()) {
        jars.addAll(FileUtils.listFiles(nameLocation, JAR_EXTENSIONS, !versionLocation.isDirectory()));
      }
    } else {
      return jars;
    }

    if (jars.isEmpty()) {
      if (groupLocation.isDirectory()) {
        jars.addAll(FileUtils.listFiles(groupLocation, JAR_EXTENSIONS, !nameLocation.isDirectory()));
      }
    } else {
      return jars;
    }

    if (jars.isEmpty() && rootLocation.isDirectory()) {
      jars.addAll(FileUtils.listFiles(rootLocation, JAR_EXTENSIONS, !groupLocation.isDirectory()));
    }

    return jars;
  }

  protected URL resolveBundle(BundleSpec spec) {
    for (int i = repositories.length - 1; i >= 0; i--) {
      final URL location = repositories[i];
      // TODO: support other protocol besides file:// - for now this is being checked in the constructor
      Assert.assertTrue(location.getProtocol().equalsIgnoreCase("file"));

      final File root = FileUtils.toFile(location);
      final File repository = new File(root, spec.getGroupId().replace('.', File.separatorChar));
      if (!repository.exists() || !repository.isDirectory()) {
        warn(Message.WARN_REPOSITORY_UNRESOLVED, new Object[] { repository.getAbsolutePath() });
        continue;
      }

      final Collection jars = findJars(root, spec.getGroupId(), spec.getName(), spec.getVersion());
      for (final Iterator j = jars.iterator(); j.hasNext();) {
        final File bundleFile = (File) j.next();
        if (!bundleFile.isFile()) {
          warn(Message.WARN_FILE_IGNORED_INVALID_NAME, new Object[] { bundleFile.getName() });
          continue;
        }
        final Manifest manifest = getManifest(bundleFile);
        if (manifest == null) {
          warn(Message.WARN_FILE_IGNORED_MISSING_MANIFEST, new Object[] { bundleFile.getName() });
          continue;
        }
        final String symname = manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
        final String version = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
        if (spec.isCompatible(symname, version)) {
          try {
            return bundleFile.toURL();
          } catch (MalformedURLException e) {
            fatal(Message.ERROR_BUNDLE_MALFORMED_URL, new Object[] { bundleFile.getName() }); // should be fatal???
            return null;
          }
        }
      }
    }
    return null;
  }

  protected URL resolveLocation(final String name, final String version, final String groupId) {
    final String symname = MavenToOSGi.artifactIdToSymbolicName(groupId, name);
    final String osgiVersionStr = MavenToOSGi.projectVersionToBundleVersion(version);
    Version osgiVersion = Version.parse(osgiVersionStr);

    if (logger.isDebugEnabled()) {
      logger.debug("Resolving location of " + groupId + ":" + name + ":" + version);
    }

    for (int i = repositories.length - 1; i >= 0; i--) {
      final String repositoryURL = repositories[i].toString() + (repositories[i].toString().endsWith("/") ? "" : "/");
      URL url = null;
      try {
        url = new URL(repositoryURL);
      } catch (MalformedURLException e) {
        // ignore bad URLs
        logger.warn("Ignoring bad repository URL during resolution: " + repositoryURL, e);
        continue;
      }

      final Collection jars = findJars(FileUtils.toFile(url), groupId, name, version);
      for (final Iterator j = jars.iterator(); j.hasNext();) {
        final File jar = (File) j.next();
        final Manifest manifest = getManifest(jar);

        if (isBundleMatch(jar, manifest, symname, osgiVersion)) {
          try {
            return addToRegistry(jar.toURL(), manifest);
          } catch (MalformedURLException e) {
            logger.error(e.getMessage(), e);
          }
        }
      }

    }
    return null;
  }

  private boolean isBundleMatch(File jarFile, Manifest manifest, String bundleName, Version bundleVersion) {
    if (logger.isDebugEnabled()) logger.debug("Checking " + jarFile + " for " + bundleName + ":" + bundleVersion);

    // ignore bad JAR files
    if (manifest == null) return false;

    // found a match!
    if (BundleSpec.isMatchingSymbolicName(bundleName, manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME))) {
      final String manifestVersion = manifest.getMainAttributes().getValue(BUNDLE_VERSION);
      try {
        if (bundleVersion.equals(Version.parse(manifestVersion))) { return true; }
      } catch (NumberFormatException e) { // thrown by parseVersion()
        consoleLogger.warn("Bad manifest bundle version in jar='" + jarFile.getAbsolutePath() + "', version='"
                           + manifestVersion + "'.  Skipping...", e);
      }
    }

    return false;
  }

  private void resolveDefaultModules() throws BundleException {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String defaultModulesProp = props != null ? props.getProperty("default") : null;

    if (defaultModulesProp == null) {
      consoleLogger.debug("No implicit modules were loaded because the l1.modules.default property "
                          + "in tc.properties file was not set.");
      return;
    }

    final String[] defaultModulesSpec = BundleSpec.getRequirements(defaultModulesProp);
    if (defaultModulesSpec.length > 0) {
      for (int i = 0; i < defaultModulesSpec.length; i++) {
        BundleSpec spec = BundleSpec.newInstance(defaultModulesSpec[i]);
        ensureBundle(spec);
      }
    } else {
      consoleLogger.debug("No implicit modules were loaded because the l1.modules.default property "
                          + "in tc.properties file was empty.");
    }
  }

  private String repositoriesToString() {
    final StringBuffer repos = new StringBuffer();
    for (int j = 0; j < repositories.length; j++) {
      if (j > 0) repos.append(";");
      repos.append(repositories[j]);
    }

    return repos.toString();
  }

  private void resolveAdditionalModules() throws BundleException {
    final TCProperties props = TCPropertiesImpl.getProperties().getPropertiesFor(TC_PROPERTIES_SECTION);
    final String additionalModulesProp = props != null ? props.getProperty("additional") : null;
    if (additionalModulesProp != null) {
      String[] additionalModulesSpec = BundleSpec.getRequirements(additionalModulesProp);
      if (additionalModulesSpec.length > 0) {
        for (int i = 0; i < additionalModulesSpec.length; i++) {
          BundleSpec spec = BundleSpec.newInstance(additionalModulesSpec[i]);
          ensureBundle(spec);
        }
      }
    }
  }

  private BundleSpec[] getRequirements(Manifest manifest) {
    List requirementList = new ArrayList();
    String[] manifestRequirements = BundleSpec.getRequirements(manifest);
    if (manifestRequirements.length > 0) {
      for (int i = 0; i < manifestRequirements.length; i++) {
        requirementList.add(BundleSpec.newInstance(manifestRequirements[i]));
      }
    }
    return (BundleSpec[]) requirementList.toArray(new BundleSpec[0]);
  }

  private void resolveDependencies(final URL location) throws BundleException {
    final Manifest manifest = getManifest(location);
    if (manifest == null) {
      final String msg = fatal(Message.ERROR_BUNDLE_UNREADABLE, new Object[] { FileUtils.toFile(location).getName(),
          FileUtils.toFile(location).getParent() });
      throw new InvalidBundleManifestException(msg);
    }

    final BundleSpec[] requirements = getRequirements(manifest);
    for (int i = 0; i < requirements.length; i++) {
      final BundleSpec spec = requirements[i];
      ensureBundle(spec);
    }

    addToRegistry(location, manifest);
  }

  private void ensureBundle(final BundleSpec spec) throws BundleException {
    URL required = findInRegistry(spec);
    if (required == null) {
      required = resolveBundle(spec);
      if (required == null) {
        final String msg = fatal(Message.ERROR_BUNDLE_DEPENDENCY_UNRESOLVED, new Object[] { spec.getName(),
            spec.getVersion(), spec.getGroupId(), repositoriesToString() });
        throw new MissingBundleException(msg);
      }
      addToRegistry(required, getManifest(required));
      resolveDependencies(required);
    }
  }

  private URL addToRegistry(final URL location, final Manifest manifest) {
    final Entry entry = new Entry(location, manifest);
    if (!registry.contains(entry)) registry.add(entry);
    return entry.getLocation();
  }

  private URL findInRegistry(BundleSpec spec) {
    URL location = null;
    for (Iterator i = registry.iterator(); i.hasNext();) {
      final Entry entry = (Entry) i.next();
      if (spec.isCompatible(entry.getSymbolicName(), entry.getVersion())) {
        location = entry.getLocation();
        break;
      }
    }
    return location;
  }

  /**
   * Read the manifest from a JAR file.
   * 
   * @param file The JAR file
   * @return An instance of a Manifest; null if file is null or if it was unable to read the file for some reason.
   */
  private Manifest getManifest(final File file) {
    if (file == null) return null;
    try {
      final JarFile bundle = new JarFile(file);
      return bundle.getManifest();
    } catch (IOException e) {
      return null;
    }
  }

  /**
   * Read the manifest from a JAR file referred to by an URL
   * 
   * @param location The URL of the JAR file
   * @return An instance of a Manifest; null if unable to resolve the URL to a File.
   */
  private Manifest getManifest(final URL location) {
    return getManifest(FileUtils.toFile(location));
  }

  private String warn(final Message message, final Object[] arguments) {
    final String msg = formatMessage(message, arguments);
    consoleLogger.warn(msg);
    return msg;
  }

  private String fatal(final Message message, final Object[] arguments) {
    final String msg = formatMessage(message, arguments);
    consoleLogger.fatal(msg);
    return msg;
  }

  private static String formatMessage(final Message message, final Object[] arguments) {
    return MessageFormat.format(resourceBundle.getString(message.key()), arguments);
  }

  // XXX it is a very bad idea to use URL to calculate hashcode
  private final class Entry {
    private URL      location;
    private Manifest manifest;

    public Entry(final URL location, final Manifest manifest) {
      this.location = location;
      this.manifest = manifest;
    }

    public String getVersion() {
      return manifest.getMainAttributes().getValue(BUNDLE_VERSION);
    }

    public String getSymbolicName() {
      return manifest.getMainAttributes().getValue(BUNDLE_SYMBOLICNAME);
    }

    public URL getLocation() {
      return location;
    }

    public boolean equals(Object object) {
      if (this == object) return true;
      if (!(object instanceof Entry)) return false;
      final Entry entry = (Entry) object;
      return location.equals(entry.getLocation()) && getVersion().equals(entry.getVersion())
             && getSymbolicName().equals(entry.getSymbolicName());
    }

    private static final int SEED1 = 18181;
    private static final int SEED2 = 181081;

    public int hashCode() {
      int result = SEED1;
      result = hash(result, this.location);
      result = hash(result, this.manifest);
      return result;
    }

    private int hash(int seed, int value) {
      return SEED2 * seed + value;
    }

    private int hash(int seed, Object object) {
      int result = seed;
      if (object == null) {
        result = hash(result, 0);
      } else if (!object.getClass().isArray()) {
        result = hash(result, object);
      } else {
        int len = Array.getLength(object);
        for (int i = 0; i < len; i++) {
          Object o = Array.get(object, i);
          result = hash(result, o);
        }
      }
      return result;
    }
  }

  private static final class Message {

    static final Message WARN_BUNDLE_UNRESOLVED               = new Message("warn.bundle.unresolved");
    static final Message WARN_REPOSITORY_UNRESOLVED           = new Message("warn.repository.unresolved");
    static final Message WARN_FILE_IGNORED_INVALID_NAME       = new Message("warn.file.ignored.invalid-name");
    static final Message WARN_FILE_IGNORED_MISSING_MANIFEST   = new Message("warn.file.ignored.missing-manifest");
    static final Message WARN_REPOSITORY_PROTOCOL_UNSUPPORTED = new Message("warn.repository.protocol.unsupported");
    static final Message WARN_EXCEPTION_OCCURED               = new Message("warn.exception.occured");
    static final Message ERROR_BUNDLE_UNREADABLE              = new Message("error.bundle.unreadable");
    static final Message ERROR_BUNDLE_UNRESOLVED              = new Message("error.bundle.unresolved");
    static final Message ERROR_BUNDLE_DEPENDENCY_UNRESOLVED   = new Message("error.bundle-dependency.unresolved");
    static final Message ERROR_BUNDLE_MALFORMED_URL           = new Message("error.bundle.malformed-url");

    private final String resourceBundleKey;

    private Message(final String resourceBundleKey) {
      this.resourceBundleKey = resourceBundleKey;
    }

    String key() {
      return resourceBundleKey;
    }
  }

  private static final TCLogger       consoleLogger = CustomerLogging.getConsoleLogger();
  private static final ResourceBundle resourceBundle;

  static {
    try {
      resourceBundle = ResourceBundle.getBundle(Resolver.class.getName(), Locale.getDefault(), Resolver.class
          .getClassLoader());
    } catch (MissingResourceException mre) {
      throw new RuntimeException("No resource bundle exists for " + Resolver.class.getName());
    } catch (Throwable t) {
      throw new RuntimeException("Unexpected error loading resource bundle for " + Resolver.class.getName(), t);
    }
  }
}
