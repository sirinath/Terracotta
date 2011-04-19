/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

import org.osgi.framework.BundleException;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

import java.net.URL;
import java.util.Collections;

import junit.framework.TestCase;

public class KnopflerfishOSGiTest extends TestCase {

  private KnopflerfishOSGi osgiRuntime = null;

  @Override
  public void setUp() throws Exception {
    osgiRuntime = new KnopflerfishOSGi(new URL[0], Collections.EMPTY_LIST);
  }

  @Override
  public void tearDown() {
    osgiRuntime = null;
  }

  /**
   * Test that the Terracotta-RequireVersion value abides by the expected version pattern/format, and that it is caught
   * if it isn't.
   */
  public void testRequireVersionPatternCheck() {
    useGoodRequireVersion("off", IVersionCheck.IGNORED);
    useGoodRequireVersion("warn", IVersionCheck.OK);
    useGoodRequireVersion("enforce", IVersionCheck.OK);
    useGoodRequireVersion("strict", IVersionCheck.OK);

    useBadRequireVersion("off", IVersionCheck.IGNORED);
    useBadRequireVersion("warn", IVersionCheck.ERROR_BAD_REQUIRE_ATTRIBUTE);
    useBadRequireVersion("enforce", IVersionCheck.ERROR_BAD_REQUIRE_ATTRIBUTE);
    useBadRequireVersion("strict", IVersionCheck.ERROR_BAD_REQUIRE_ATTRIBUTE);
  }

  private void useGoodRequireVersion(String mode, int expected) {
    setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, mode);
    int actual = osgiRuntime.versionCheck(mode, "1.0.0.SNAPSHOT", "1.0.0.SNAPSHOT");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "1.0.0", "1.0.0");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "1.1.1.1", "1.1.1.1");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "1", "1");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "1.1.0-SNAPSHOT", "1.1.0-SNAPSHOT");
    assertEquals(expected, actual);
  }

  private void useBadRequireVersion(String mode, int expected) {
    setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, mode);

    int actual = osgiRuntime.versionCheck(mode, "A.B.C-SNAPSHOT", "A.B.C-SNAPSHOT");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "A.B.C.SNAPSHOT", "A.B.C.SNAPSHOT");
    assertEquals(expected, actual);

    actual = osgiRuntime.versionCheck(mode, "1.A.Z.SNAPSHOT", "1.A.Z.SNAPSHOT");
    assertEquals(expected, actual);
  }

  /**
   * Test that null or empty Terracotta-RequireVersion is forgiven or caught depending on the version check mode.
   */
  public void testNullOrEmptyRequireVersion() {
    useNullOrEmptyRequireVersion("off", null, IVersionCheck.IGNORED);
    useNullOrEmptyRequireVersion("warn", null, IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING);
    useNullOrEmptyRequireVersion("enforce", null, IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING);
    useNullOrEmptyRequireVersion("strict", null, IVersionCheck.ERROR_REQUIRE_ATTRIBUTE_MISSING);

    useNullOrEmptyRequireVersion("off", "", IVersionCheck.IGNORED);
    useNullOrEmptyRequireVersion("warn", "", IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING);
    useNullOrEmptyRequireVersion("enforce", "", IVersionCheck.WARN_REQUIRE_ATTRIBUTE_MISSING);
    useNullOrEmptyRequireVersion("strict", "", IVersionCheck.ERROR_REQUIRE_ATTRIBUTE_MISSING);
  }

  private void useNullOrEmptyRequireVersion(String mode, String version, int expected) {
    setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, mode);
    int actual = osgiRuntime.versionCheck(mode, version, "1.0.0");
    assertEquals(expected, actual);
  }

  /**
   * Test that version comparisons pass or fail depending on the version check mode.
   */
  public void testVersionMatching() {
    compareVersion("off", "1.0.0", "1.0.0", IVersionCheck.IGNORED);
    compareVersion("off", "1.0.0", "2.0.0", IVersionCheck.IGNORED);

    compareVersion("warn", "1.0.0", "1.0.0", IVersionCheck.OK);
    compareVersion("warn", "1.0.0", "2.0.0", IVersionCheck.WARN_INCORRECT_VERSION);

    compareVersion("enforce", "1.0.0", "1.0.0", IVersionCheck.OK);
    compareVersion("enforce", "1.0.0", "2.0.0", IVersionCheck.ERROR_INCORRECT_VERSION);

    compareVersion("strict", "1.0.0", "1.0.0", IVersionCheck.OK);
    compareVersion("strict", "1.0.0", "2.0.0", IVersionCheck.ERROR_INCORRECT_VERSION);
  }

  private void compareVersion(String mode, String version, String tcversion, int expected) {
    setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, mode);
    int actual = osgiRuntime.versionCheck(mode, version, tcversion);
    assertEquals(expected, actual);
  }

  /**
   * Test that valid modes are recognized
   */
  public void testSetVersionCheckMode() {
    try {
      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "off");
      String mode = osgiRuntime.versionCheckMode();
      assertEquals(IVersionCheck.OFF, mode);

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "warn");
      mode = osgiRuntime.versionCheckMode();
      assertEquals(IVersionCheck.WARN, mode);

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "enforce");
      mode = osgiRuntime.versionCheckMode();
      assertEquals(IVersionCheck.ENFORCE, mode);

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "strict");
      mode = osgiRuntime.versionCheckMode();
      assertEquals(IVersionCheck.STRICT, mode);
    } catch (BundleException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test that default mode is set when empty
   */
  public void testDefaultVersionCheckMode() {
    try {
      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "");
      String mode = osgiRuntime.versionCheckMode();
      assertEquals(IVersionCheck.OFF, mode);
    } catch (BundleException ex) {
      fail(ex.getMessage());
    }
  }

  /**
   * Test that invalid modes are caught
   */
  public void testBadVersionCheckMode() {
    String mode = null;
    try {
      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "foobar");
      mode = osgiRuntime.versionCheckMode();

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "OFF");
      mode = osgiRuntime.versionCheckMode();

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "WARN");
      mode = osgiRuntime.versionCheckMode();

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "ENFORCE");
      mode = osgiRuntime.versionCheckMode();

      setProperty(TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK, "STRICT");
      mode = osgiRuntime.versionCheckMode();
    } catch (BundleException ex) {
      assertNull(mode);
      return;
    }
    fail("Invalid mode value set for property " + TCPropertiesConsts.L1_MODULES_TC_VERSION_CHECK + mode);
  }

  private void setProperty(String key, String value) {
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    tcProps.setProperty(key, value);
  }

}
