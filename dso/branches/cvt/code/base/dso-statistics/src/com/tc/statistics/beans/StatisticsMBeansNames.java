/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.statistics.beans;

import com.tc.management.TerracottaManagement;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class StatisticsMBeansNames {
  public static final ObjectName STATISTICS_EMITTER;
  public static final ObjectName STATISTICS_MANAGER;

  static {
    try {
      STATISTICS_EMITTER = TerracottaManagement.createObjectName(TerracottaManagement.Type.Agent, TerracottaManagement.Subsystem.Statistics, null, "Terracotta Statistics Emitter", true);
      STATISTICS_MANAGER = TerracottaManagement.createObjectName(TerracottaManagement.Type.Agent, TerracottaManagement.Subsystem.Statistics, null, "Terracotta Statistics Manager", true);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }
}