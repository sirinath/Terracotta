/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.offheap;

import org.junit.Assert;

import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;

/**
 *
 * @author mscott
 */
public class ManualOffHeapConfigTest extends TCTestCase {

  @Override
  public void setUp() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "4k");
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, "8M");
  }

  public void testHighMinPageSize() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "8M");
    OffHeapConfig config = new OffHeapConfig(true, "10G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
  }

  public void testBrokenPageSizing() {
    TCProperties props = TCPropertiesImpl.getProperties();
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MIN_PAGE_SIZE, "8M");
    props.setProperty(TCPropertiesConsts.L2_OFFHEAP_MAX_PAGE_SIZE, "1M");
    OffHeapConfig config = new OffHeapConfig(true, "10G", true) {};
    Assert.assertTrue(config.getMinMapPageSize() <=config.getMaxMapPageSize());
  }
}
