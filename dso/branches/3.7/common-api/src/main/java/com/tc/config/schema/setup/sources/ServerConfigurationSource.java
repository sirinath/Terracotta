/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.util.Assert;
import com.tc.util.io.ServerURL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * A {@link ConfigurationSource} that reads from a URL.
 */
public class ServerConfigurationSource implements ConfigurationSource {

  private final String host;
  private final int port;

  public ServerConfigurationSource(String host, int port) {
    Assert.assertNotBlank(host);
    Assert.assertTrue(port > 0);
    this.host = host;
    this.port = port;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws IOException, ConfigurationSetupException {
    try {
      String protocol = "http";
      if (Boolean.getBoolean("tc.ssl")) {
        protocol = "https";
      }

      ServerURL theURL = new ServerURL(protocol, host, port, "/config" , (int)maxTimeoutMillis);
      return theURL.openStream();
    } catch (MalformedURLException murle) {
      throw new ConfigurationSetupException("Can't load configuration from "+this+".");
    }
  }

  public File directoryLoadedFrom() {
    return null;
  }

  public boolean isTrusted() {
    return true;
  }

  public String toString() {
    return "server at '" + this.host + ":" + this.port + "'";
  }

}
