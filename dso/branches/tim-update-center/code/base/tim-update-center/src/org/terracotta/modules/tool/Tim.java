/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool;

import java.net.URL;
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
public interface Tim {

  /** Returns the composite unique identifier for this TIM. */
  public TimIdentifier getTimId();

  /** The URL from which the TIM JAR file can be downloaded. */
  public URL getDownloadUrl();

  /** Returns the description of this TIM. */
  public String getDescription();

  /**
   * A list of this TIM's direct dependencies.
   */
  public List<Tim> dependencies();

  /**
   * A flattened list of transitive dependencies for this TIM.
   */
  public List<Tim> transitiveDependencies();
}
