/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package org.terracotta.modules.tool.config;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

/**
 * Global configuration for the TIM Update Center application.
 */
public class Config {
  private static final String KEYSPACE = "tim-update.";
  private String tcVersion;
  private URL    proxyUrl;
  private File   modulesDirectory;
  private URL    dataFileUrl;
  private File   dataFile;

  public static Config createConfig(Properties properties) {
    Config result = new Config();
    result.setTcVersion(getProperty(properties, "tcVersion"));
    result.setDataFile(new File(getProperty(properties, "dataFile")));
    result.setDataFileUrl(createUrl(getProperty(properties, "dataFileUrl"),
                          "dataFileUrl is not a valid URL"));
    try {
      result.setDataFileUrl(new URL(getProperty(properties, "dataFileUrl")));
    } catch (MalformedURLException e) {
      throw new InvalidConfigurationException("dataFileUrl is not a valid URL", e);
    }

    String proxy = getProperty(properties, "proxyUrl");
    if (proxy != null)
      result.setProxyUrl(createUrl(proxy, "Proxy URL is not a valid URL"));

    return result;
  }

  private static URL createUrl(String urlString, String errorMessage) {
    try {
      return new URL(urlString);
    } catch (MalformedURLException e) {
      throw new InvalidConfigurationException(errorMessage, e);
    }
  }

  private static String getProperty(Properties props, String name) {
    return getProperty(props, name, null);
  }

  private static String getProperty(Properties props, String name, String defaultValue) {
    return props.getProperty(KEYSPACE + name, defaultValue);
  }
/*
  private static final Pattern variablePattern = Pattern.compile("\\$\\{(.*?)\\}");
  private static String interpolate(String value) {
    Matcher matcher = variablePattern.matcher(value);
    StringBuffer buf = new StringBuffer();
    int startIndex = 0;
    while (matcher.find()) {
      
    }
  }
*/
  public URL getProxyUrl() {
    return proxyUrl;
  }
  public void setProxyUrl(URL proxyUrl) {
    this.proxyUrl = proxyUrl;
  }
  public String getTcVersion() {
    return tcVersion;
  }
  public void setTcVersion(String tcVersion) {
    this.tcVersion = tcVersion;
  }
  public File getModulesDirectory() {
    return modulesDirectory;
  }
  public void setModulesDirectory(File modulesDirectory) {
    this.modulesDirectory = modulesDirectory;
  }
  public URL getDataFileUrl() {
    return dataFileUrl;
  }
  public void setDataFileUrl(URL dataFileUrl) {
    this.dataFileUrl = dataFileUrl;
  }
  public File getDataFile() {
    return dataFile;
  }
  public void setDataFile(File dataFileDirectory) {
    this.dataFile = dataFileDirectory;
  }
}
