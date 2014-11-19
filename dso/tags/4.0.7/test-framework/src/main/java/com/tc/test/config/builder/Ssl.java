/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test.config.builder;

/**
 * @author Ludovic Orban
 */
public class Ssl {

  private String certificate;

  public String getCertificate() {
    return certificate;
  }

  public void setCertificate(String certificate) {
    this.certificate = certificate;
  }

  public Ssl certificate(String certificate) {
    setCertificate(certificate);
    return this;
  }

}
