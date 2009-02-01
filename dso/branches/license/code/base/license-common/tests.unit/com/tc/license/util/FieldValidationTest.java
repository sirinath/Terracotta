package com.tc.license.util;

import junit.framework.TestCase;

public class FieldValidationTest extends TestCase {

  public void testRequiredFields() throws Exception {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();
    requiredfieldTestTemplate(descriptor, LicenseConstants.LICENSE_TYPE);
    requiredfieldTestTemplate(descriptor, LicenseConstants.LICENSE_NUMBER);
    requiredfieldTestTemplate(descriptor, LicenseConstants.LICENSEE);
    requiredfieldTestTemplate(descriptor, LicenseConstants.MAX_CLIENTS);
    requiredfieldTestTemplate(descriptor, LicenseConstants.PRODUCT);
  }

  public void testLicenseType() throws Exception {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();

    LicenseField field = descriptor.createField(LicenseConstants.LICENSE_TYPE);
    field.setRawValue(LicenseConstants.COMMERCIAL);
    field.setRawValue(LicenseConstants.TRIAL);

    try {
      field.setRawValue("DUDE");
      fail("Should have thrown IllegalArgumentException");
    } catch (LicenseException e) {
      // expected
    }
  }

  public void testExpirationField() throws Exception {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();

    LicenseField field = descriptor.createField(LicenseConstants.EXPIRATION_DATE);
    field.setRawValue(null);

    field.setRawValue("2009-01-01");

    try {
      field.setRawValue("01-01-2009");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }

    try {
      field.setRawValue("2009-02-29");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }

  public void testProduct() throws Exception {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();

    LicenseField field = descriptor.createField(LicenseConstants.PRODUCT);
    field.setRawValue(LicenseConstants.FX);
    field.setRawValue(LicenseConstants.EX);
    field.setRawValue(LicenseConstants.EX_SESSIONS);

    try {
      field.setRawValue("DUDE");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }

  public void testMaxClients() throws Exception {
    LicenseDescriptor descriptor = LicenseDescriptor.getInstance();

    LicenseField field = descriptor.createField(LicenseConstants.MAX_CLIENTS);
    field.setRawValue("1");
    field.setRawValue("4");

    try {
      field.setRawValue("0");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }

    try {
      field.setRawValue("-1");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }

    try {
      field.setRawValue("asdf");
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }

  private void requiredfieldTestTemplate(LicenseDescriptor descriptor, String fieldName) {
    LicenseField field = descriptor.createField(fieldName);

    try {
      field.setRawValue(null);
      fail("Should have thrown LicenseException");
    } catch (LicenseException e) {
      // expected
    }
  }
}
