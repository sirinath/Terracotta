/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single Terracotta Integration Module (TIM) artifact.
 *
 * A TIM has a composite unique identifier consisting of groupId, artifactId,
 * and version, which is represented by the {@link ModuleId} class.
 *
 * Note that TIMs that are packaged together into an archive are still
 * represented as separate Tim objects.
 */

public class Module {
  private final ModuleId id;
  private final String tcVersion;
  private final String website;
  private final String vendor;
  private final String copyright;
  private final String category;
  private final String description;
  private final String repoUrl;
  private final String installPath;
  private final String filename;
  
  public String getTcVersion() {
    return tcVersion;
  }

  public String getWebsite() {
    return website;
  }

  public String getVendor() {
    return vendor;
  }

  public String getCopyright() {
    return copyright;
  }

  public String getCategory() {
    return category;
  }

  public String getRepoUrl() {
    return repoUrl;
  }

  public String getInstallPath() {
    return installPath;
  }

  public String getFilename() {
    return filename;
  }

  public List<ModuleId> getDependencies() {
    return dependencies;
  }

  private final List<ModuleId> dependencies;

  public static Module create(Element module) {
    return new Module(module);
  }
  
  public boolean isCompatible(String with) {
    return this.tcVersion.equals("*") || this.tcVersion.equals(with);
  }
  
  Module(Element root) {
    this.id = createId(root);
    this.tcVersion = getChildText(root, "tc-version");
    this.website = getChildText(root, "website");
    this.vendor = getChildText(root, "vendor");
    this.copyright = getChildText(root, "copyright");
    this.category = getChildText(root, "category");
    this.description = getChildText(root, "description");
    this.repoUrl = getChildText(root, "repoUrl");
    this.installPath = getChildText(root, "installPath");
    this.filename = getChildText(root, "filename");
    this.dependencies = new ArrayList<ModuleId>();
    if (root.getChild("dependencies") == null) return;
    List<Element> moduleRefs = root.getChild("dependencies").getChildren();
    for (Element ref : moduleRefs) {
      this.dependencies.add(createId(ref));
    }
  }

  private ModuleId createId(Element element) {
    String artifactId = element.getAttributeValue("artifactId");
    String groupId = element.getAttributeValue("groupId");
    String version = element.getAttributeValue("version");
    return new ModuleId(groupId, artifactId, version);
  }
  
  private String getChildText(Element element, String name) {
    return getChildText(element, name, "");
  }
  
  private String getChildText(Element element, String name, String defaultValue) {
    return element.getChildText(name) == null ? defaultValue : element.getChildText(name);
  }
  
  /** Returns the composite unique identifier for this TIM. */
  public ModuleId getId() {
    return id;
  }

  /** Returns the description of this TIM. */
  public String getDescription() {
    return description;
  }

  /**
   * A list of this TIM's direct dependencies.
   */
  public List<ModuleId> dependencies() {
    return Collections.unmodifiableList(dependencies);
  }

  /**
   * A flattened list of transitive dependencies for this TIM.
   */
  //public List<Tim> transitiveDependencies();
}
