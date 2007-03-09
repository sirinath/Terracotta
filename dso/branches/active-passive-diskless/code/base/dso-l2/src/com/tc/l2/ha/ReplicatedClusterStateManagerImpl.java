/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.ha;

import com.tc.l2.api.ReplicatedClusterStateManager;
import com.tc.net.groups.GroupManager;

public class ReplicatedClusterStateManagerImpl implements ReplicatedClusterStateManager {

  private final GroupManager groupManager;
  private ClusterState state;

  public ReplicatedClusterStateManagerImpl(GroupManager groupManager) {
    this.groupManager = groupManager;
  }
  
  private static final class ClusterState {
    
  }
}
