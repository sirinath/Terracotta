/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.modules.tool;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.jdom.Element;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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

  public List<ModuleId> dependencies() {
    List<ModuleId> list = new ArrayList<ModuleId>();
    list.addAll(computeManifest().keySet());
    list.remove(id);
    Collections.sort(list);
    return list;
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
    return Collections.unmodifiableList(dependencies);
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
   * Returns a list of all available version for this module. The list returned does not include the version of this
   * module.
   */
  public List<String> getVersions() {
    return getVersions(false);
  }

  public List<String> getAllVersions() {
    return getVersions(true);
  }

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
   * 
   * @throws IOException
   */
  public void install(boolean overwrite, boolean pretend, PrintWriter out) {
    Map<ModuleId, Dependency> manifest = computeManifest();
    List<ModuleId> list = new ArrayList<ModuleId>(manifest.keySet());
    for (ModuleId key : list) {
      Dependency dependency = manifest.get(key);

      File destdir = new File(installDirectory(), dependency.getInstallPath());
      File destfile = new File(destdir, dependency.getFilename());
      if (isInstalled(dependency) && !overwrite) {
        out.println("Skipped: " + destfile.getName());
        continue;
      }

      if (!pretend) {
        File srcfile = null;
        try {
          srcfile = File.createTempFile("tuc", null);
          download(dependency.getRepoUrl(), srcfile.getCanonicalPath());
        } catch (IOException e) {
          out.println("Unable to download: " + e.getMessage());
          continue;
        }

        try {
          FileUtils.forceMkdir(destdir);
          FileUtils.copyFile(srcfile, destfile);
        } catch (IOException e) {
          out.println("Unable to install: " + e.getMessage());
          continue;
        }
      }
      out.println("Installed: " + destfile);
    }
  }

  static void download(String address, String destpath) throws IOException {
    OutputStream out = null;
    URLConnection conn = null;
    InputStream in = null;
    try {
      URL url = new URL(address);
      out = new BufferedOutputStream(new FileOutputStream(destpath));
      conn = url.openConnection();
      in = conn.getInputStream();
      byte[] buffer = new byte[1024];
      int bytesread = 0;
      long byteswritten = 0;
      while ((bytesread = in.read(buffer)) != -1) {
        out.write(buffer, 0, bytesread);
        byteswritten += bytesread;
      }
    } finally {
      try {
        if (in != null) in.close();
        if (out != null) out.close();
      } catch (IOException ioe) {
        //
      }
    }
  }

  private File installPath(String path, String name) {
    return new File(new File(installDirectory(), path), name);
  }

  private File installPath() {
    return installPath(installPath, filename);
  }

  private File installDirectory() {
    String rootdir = System.getProperty("tc.install-root", System.getProperty("java.io.tmpdir"));
    return new File(rootdir, "modules");
  }

  public boolean isInstalled(Dependency dependency) {
    return installPath(dependency.getInstallPath(), dependency.getFilename()).exists();
  }

  public boolean isInstalled() {
    return installPath().exists();
  }

  private void printNotes(PrintWriter out, boolean printDigest) {
    if (printDigest) printLongDigest(out);
    out.println("   Compatible with " + (tcVersion.equals("*") ? "any Terracotta version." : "TC " + tcVersion));
    if (isInstalled()) out.println("   Installed at " + installDirectory());
  }

  public void printDetails(PrintWriter out) {
    printLongDigest(out);
    out.println();
    out.println("   groupId   : " + id.getGroupId());
    out.println("   artifactId: " + id.getArtifactId());
    out.println("   version   : " + id.getVersion());
    out.println();
    List<ModuleId> requires = dependencies();
    if (!requires.isEmpty()) {
      out.print("   Requires  : ");
      Collections.sort(requires);
      for (ModuleId m : requires) {
        out.print(m.getArtifactId());
        if (!m.isDefaultGroupId()) out.print(" [" + m.getGroupId() + "]");
        out.println(" (" + m.getVersion() + ")");
        out.print("               ");
      }
    }
    out.println();
    printSummary(out, false);
  }

  private void printSummary(PrintWriter out, boolean printDigest) {
    if (printDigest) printLongDigest(out);
    out.println("   Author    : " + vendor);
    out.println("   Copyright : " + copyright);
    out.println("   Homepage  : " + website);
    out.println("   Download  : " + repoUrl);
    out.println();
    out.println("   " + description.replaceAll("  ", " "));
    out.println();
    printNotes(out, false);
  }

  public void printSummary(PrintWriter out) {
    printSummary(out, true);
  }

  private void printLongDigest(PrintWriter out) {
    String groupId = id.isDefaultGroupId() ? "" : " [" + id.getGroupId() + "]";
    String versions = id.getVersion();
    List<String> allversions = getVersions();
    if (!allversions.isEmpty()) {
      Collections.reverse(allversions);
      versions = versions.concat("*, ").concat(StringUtils.join(allversions.iterator(), ", "));
    }
    out.println(id.getArtifactId() + groupId + " (" + versions + ")");
  }

  public void printDigest(PrintWriter out) {
    String groupId = id.isDefaultGroupId() ? "" : " [" + id.getGroupId() + "]";
    out.println(id.getArtifactId() + groupId + " (" + id.getVersion() + ")");
  }

  private Map<ModuleId, Dependency> computeManifest() {
    Map<ModuleId, Dependency> manifest = new HashMap<ModuleId, Dependency>();
    manifest.put(this.id, new Dependency(this));
    assert this.dependencies != null;
    for (Dependency dependency : this.dependencies) {
      if (dependency.isReference()) {
        Module module = this.modules.get(dependency.getId());
        assert module != null;
        for (Entry<ModuleId, Dependency> entry : module.computeManifest().entrySet()) {
          if (manifest.containsKey(entry.getKey())) continue;
          manifest.put(entry.getKey(), entry.getValue());
        }
        continue;
      }
      manifest.put(dependency.getId(), dependency);
    }
    return manifest;
  }

}
