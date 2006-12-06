/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.license;

import com.tc.test.EqualityChecker;
import com.tc.test.TCTestCase;
import com.tc.util.TCAssertionError;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.ProviderException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

/**
 * Unit test for {@link StandardTerracottaLicense}.
 */
public class StandardTerracottaLicenseTest extends TCTestCase {

  private Date             expirationDate;

  private KeyPairGenerator generator;

  private PublicKey        publicKey;
  private PrivateKey       privateKey;

  private Signature        signingSignature;
  private Signature        verificationSignature;

  public void setUp() throws Exception {
    this.expirationDate = new Date();
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(this.expirationDate);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    this.expirationDate = cal.getTime();

    this.generator = KeyPairGenerator.getInstance("DSA");
    this.generator.initialize(1024);

    KeyPair keyPair = this.generator.generateKeyPair();
    this.publicKey = keyPair.getPublic();
    this.privateKey = keyPair.getPrivate();

    this.signingSignature = Signature.getInstance("SHA1withDSA");
    this.verificationSignature = Signature.getInstance("SHA1withDSA");
  }

  public void runBare() throws Throwable {
    try {
      super.runBare();
    } catch (ProviderException pe) {
      // / XXX: debugging -- this exception happened a few times in the monkey.
      // Maybe having this data archived will help debug?
      File tempFile = getTempFile("debug-" + getName() + ".ser");
      ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tempFile));
      oos.writeObject(this.expirationDate);
      oos.writeObject(this.privateKey);
      oos.writeObject(this.publicKey);
      oos.close();
      throw pe;
    }
  }

  public void testConstruction() throws Exception {
    try {
      new StandardTerracottaLicense(null, this.verificationSignature, this.publicKey);
      fail("Didn't get NPE on no reader");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(new ByteArrayInputStream("foobar".getBytes()), null, this.publicKey);
      fail("Didn't get NPE on no signature");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(new ByteArrayInputStream("foobar".getBytes()), null, null);
      fail("Didn't get NPE on no public key");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, null, StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    102389, TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get NPE on no licensee");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", null, this.expirationDate, 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get NPE on no license type");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", "", new Date(), 2, 102389, TerracottaLicense.ALL_POSSIBLE_MODULES,
                                    true);
      fail("Didn't get IAE on empty license type");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", "", new Date(), 2, 102389, TerracottaLicense.ALL_POSSIBLE_MODULES,
                                    false);
      fail("Didn't get IAE on empty license type");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", "    ", new Date(), 2, 102389, TerracottaLicense.ALL_POSSIBLE_MODULES,
                                    false);
      fail("Didn't get IAE on blank license type");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", "whatever", new Date(), 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get IAE on wrong license type");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get IAE on empty licensee");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "   ", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get IAE on blank licensee");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(0, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on zero serial number");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(-1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 2, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on negative serial number");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 0, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on zero max-L1 connections");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), -1, 102389,
                                    TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on negative max-L1 connections");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    -1, TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on negative max-L2 runtime");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    0, TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on zero max-L2 runtime");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    9, TerracottaLicense.ALL_POSSIBLE_MODULES, false);
      fail("Didn't get TCAE on too-small max-L2 runtime");
    } catch (TCAssertionError tcae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    102389, null, false);
      fail("Didn't get NPE on null module list");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    102389, new String[] { TerracottaLicense.MODULE_DSO, null }, false);
      fail("Didn't get NPE on null module");
    } catch (NullPointerException npe) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 2,
                                    102389, new String[] { TerracottaLicense.MODULE_DSO, "" }, false);
      fail("Didn't get IAE on empty module");
    } catch (IllegalArgumentException iae) {
      // ok
    }

    try {
      new StandardTerracottaLicense(1, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, new Date(), 2, 102389,
                                    new String[] { TerracottaLicense.MODULE_DSO, "    " }, false);
      fail("Didn't get IAE on blank module");
    } catch (IllegalArgumentException iae) {
      // ok
    }
  }

  public void testReadWrite() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    StandardTerracottaLicense out = new StandardTerracottaLicense(1, "foobar",
                                                                  StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                  this.expirationDate, 37, 3842952,
                                                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false);
    out.writeTo(baos, this.signingSignature, this.privateKey);

    StandardTerracottaLicense in = new StandardTerracottaLicense(new ByteArrayInputStream(baos.toByteArray()),
                                                                 this.verificationSignature, this.publicKey);

    assertEquals(out, in);
  }

  public void testLicenseeSpaces() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    StandardTerracottaLicense out = new StandardTerracottaLicense(1, "   bonk this   is cool  ",
                                                                  StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                  this.expirationDate, 37, 3842952,
                                                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false);
    out.writeTo(baos, this.signingSignature, this.privateKey);

    StandardTerracottaLicense in = new StandardTerracottaLicense(new ByteArrayInputStream(baos.toByteArray()),
                                                                 this.verificationSignature, this.publicKey);

    assertEquals(out, in);
  }

  public void testDataCorruption() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    StandardTerracottaLicense out = new StandardTerracottaLicense(1, "foobar",
                                                                  StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                  this.expirationDate, 37, 3842952,
                                                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false);
    out.writeTo(baos, this.signingSignature, this.privateKey);

    BufferedReader reader = new BufferedReader(new StringReader(new String(baos.toByteArray())));
    StringWriter sw = new StringWriter();

    String line;
    boolean found = false;

    while ((line = reader.readLine()) != null) {
      if (line.trim().toLowerCase().startsWith("serial number")) {
        assertFalse(found);
        line = "Serial Number: 3";
        found = true;
      }

      sw.write(line + "\n");
    }

    assertTrue(found);

    try {
      new StandardTerracottaLicense(new ByteArrayInputStream(sw.toString().getBytes()), this.verificationSignature,
                                    this.publicKey);
      fail("Didn't get SignatureException on corrupt data.");
    } catch (SignatureException se) {
      // ok
    }
  }

  public void testLineEndingTransformation() throws Exception {
    checkLineEndingTransformation("\n");
    checkLineEndingTransformation("\r");
    checkLineEndingTransformation("\r\n");
  }

  public void checkLineEndingTransformation(String newLineEnding) throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    StandardTerracottaLicense out = new StandardTerracottaLicense(1, "foobar",
                                                                  StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                  this.expirationDate, 37, 3842952,
                                                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false);
    out.writeTo(baos, this.signingSignature, this.privateKey);

    BufferedReader reader = new BufferedReader(new StringReader(new String(baos.toByteArray())));
    StringWriter sw = new StringWriter();

    String line;

    while ((line = reader.readLine()) != null) {
      sw.write(line.trim() + newLineEnding);
    }

    StandardTerracottaLicense in = new StandardTerracottaLicense(new ByteArrayInputStream(sw.toString().getBytes()),
                                                                 this.verificationSignature, this.publicKey);

    assertEquals(out, in);
  }

  public void testSignatureCorruption() throws Exception {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();

    StandardTerracottaLicense out = new StandardTerracottaLicense(1, "foobar",
                                                                  StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                  this.expirationDate, 37, 3842952,
                                                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false);
    out.writeTo(baos, this.signingSignature, this.privateKey);

    BufferedReader reader = new BufferedReader(new StringReader(new String(baos.toByteArray())));
    StringWriter sw = new StringWriter();

    String line;
    boolean found = false, justAfterFound = false;

    while ((line = reader.readLine()) != null) {
      if (line.trim().toLowerCase().indexOf("begin signature") > 0) {
        assertFalse(found);
        found = true;
        justAfterFound = true;
      } else if (justAfterFound) {
        line = "XXX" + line.substring(3);
        justAfterFound = false;
      }

      sw.write(line + "\n");
    }

    assertTrue(found);

    try {
      new StandardTerracottaLicense(new ByteArrayInputStream(sw.toString().getBytes()), this.verificationSignature,
                                    this.publicKey);
      fail("Didn't get SignatureException on corrupt data.");
    } catch (SignatureException se) {
      // ok
    }
  }

  public void testComponents() throws Exception {
    StandardTerracottaLicense license = new StandardTerracottaLicense(
                                                                      942,
                                                                      "foobar",
                                                                      StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                      this.expirationDate, 37, 3842952,
                                                                      TerracottaLicense.ALL_POSSIBLE_MODULES, true);

    assertEquals(942, license.serialNumber());
    assertEquals("foobar", license.licensee());
    assertEquals(StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION, license.licenseType());
    assertEquals(this.expirationDate, license.l2ExpiresOn());
    assertEquals(37, license.maxL2Connections());
    assertEquals(3842952000L, license.maxL2RuntimeMillis());
    assertEquals(true, license.dsoHAEnabled());

    for (int i = 0; i < TerracottaLicense.ALL_POSSIBLE_MODULES.length; ++i) {
      assertTrue(license.isModuleEnabled(TerracottaLicense.ALL_POSSIBLE_MODULES[i]));
    }
  }

  public void testEnabledModules() throws Exception {
    StandardTerracottaLicense license = new StandardTerracottaLicense(
                                                                      942,
                                                                      "foobar",
                                                                      StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                                                      this.expirationDate,
                                                                      37,
                                                                      3842952,
                                                                      new String[] { TerracottaLicense.MODULE_SESSION_REPLICATION_TOMCAT },
                                                                      false);

    assertTrue(license.isModuleEnabled(TerracottaLicense.MODULE_SESSION_REPLICATION_TOMCAT));
  }

  public void testEqualsAndHashCode() throws Exception {
    Date otherExpirationDate = new Date(432890542L);
    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
    cal.setTime(otherExpirationDate);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    otherExpirationDate = cal.getTime();

    Date otherExpirationDateClone = new Date(otherExpirationDate.getTime());

    Object[] arr1 = new Object[] {
        new StandardTerracottaLicense(941, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobaz", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      otherExpirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 48, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 48, 3842952,
                                      new String[] { TerracottaLicense.MODULE_SESSION_REPLICATION_TOMCAT }, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate,
                                      48, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      otherExpirationDate, 37, 3852952, TerracottaLicense.ALL_POSSIBLE_MODULES, false) };
    Object[] arr2 = new Object[] {
        new StandardTerracottaLicense(941, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobaz", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      otherExpirationDate, 37, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 48, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      this.expirationDate, 48, 3842952,
                                      new String[] { TerracottaLicense.MODULE_SESSION_REPLICATION_TOMCAT }, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate,
                                      48, 3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false),
        new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                      otherExpirationDateClone, 37, 3852952, TerracottaLicense.ALL_POSSIBLE_MODULES,
                                      false) };

    EqualityChecker.checkArraysForEquality(arr1, arr2);
  }

  public void testToString() throws Exception {
    new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_TRIAL, this.expirationDate, 37,
                                  3842952, TerracottaLicense.ALL_POSSIBLE_MODULES, false).toString();
    new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                  this.expirationDate, Integer.MAX_VALUE, 3842952,
                                  TerracottaLicense.ALL_POSSIBLE_MODULES, false).toString();
    new StandardTerracottaLicense(942, "foobar", StandardTerracottaLicense.LICENSE_TYPE_PRODUCTION,
                                  this.expirationDate, 37, Integer.MAX_VALUE, TerracottaLicense.ALL_POSSIBLE_MODULES,
                                  false).toString();
  }

}
