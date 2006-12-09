/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import org.apache.commons.io.CopyUtils;

import com.tc.config.Directories;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.license.InvalidLicenseException;
import com.tc.license.StandardTerracottaLicense;
import com.tc.license.TerracottaLicense;
import com.tc.util.Assert;
import com.tc.util.Resolve;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.text.ParseException;

public final class ResolveLicense implements Resolve {

  public  static final String   LICENSE                          = "license.lic";
  private static final String   LICENSE_PUBLIC_KEY_RESOURCE_NAME = "license-public-key.x509";
  private static final String   KEY_MSG                          = "Unable to read the public key for the license file from resource";
  private static final String   LIC_MSG                          = "We couldn't load data from the specified license file,";
  private static TerracottaLicense license;

  private ResolveLicense() {
    // cannot instantiate
  }

  public static synchronized TerracottaLicense getLicense() throws InvalidLicenseException, ConfigurationSetupException {
    if (license != null) return license;
    FileInputStream in;
    PublicKey terracottaLicensePublicKey;
    String tcLicPath;
    
    try {
      tcLicPath = Directories.getInstallationRoot() + File.separator + LICENSE;
    } catch (FileNotFoundException e) {
      throw new ConfigurationSetupException(e.getMessage(), e);
    }
    File tcLic = new File(tcLicPath);

    if (!tcLic.exists()) {
      System.out.println("\nNo License Specified.");
      System.out.println("Looking for: " + tcLicPath);
      String licReqUrl = "http://www.terracottatech.com/request.jsp?page=license";
      System.out.println("For information on obtaining a license see "+licReqUrl);
      System.out.println("Server Shutting Down...\n");
      System.out.flush();
      System.exit(3);
    }

    try {
      InputStream keyIn = StandardTerracottaLicense.class.getResourceAsStream(LICENSE_PUBLIC_KEY_RESOURCE_NAME);
      Assert.assertNotNull(keyIn);
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      CopyUtils.copy(keyIn, baos);
      X509EncodedKeySpec keySpec = new X509EncodedKeySpec(baos.toByteArray());
      KeyFactory keyFactory = KeyFactory.getInstance("DSA");
      terracottaLicensePublicKey = keyFactory.generatePublic(keySpec);

    } catch (IOException ioe) {
      throw new ConfigurationSetupException(KEY_MSG + " '" + LICENSE_PUBLIC_KEY_RESOURCE_NAME + "'.", ioe);
    } catch (NoSuchAlgorithmException nsae) {
      throw new ConfigurationSetupException(KEY_MSG + " '" + LICENSE_PUBLIC_KEY_RESOURCE_NAME + "'.", nsae);
    } catch (InvalidKeySpecException ikse) {
      throw new ConfigurationSetupException(KEY_MSG + " '" + LICENSE_PUBLIC_KEY_RESOURCE_NAME + "'.", ikse);
    }

    try {
      in = new FileInputStream(tcLic);
      return license = new StandardTerracottaLicense(in, Signature.getInstance("SHA1withDSA"), terracottaLicensePublicKey);

    } catch (IOException ioe) {
      throw new InvalidLicenseException(LIC_MSG + " '" + tcLicPath + "'.", ioe);
    } catch (InvalidKeyException ike) {
      throw new InvalidLicenseException(LIC_MSG + " '" + tcLicPath + "'.", ike);
    } catch (SignatureException se) {
      throw new InvalidLicenseException(LIC_MSG + " '" + tcLicPath + "'.", se);
    } catch (NoSuchAlgorithmException nsae) {
      throw new InvalidLicenseException(LIC_MSG + " '" + tcLicPath + "'.", nsae);
    } catch (ParseException pe) {
      throw new InvalidLicenseException(LIC_MSG + " '" + tcLicPath + "'.", pe);
    }
  }
}
