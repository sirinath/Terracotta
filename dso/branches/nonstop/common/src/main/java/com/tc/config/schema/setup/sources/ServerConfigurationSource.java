/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config.schema.setup.sources;

import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.net.core.SecurityInfo;
import com.tc.security.PwProvider;
import com.tc.util.Assert;
import com.tc.util.io.ServerURL;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

/**
 * A {@link ConfigurationSource} that reads from a URL.
 *
 * @see URLConfigurationSourceTest
 */
public class ServerConfigurationSource implements ConfigurationSource {

  private final String       host;
  private final int          port;
  private final SecurityInfo securityInfo;
  private final PwProvider   pwProvider;

  public ServerConfigurationSource(final String host, final int port, final SecurityInfo securityInfo, final PwProvider pwProvider) {
      this.securityInfo = securityInfo;
    Assert.assertNotBlank(host);
    Assert.assertTrue(port > 0);
    this.host = host;
    this.port = port;
    this.pwProvider = pwProvider;
  }

  public InputStream getInputStream(long maxTimeoutMillis) throws IOException, ConfigurationSetupException {
    try {
      ServerURL theURL = new ServerURL(host, port, "/config" , (int)maxTimeoutMillis, securityInfo);

      // JDK: 1.4.2 - These settings are proprietary to Sun's implementation of java.net.URL in version 1.4.2
      System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(maxTimeoutMillis));
      System.setProperty("sun.net.client.defaultReadTimeout", String.valueOf(maxTimeoutMillis));

      return theURL.openStream(pwProvider);
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
