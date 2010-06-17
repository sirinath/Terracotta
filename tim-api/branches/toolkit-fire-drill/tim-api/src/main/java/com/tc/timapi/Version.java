/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.timapi;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class should really never be changed. If you screw with the method names or the package of this class then the
 * core client will not be able to report TIM API version mismatches very well
 */
public class Version {

  private final int     major;
  private final int     minor;
  private final int     incremental;
  private final String  qualifier;
  private final boolean isSnapshot;
  private final String  versionString;

  private static class DefaultInstanceHolder {
    static final Version INSTANCE = loadDefault();

    private static Version loadDefault() {
      InputStream in = Version.class.getResourceAsStream("version.properties");
      if (in == null) { throw new AssertionError("Missing version.properties resource"); }

      try {
        Properties props = new Properties();
        props.load(in);
        return new Version(props);
      } catch (IOException e) {
        throw new AssertionError(e);
      } finally {
        try {
          in.close();
        } catch (IOException ioe) {
          // ignore
        }
      }
    }
  }

  Version(Properties props) {
    String ver = props.getProperty("version");
    if (ver == null) { throw new AssertionError("missing version property"); }
    ver = ver.trim();

    Pattern pattern = Pattern.compile("^(\\d+)\\.(\\d+)\\.(\\d+)-?(.*)?$");
    Matcher m = pattern.matcher(ver);
    if (!m.matches()) { throw new AssertionError("unexpected version string: " + ver); }

    versionString = ver;
    major = Integer.parseInt(m.group(1));
    minor = Integer.parseInt(m.group(2));
    incremental = Integer.parseInt(m.group(3));
    qualifier = m.group(4);
    isSnapshot = "SNAPSHOT".equals(qualifier);
  }

  public static Version getVersion() {
    return DefaultInstanceHolder.INSTANCE;
  }

  public boolean isSnapshot() {
    return isSnapshot;
  }

  public int getMajor() {
    return major;
  }

  public int getMinor() {
    return minor;
  }

  public int getIncremental() {
    return incremental;
  }

  public String getQualifier() {
    return qualifier;
  }

  public String getFullVersionString() {
    return versionString;
  }

}
