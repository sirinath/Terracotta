/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.net.URL;
import java.util.Collections;
import java.util.List;

/**
 * A single Terracotta Integration Module (TIM) artifact.
 *
 * A TIM has a composite unique identifier consisting of groupId, artifactId,
 * and version, which is represented by the {@link TimIdentifier} class.
 *
 * Note that TIMs that are packaged together into an archive are still
 * represented as separate Tim objects.
 */
/*
 * <module artifactId='tim-hibernate-3.1.2' version='1.1.2' groupId='org.terracotta.modules'>
  <tc-version>2.6.2</tc-version>
  <website>http://forge-dev.terracotta.lan/releases/projects/tim-hibernate/</website>
  <vendor>Terracotta, Inc.</vendor>
  <copyright>Copyright (c) 2007 Terracotta, Inc.</copyright>
  <category>Terracotta Integration Module</category>
  <description>Terracotta Integration Module for clustering Hibernate</description>
  <repoUrl>
    http://forge-dev.terracotta.lan/repo/org/terracotta/modules/tim-hibernate-3.1.2/1.1.2/tim-hibernate-3.1.2-1.1.2.jar
  </repoUrl>
  <installPath>org/terracotta/modules/tim-hibernate-3.1.2/1.1.2</installPath>
  <filename>tim-hibernate-3.1.2-1.1.2.jar</filename>
  <dependencies>
    <module artifactId='modules-common' version='2.6.2' groupId='org.terracotta.modules'>
      <repoUrl>
        http://forge-dev.terracotta.lan/repo/org/terracotta/modules/modules-common/2.6.2/modules-common-2.6.2.jar
      </repoUrl>
      <installPath>org/terracotta/modules/modules-common/2.6.2</installPath>
      <filename>modules-common-2.6.2.jar</filename>
    </module>
    <moduleRef artifactId='tim-cglib-2.1.3' version='1.1.2' groupId='org.terracotta.modules'/>
    <moduleRef artifactId='tim-ehcache-commons' version='1.1.2' groupId='org.terracotta.modules'/>
    <moduleRef artifactId='tim-ehcache-1.3' version='1.1.2' groupId='org.terracotta.modules'/>
  </dependencies> 
 */
public class Tim {
  private final TimIdentifier id;
  private final String terracottaVersion;
  private final String webSite;
  private final String vendor;
  private final String copyright;
  private final String category;
  private final String description;
  private final String repoUrl;
  private final String installPath;
  private final String fileName;
  private final List<TimIdentifier> dependencies;

  public static Tim parse(String xml) {
    return null;
  }

  /** Returns the composite unique identifier for this TIM. */
  public TimIdentifier getTimId() {
    return id;
  }

  /** The URL from which the TIM JAR file can be downloaded. */
  public URL getDownloadUrl() {
    return repoUrl;
  }

  /** Returns the description of this TIM. */
  public String getDescription() {
    return description;
  }

  /**
   * A list of this TIM's direct dependencies.
   */
  public List<TimIdentifier> dependencies() {
    return Collections.unmodifiableList(dependencies);
  }

  /**
   * A flattened list of transitive dependencies for this TIM.
   */
  //public List<Tim> transitiveDependencies();
}
