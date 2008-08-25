/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActivePassiveTestSetupManager {

  private int                          serverCount              = -1;
  private long                         serverCrashWaitTimeInSec = 15;
  private int                          maxCrashCount            = Integer.MAX_VALUE;
  private ActivePassiveSharedDataMode  activePassiveMode;
  private ActivePassivePersistenceMode persistenceMode          = new ActivePassivePersistenceMode(
                                                                                                   ActivePassivePersistenceMode.TEMPORARY_SWAP_ONLY);
  private int                          electionTime             = 5;
  private ActivePassiveCrashMode       crashMode;
  private List                         activeServerGroups       = new ArrayList();

  public void setServerCount(int count) {
    if (count < 2) { throw new AssertionError("Server count must be 2 or more:  count=[" + count + "]"); }
    serverCount = count;
  }

  public int getServerCount() {
    if (this.serverCount == -1) { throw new AssertionError("Server count has not been set."); }
    return serverCount;
  }

  public void setServerCrashMode(String mode) {
    crashMode = new ActivePassiveCrashMode(mode);
  }

  public void setMaxCrashCount(int count) {
    if (count < 0) { throw new AssertionError("Max crash count should not be a neg number"); }
    maxCrashCount = count;
  }

  public int getMaxCrashCount() {
    return maxCrashCount;
  }

  public String getServerCrashMode() {
    if (crashMode == null) { throw new AssertionError("Server crash mode was not set."); }
    return crashMode.getMode();
  }

  public void setServerShareDataMode(String mode) {
    activePassiveMode = new ActivePassiveSharedDataMode(mode);
  }

  public boolean isNetworkShare() {
    if (activePassiveMode == null) { throw new AssertionError("Server share mode was not set."); }
    return activePassiveMode.isNetworkShare();
  }

  public void setServerPersistenceMode(String mode) {
    persistenceMode = new ActivePassivePersistenceMode(mode);
  }

  public String getServerPersistenceMode() {
    if (persistenceMode == null) { throw new AssertionError("Server persistence mode was not set."); }
    return persistenceMode.getMode();
  }

  public void setServerCrashWaitTimeInSec(long time) {
    if (time < 0) { throw new AssertionError("Wait time should not be a negative number."); }
    serverCrashWaitTimeInSec = time;
  }

  public long getServerCrashWaitTimeInSec() {
    return serverCrashWaitTimeInSec;
  }

  public void setElectionTime(int time) {
    if (time < 0) { throw new AssertionError("Election time should not be negative."); }
    this.electionTime = time;
  }

  public int getElectionTime() {
    return this.electionTime;
  }

  public void addActiveServerGroup(int membersCount, String local_activePassiveMode, int local_electionTime) {
    this.activeServerGroups.add(new Group(membersCount, local_activePassiveMode, local_electionTime));
  }

  public int getActiveServerGroupCount() {
    checkServerCount();
    return this.activeServerGroups.size();
  }

  public int getGroupMemberCount(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getMemberCount();
  }

  public int getGroupElectionTime(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getElectionTime();
  }

  public String getGroupServerShareDataMode(int groupIndex) {
    checkServerCount();
    return ((Group) this.activeServerGroups.get(groupIndex)).getMode();
  }

  public boolean isActiveActive() {
    return getActiveServerGroupCount() > 1;
  }

  public boolean isActivePassive() {
    return !isActiveActive();
  }

  private void checkServerCount() {
    if (this.activeServerGroups.size() == 0) {
      addActiveServerGroup(this.serverCount, this.activePassiveMode.getMode(), this.electionTime);
    }

    int totalMemberCount = 0;
    for (Iterator iter = this.activeServerGroups.iterator(); iter.hasNext();) {
      Group grp = (Group) iter.next();
      totalMemberCount += grp.getMemberCount();
    }
    if (totalMemberCount != serverCount) { throw new AssertionError(
                                                                    "Number of servers indicated does not match the number of active-server-group members:  totalMemberCount=["
                                                                        + totalMemberCount + "] serverCount=["
                                                                        + this.serverCount + "]."); }
  }

  private static class Group {
    private final int    memberCount;
    private final String groupPersistenceMode;
    private final int    groupElectionTime;

    public Group(int memberCount, String persistenceMode, int electionTime) {
      this.memberCount = memberCount;
      groupPersistenceMode = persistenceMode;
      groupElectionTime = electionTime;
    }

    public int getMemberCount() {
      return this.memberCount;
    }

    public String getMode() {
      return this.groupPersistenceMode;
    }

    public int getElectionTime() {
      return this.groupElectionTime;
    }
  }

}
