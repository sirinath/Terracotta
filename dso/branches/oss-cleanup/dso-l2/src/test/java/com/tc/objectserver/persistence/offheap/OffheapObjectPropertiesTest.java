/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.persistence.offheap;

import com.tc.test.TCTestCase;
import com.tc.util.Assert;

public class OffheapObjectPropertiesTest extends TCTestCase {

  public void testBasic() throws Exception {
    runCase(OffheapObjectProperties.ONE_MB * 512, "512 MB");
    runCase(OffheapObjectProperties.ONE_MB * 1023, "1023 MB");
    runCase(OffheapObjectProperties.ONE_GB * 2, "2 GB");
    runCase(OffheapObjectProperties.ONE_GB * 512, "4 GB");
    runCase(OffheapObjectProperties.ONE_GB * 8, "8 GB");
    runCase(OffheapObjectProperties.ONE_GB * 50, "50 GB");
  }

  public void runCase(long memoryGiven, String memory) throws Exception {
    OffheapObjectProperties props = new OffheapObjectProperties(
                                                                memoryGiven
                                                                    * (int) (100.0 / OffheapObjectProperties.OBJECT_PERCENTAGE));
    long mem = (props.getObjectConcurrency() * props.getObjectInitialDataSize()) + props.getObjectTableSize() * 16;

    System.out.println(memory + " === " + props.toString());
    System.out.println(memory + " === Mem required " + mem);

    Assert.assertTrue(props.getObjectConcurrency() <= OffheapObjectProperties.OBJECT_CONCURRENCY_ABOVE_4GB);
    Assert.assertTrue(props.getObjectConcurrency() > 0);

    Assert.assertTrue(props.getObjectInitialDataSize() <= OffheapObjectProperties.OBJECT_MAX_INIT_DATA_SIZE);
    Assert.assertTrue(props.getObjectInitialDataSize() >= OffheapObjectProperties.OBJECT_MIN_INIT_DATA_SIZE);

    Assert.assertTrue(props.getObjectTableSize() <= OffheapObjectProperties.OBJECT_TABLE_SIZE_ABOVE_1GB);
    Assert.assertTrue(props.getObjectTableSize() >= OffheapObjectProperties.OBJECT_TABLE_SIZE_BELOW_1GB);

    Assert.assertTrue(mem < memoryGiven);
  }
}