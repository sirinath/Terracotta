/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

import com.tc.exception.TCRuntimeException;
import com.tc.test.TempDirectoryHelper;
import com.tctest.spring.integrationtests.framework.PropertiesHackForRunningInEclipse;

import java.io.File;
import java.io.IOException;

public class TempDirectoryUtil {

  private static TempDirectoryHelper tempDirectoryHelper;

  public static File getTempDirectory(Class type) throws IOException {
    return getTempDirectoryHelper(type).getDirectory();
  }
  
  protected static synchronized TempDirectoryHelper getTempDirectoryHelper(Class type) {
    if (tempDirectoryHelper == null) {
      try {
        PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();
        tempDirectoryHelper = new TempDirectoryHelper(type, true);
      } catch (IOException ioe) {
        throw new TCRuntimeException("Can't get configuration for temp directory", ioe);
      }
    }

    return tempDirectoryHelper;
  }

}
