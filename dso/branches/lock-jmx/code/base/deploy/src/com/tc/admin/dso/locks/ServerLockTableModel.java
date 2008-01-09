/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.dso.locks;

import com.tc.admin.common.XObjectTableModel;
import com.tc.management.beans.LockStatisticsMonitorMBean;
import com.tc.management.lock.stats.LockSpec;

import java.util.Collection;
import java.util.Iterator;

public class ServerLockTableModel extends XObjectTableModel {
  LockStatisticsMonitorMBean               fLockStats;

  static protected String[]                cNames      = { "Lock", "<html>Times<br>Requested</html>",
      "<html>Times<br>Hopped</html>", "<html>Average<br>Contenders</html>", "<html>Average<br>Acquire Time</html>",
      "<html>Average<br>Held Time</html>"             };
  static protected String[]                cFields     = { "Name", "Requested", "Hops", "Waiters", "AcquireTime",
      "HeldTime"                                      };
  public static final ServerLockTableModel EMPTY_MODEL = new ServerLockTableModel();

  public ServerLockTableModel() {
    super(LockSpecWrapper.class, cFields, cNames);
  }

  public ServerLockTableModel(LockStatisticsMonitorMBean lockStats) {
    this();
    fLockStats = lockStats;
    init();
  }

  public void init() {
    clear();
    Collection<LockSpec> lockInfos = fLockStats.getLockSpecs();
    Iterator<LockSpec> iter = lockInfos.iterator();
    while (iter.hasNext()) {
      add(new LockSpecWrapper(iter.next()));
    }
  }
  
  public static class LockSpecWrapper {
    private LockSpec fLockSpec;
    private String   fName;

    LockSpecWrapper(LockSpec lockSpec) {
      fLockSpec = lockSpec;

      fName = fLockSpec.getLockID().asString();
      String objectType = fLockSpec.getObjectType();
      if (objectType != null && objectType.length() > 0) {
        fName += " (" + objectType + ")";
      }
    }

    public String getName() {
      return fName;
    }

    public long getRequested() {
      return fLockSpec.getServerStats().getNumOfLockRequested();
    }

    public long getHops() {
      return fLockSpec.getServerStats().getNumOfLockHopRequests();
    }

    public long getWaiters() {
      return fLockSpec.getServerStats().getNumOfPendingWaiters();
    }

    public long getAcquireTime() {
      return fLockSpec.getServerStats().getAvgWaitTimeToAwardInMillis();
    }

    public long getHeldTime() {
      return fLockSpec.getServerStats().getAvgHeldTimeInMillis();
    }
  }
}
