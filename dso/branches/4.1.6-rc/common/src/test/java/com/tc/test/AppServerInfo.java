/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AppServerInfo {
  public static final int      WEBLOGIC              = 0;
  public static final int      JBOSS                 = 1;
  public static final int      TOMCAT                = 2;
  public static final int      WASCE                 = 3;
  public static final int      GLASSFISH             = 4;
  public static final int      JETTY                 = 5;
  public static final int      RESIN                 = 6;
  public static final int      WEBSPHERE             = 7;

  private final int            id;
  private final String         name;
  private final String         major;
  private final String         minor;

  private static final Pattern nameAndVersionPattern = Pattern.compile("^(.+)-(\\d+)\\.(.+)$");

  /**
   * Creates a new {@link AppServerInfo} object whose properties are parsed from the given
   * <code>nameAndVersion> string, which must be of the form
   * name-major-version.minor-version.
   * 
   * @throws IllegalArgumentException if the <code>nameAndVersion</code> does not parse properly.
   */
  public static AppServerInfo parse(final String nameAndVersion) {
    Matcher matcher = nameAndVersionPattern.matcher(nameAndVersion);
    if (!matcher.matches()) { throw new IllegalArgumentException("Cannot parse appserver specification: "
                                                                 + nameAndVersion); }
    return new AppServerInfo(matcher.group(1), matcher.group(2), matcher.group(3));
  }

  public AppServerInfo(String name, String majorVersion, String minorVersion) {
    this.name = name;
    this.major = majorVersion;
    this.minor = minorVersion;

    if (name == null) throw new RuntimeException("No appserver name has been set");

    if (name.startsWith("weblogic")) {
      id = WEBLOGIC;
    } else if (name.startsWith("jboss")) {
      id = JBOSS;
    } else if (name.startsWith("tomcat")) {
      id = TOMCAT;
    } else if (name.startsWith("wasce")) {
      id = WASCE;
    } else if (name.startsWith("glassfish")) {
      id = GLASSFISH;
    } else if (name.startsWith("jetty")) {
      id = JETTY;
    } else if (name.startsWith("resin")) {
      id = RESIN;
    } else if (name.startsWith("websphere")) {
      id = WEBSPHERE;
    } else {
      id = -1;
    }
  }

  public int getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getMajor() {
    return major;
  }

  public String getMinor() {
    return minor;
  }

  @Override
  public String toString() {
    return name + "-" + major + "." + minor;
  }
}
