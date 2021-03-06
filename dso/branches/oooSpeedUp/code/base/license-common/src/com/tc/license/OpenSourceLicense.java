/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.license;

import java.util.Date;

public class OpenSourceLicense implements License {
  private final Capabilities capabilities;

  public OpenSourceLicense(Capabilities openSourceCapabilities) {
    capabilities = openSourceCapabilities;
  }

  public Capabilities capabilities() {
    return capabilities;
  }

  public Date expirationDate() {
    return null;
  }

  public String getSignature() {
    return "N/A";
  }

  public String licenseNumber() {
    return "N/A";
  }

  public String licenseType() {
    return "N/A";
  }

  public String licensee() {
    return "Opensource Community";
  }

  public int maxClients() {
    return Integer.MAX_VALUE;
  }

  public String product() {
    return "Community";
  }

  public void setSignature(String signature) {
    // do nothing
  }

  public String edition() {
    return "Community";
  }
  
  public byte[] getCanonicalData() {
    return null;
  }

}
