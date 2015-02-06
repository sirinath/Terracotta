/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.test;

import com.tc.util.runtime.Vm;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

public class TCTestCaseTest extends TCTestCase {

  public TCTestCaseTest() {
    if (Vm.isIBM()) {
      disableTest();
    }
  }

  public void testHeapDump() throws IOException {
    if (Vm.isJDK16Compliant()) {
      assertEquals(0, getHprofs().length);
      dumpHeap(getTempDirectory());
      assertEquals(1, getHprofs().length);
      dumpHeap(getTempDirectory());
      assertEquals(2, getHprofs().length);
      dumpHeap(getTempDirectory());
    }
  }

  private File[] getHprofs() throws IOException {
    return getTempDirectory().listFiles(new FilenameFilter() {
      @Override
      public boolean accept(File dir, String name) {
        return name.endsWith(".hprof");
      }
    });
  }

}
