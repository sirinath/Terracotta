/**
 * 
 */
package org.terracotta.maven.plugins.tc;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;

/**
 * Generate Manifest for Terracotta Integration Module (TIM)
 * 
 * @goal manifest
 * @author Eugene Kuleshov
 * @execute phase="generate-resources"
 * @phase generate-resources
 * @requiresDependencyResolution compile
 */
public class ManifestMojo extends AbstractMojo {

  /**
   * @parameter expression="${manifest.file}" default-value="${project.build.directory}/MANIFEST.MF"
   */
  private File manifestFile;

  /**
   * @parameter expression="${bundleCategory}"
   */
  private String bundleCategory;

  /**
   * @parameter expression="${bundleCopyright}"
   */
  private String bundleCopyright;

  /**
   * @parameter expression="${bundleDescription}" default-value="${project.description}"
   */
  private String bundleDescription;

  /**
   * @parameter expression="${bundleName}" default-value="${project.name}"
   */
  private String bundleName;

  /**
   * @parameter expression="${bundleVendor}" default-value="${project.organization.name}"
   */
  private String bundleVendor;

  /**
   * @parameter expression="${bundleActivator}"
   */
  private String bundleActivator;

  /**
   * Specifications for the required bundles. For example,
   * org.terracotta.modules.modules_common;bundle-version:="1.0.0.SNAPSHOT",
   * org.terracotta.modules.clustered_cglib_2.1.3;bundle-version:="1.0.0.SNAPSHOT"
   * 
   * @parameter expression="${requireBundle}"
   */
  private String requireBundle;

  /**
   * @parameter expression="${importPackage}"
   */
  private String importPackage;

  /**
   * @parameter expression="${bundleSymbolicName}"
   */
  private String bundleSymbolicName;

  /**
   * @parameter expression="${bundleVersion}"
   */
  private String bundleVersion;

  // Bundle-ManifestVersion: 2
  // Bundle-RequiredExecutionEnvironment: J2SE-1.4

  /**
   * @parameter expression="${project}"
   * @required
   * @readonly
   */
  protected MavenProject project;

  public void execute() throws MojoExecutionException, MojoFailureException {
    Manifest manifest = new Manifest();
    
    Attributes attributes = manifest.getMainAttributes();

    attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
    
    attributes.putValue("Bundle-ManifestVersion", "2");
    if (bundleCategory != null) {
      attributes.putValue("Bundle-Category", bundleCategory);
    }
    if (bundleDescription != null) {
      attributes.putValue("Bundle-Description", bundleDescription);
    }
    if (bundleCopyright != null) {
      attributes.putValue("Bundle-Copyright", bundleCopyright);
    }
    if (bundleVendor != null) {
      attributes.putValue("Bundle-Vendor", bundleVendor);
    }
    if (bundleName != null) {
      attributes.putValue("Bundle-Name", bundleName);
    }

    if (requireBundle != null) {
      attributes.putValue("Require-Bundle", requireBundle);
    } else {
      // TODO generate Require-Bundle
    }
    
    if (importPackage != null) {
      attributes.putValue("Import-Package", importPackage);
    }

    if(bundleActivator!=null) {
      attributes.putValue("Bundle-Activator", bundleActivator);
    }
    
    if (bundleSymbolicName != null) {
      attributes.putValue("Bundle-SymbolicName", bundleSymbolicName);
    } else {
      String groupId = project.getGroupId();
      String artifactId = project.getArtifactId();
      String symbolicName = groupId + "." + artifactId;
      symbolicName = symbolicName.replace('-', '_');
      attributes.putValue("Bundle-SymbolicName", symbolicName);
    }

    if (bundleVersion != null) {
      attributes.putValue("Bundle-Version", bundleVersion);
    } else {
      DefaultArtifactVersion v = new DefaultArtifactVersion(project.getVersion());
      String version = v.getMajorVersion() + "." + v.getMinorVersion() + //
          "." + v.getIncrementalVersion();
      if(v.getQualifier()!=null) {
        version += "." + v.getQualifier();
      }
      attributes.putValue("Bundle-Version", version);
    }

    for (Iterator it = attributes.keySet().iterator(); it.hasNext();) {
      Attributes.Name key = (Attributes.Name) it.next();
      getLog().info(key + ": " + attributes.getValue(key));
    }
    
    FileOutputStream fos = null;
    try {
      manifestFile.getAbsoluteFile().getParentFile().mkdirs();
      
      fos = new FileOutputStream(manifestFile);
      manifest.write(fos);
      fos.flush();
    } catch (IOException ex) {
      getLog().error("Unable to write manifest file " + manifestFile.getAbsolutePath(), ex);
      throw new MojoFailureException(ex.getMessage());
    } finally {
      if (fos != null) {
        try {
          fos.close();
        } catch (IOException ex) {
          getLog().error("Unable to close stream", ex);
        }
      }
    }
  }

}
