/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.TerracottaManagement;
import com.tc.management.TerracottaManagement.Subsystem;
import com.tc.management.TerracottaManagement.Type;

import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;

public class L2MBeanNames {

  public static final ObjectName TC_SERVER_INFO;
  public static final ObjectName LOGGER;
  public static final ObjectName DSO;
  
  //XXX: this for test, to read server data when active-active is running.
  public static final ObjectName LOCAL_DGC_STATS;
  public static final ObjectName DSO_APP_EVENTS;
  public static final ObjectName OBJECT_MANAGEMENT;
  public static final ObjectName DUMPER;
  public static final ObjectName LOCK_STATISTICS;
  public static final ObjectName SERVER_DB_BACKUP;

  static {
    try {
      TC_SERVER_INFO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Server", false);
      LOGGER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Logger", false);
      DSO = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "DSO", true);
      LOCAL_DGC_STATS = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "DSO Server", true);
      DSO_APP_EVENTS = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Application Events", false);
      OBJECT_MANAGEMENT = TerracottaManagement.createObjectName(Type.Server, Subsystem.ObjectManagement, null, "ObjectManagement", true);
      DUMPER = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "L2Dumper", false);
      LOCK_STATISTICS = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Lock Statistics", false);
      SERVER_DB_BACKUP = TerracottaManagement.createObjectName(Type.Server, Subsystem.None, null, "Terracotta Server Backup", false);
    } catch (MalformedObjectNameException mone) {
      throw new RuntimeException(mone);
    } catch (NullPointerException npe) {
      throw new RuntimeException(npe);
    }
  }
}