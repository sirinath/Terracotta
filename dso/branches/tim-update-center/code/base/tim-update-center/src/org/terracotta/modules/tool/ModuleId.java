/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

/**
 * Composite unique identifier for TIMs.
 *
 * @author Jason Voegele (jvoegele@terracotta.org)
 */
public class ModuleId implements Comparable {
  private final String groupId;
  private final String artifactId;
  private final String version;

  public ModuleId(String groupId, String artifactId, String version) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
  }

  /** The TIM groupId. */
  public String getGroupId() {
    return groupId;
  }

  /** The TIM artifactId. */
  public String getArtifactId() {
    return artifactId;
  }

  /** The TIM version. */
  public String getVersion() {
    return version;
  }

  /**
   * Converts this TimIdentifier to a String of the form groupId.artifactId-version.
   */
  public String toString() {
    return groupId + "." + artifactId + "-" + version;
  }

  public String toSortableString() {
    return groupId + "." + artifactId + "-" + pad(version);
  }

  /**
   * Sibling Modules are Modules with matching groupId and artifactId.
   */
  public boolean isSibling(ModuleId id) {
    return this.groupId.equals(id.getGroupId()) && this.artifactId.equals(id.getArtifactId());
  }

  public int compareTo(Object o) {
    ModuleId other = (ModuleId) o;
    return this.toSortableString().compareTo(other.toSortableString());
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
    result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
    result = prime * result + ((version == null) ? 0 : version.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    final ModuleId other = (ModuleId) obj;
    if (artifactId == null) {
      if (other.artifactId != null) return false;
    } else if (!artifactId.equals(other.artifactId)) return false;
    if (groupId == null) {
      if (other.groupId != null) return false;
    } else if (!groupId.equals(other.groupId)) return false;
    if (version == null) {
      if (other.version != null) return false;
    } else if (!version.equals(other.version)) return false;
    return true;
  }

  private String pad(String versionString) {
    return versionString;
  }
}
