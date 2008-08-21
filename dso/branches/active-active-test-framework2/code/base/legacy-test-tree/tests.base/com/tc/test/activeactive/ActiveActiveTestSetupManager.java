/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activeactive;

import com.tc.test.activepassive.ActivePassiveTestSetupManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ActiveActiveTestSetupManager {

  private ActivePassiveTestSetupManager apTestSetupMgr     = new ActivePassiveTestSetupManager();
  private int                           electionTime       = 5;
  private List                          activeServerGroups = new ArrayList();

  public void setServerCount(int count) {
    this.apTestSetupMgr.setServerCount(count);
  }

  public int getServerCount() {
    return this.apTestSetupMgr.getServerCount();
  }

  public void setServerCrashMode(String mode) {
    this.apTestSetupMgr.setServerCrashMode(mode);
  }

  public void setMaxCrashCount(int count) {
    this.apTestSetupMgr.setMaxCrashCount(count);
  }

  public int getMaxCrashCount() {
    return this.getMaxCrashCount();
  }

  public String getServerCrashMode() {
    return this.apTestSetupMgr.getServerCrashMode();
  }

  public void setServerShareDataMode(String mode) {
    this.apTestSetupMgr.setServerShareDataMode(mode);
  }

  public boolean isNetworkShare() {
    return this.apTestSetupMgr.isNetworkShare();
  }

  public void setServerPersistenceMode(String mode) {
    this.apTestSetupMgr.setServerPersistenceMode(mode);
  }

  public String getServerPersistenceMode() {
    return this.apTestSetupMgr.getServerPersistenceMode();
  }

  public void setServerCrashWaitTimeInSec(long time) {
    this.apTestSetupMgr.setServerCrashWaitTimeInSec(time);
  }

  public long getServerCrashWaitTimeInSec() {
    return this.getServerCrashWaitTimeInSec();
  }

  public void setElectionTime(int time) {
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
    int serverCount = this.apTestSetupMgr.getServerCount();
    if (this.activeServerGroups.size() == 0) {
      addActiveServerGroup(serverCount, this.apTestSetupMgr.getServerSharedDataMode(), this.electionTime);
    }

    int totalMemberCount = 0;
    for (Iterator iter = this.activeServerGroups.iterator(); iter.hasNext();) {
      Group grp = (Group) iter.next();
      totalMemberCount += grp.getMemberCount();
    }
    if (totalMemberCount != serverCount) { throw new AssertionError(
                                                                    "Number of servers indicated does not match the number of active-server-group members:  totalMemberCount=["
                                                                        + totalMemberCount + "] serverCount=["
                                                                        + serverCount + "]."); }
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
