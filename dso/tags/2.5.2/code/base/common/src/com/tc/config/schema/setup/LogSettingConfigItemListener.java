/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.config.schema.setup;

import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.config.schema.dynamic.ConfigItemListener;
import com.tc.logging.TCLogging;

import java.io.File;

/**
 * Tells {@link TCLogging} to set its log file to the location specified. This must be attached to a {@link ConfigItem}
 * that returns {@link File} objects.
 */
public class LogSettingConfigItemListener implements ConfigItemListener {

  private final int processType;

  public LogSettingConfigItemListener(int processType) {
    this.processType = processType;
  }

  public void valueChanged(Object oldValue, Object newValue) {
    if (newValue != null) {
      TCLogging.setLogDirectory((File) newValue, processType);
    }
  }

}
