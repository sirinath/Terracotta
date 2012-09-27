/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import org.terracotta.license.License;

public class LicenseHelper {

  public static final Object EXIT_MESSAGE = LicenseManagerOld.EXIT_MESSAGE;
  public static final int    WARNING_MARK = LicenseManagerOld.WARNING_MARK;
  public static final long   HOUR         = LicenseManagerOld.HOUR;
  public static final String EXPIRED_ERROR = LicenseManagerOld.EXPIRED_ERROR;
  public static final String EXPIRY_WARNING = LicenseManagerOld.EXPIRY_WARNING;

  public static void assertLicenseValid() {
    LicenseManagerOld.assertLicenseValid();

  }

  public static int maxClientCount() {
    return LicenseManagerOld.maxClientCount();
  }

  public static License getLicense() {
    return LicenseManagerOld.getLicense();
  }

  public static void verifyServerStripingCapability() {
    LicenseManagerOld.verifyServerStripingCapability();

  }

  public static void verifyCapability(String capability) {
    LicenseManagerOld.verifyCapability(capability);

  }

  public static void verifyOperatorConsoleCapability() {
    LicenseManagerOld.verifyOperatorConsoleCapability();

  }

  public static void verifySecurityCapability() {
    LicenseManagerOld.verifySecurityCapability();

  }

  public static boolean enterpriseEdition() {
    return LicenseManagerOld.enterpriseEdition();
  }

  public static String licensedCapabilities() {
    return LicenseManagerOld.licensedCapabilities();
  }

  public static void verifyServerArrayOffheapCapability(String maxDataSize) {
    LicenseManagerOld.verifyServerArrayOffheapCapability(maxDataSize);

  }

  public static void verifyAuthenticationCapability() {
    LicenseManagerOld.verifyAuthenticationCapability();
  }

}
