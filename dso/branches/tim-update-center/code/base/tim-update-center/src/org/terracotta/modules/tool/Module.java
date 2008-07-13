/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.jdom.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A single Terracotta Integration Module (TIM) artifact. A TIM has a composite unique identifier consisting of groupId,
 * artifactId, and version, which is represented by the {@link ModuleId} class. Note that TIMs that are packaged
 * together into an archive are still represented as separate Tim objects.
 */

public class Module implements Comparable {
  private final ModuleId         id;
  private final String           repoUrl;
  private final String           installPath;
  private final String           filename;

  private final String           tcVersion;
  private final String           website;
  private final String           vendor;
  private final String           copyright;
  private final String           category;
  private final String           description;
  private final List<Dependency> dependencies;

  private final Modules          modules;

  public ModuleId getId() {
    return id;
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

  public List<Dependency> dependencies() {
    return Collections.unmodifiableList(dependencies);
  }

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

  public String getDescription() {
    return description;
  }

  /**
   * Return the list of direct dependencies of this module.
   */
  public List<Dependency> getDependencies() {
    return dependencies;
  }

  public static Module create(Modules modules, Element module) {
    return new Module(modules, module);
  }

  Module(Modules modules, Element root) {
    this.modules = modules;
    this.id = ModuleId.create(root);
    this.tcVersion = getChildText(root, "tc-version");
    this.website = getChildText(root, "website");
    this.vendor = getChildText(root, "vendor");
    this.copyright = getChildText(root, "copyright");
    this.category = getChildText(root, "category");
    this.description = getChildText(root, "description");
    this.repoUrl = getChildText(root, "repoUrl");
    this.installPath = getChildText(root, "installPath");
    this.filename = getChildText(root, "filename");
    this.dependencies = new ArrayList<Dependency>();
    if (root.getChild("dependencies") != null) {
      List<Element> children = root.getChild("dependencies").getChildren();
      for (Element child : children) {
        this.dependencies.add(new Dependency(child));
      }
    }
  }

  private String getChildText(Element element, String name) {
    return getChildText(element, name, "");
  }

  private String getChildText(Element element, String name, String defaultValue) {
    return (element.getChildText(name) == null ? defaultValue : element.getChildText(name)).trim();
  }

  public int compareTo(Object obj) {
    assert obj instanceof Module;
    Module other = (Module) obj;
    return id.compareTo(other.getId());
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) return false;
    if (obj == this) return true;
    if (getClass() != obj.getClass()) return false;
    Module other = (Module) obj;
    return toString().equals(other.toString());
  }

  public String getSymbolicName() {
    return ModuleId.computeSymbolicName(id.getGroupId(), id.getArtifactId());
  }

  public boolean isOlder(Module o) {
    assert getSymbolicName().equals(o.getSymbolicName());
    return id.sortableVersion().compareTo(o.getId().sortableVersion()) < 0;
  }

  public String toString() {
    return getSymbolicName() + " " + id.getVersion();
  }

  /**
   * Returns a list of all available version for this module. The list returned does not include this module's version.
   */
  public List<String> getVersions() {
    return getVersions(false);
  }

  /**
   * Returns a list of all available version for this module.
   * 
   * @param inclusive If true the list will include this module's version.
   */
  private List<String> getVersions(boolean inclusive) {
    List<ModuleId> idlist = new ArrayList<ModuleId>();
    for (Module module : this.modules.list()) {
      if (!module.isSibling(this)) continue;
      if (!inclusive && (module == this)) continue;
      idlist.add(module.getId());
    }
    Collections.sort(idlist);
    List<String> versions = new ArrayList<String>();
    for (ModuleId mid : idlist) {
      versions.add(mid.getVersion());
    }
    return versions;
  }

  public boolean isSibling(Module other) {
    return this.getSymbolicName().equals(other.getSymbolicName());
  }

  /**
   * Returns the siblings of this module. A sibling is another module with the matching symbolicName but a different
   * version. The list returned will not include this module.
   */
  public List<Module> getSiblings() {
    List<Module> siblings = new ArrayList<Module>();
    List<String> versions = getVersions();
    for (String version : versions) {
      Module sibling = this.modules.get(ModuleId.create(id.getGroupId(), id.getArtifactId(), version));
      siblings.add(sibling);
    }
    return siblings;
  }

  /**
   * Indicates if this module is the latest version among its siblings.
   */
  public boolean isLatest() {
    List<String> versions = getVersions(true);
    return versions.indexOf(this.getId().getVersion()) == (versions.size() - 1);
  }

  /**
   * Install this module.
   */
  public void install() {
    List<Dependency> manifest = computeManifest();
    for (Dependency entry : manifest) {
      System.out.println(" - " + entry.getFilename());
      System.out.println("   " + entry.getRepoUrl());
    }
  }

  private List<Dependency> computeManifest() {
    List<Dependency> manifest = new ArrayList<Dependency>();
    manifest.add(new Dependency(this));
    for (Dependency dependency : this.dependencies) {
      if (dependency.isReference()) {
        Module module = this.modules.get(dependency.getId());
        assert module != null;
        manifest.addAll(module.computeManifest());
        continue;
      }
      manifest.add(dependency);
    }
    return manifest;
  }

}
