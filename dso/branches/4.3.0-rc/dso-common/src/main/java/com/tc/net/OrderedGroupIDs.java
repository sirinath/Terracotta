/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net;

import java.util.Arrays;
import java.util.Collection;

/**
 * This class's purpose is to give a definite order to a set of GroupIDs and determine the coordinator GroupID, that way
 * you don't have to encode the GroupIDs mapping in many places.
 */
public class OrderedGroupIDs {

  private final GroupID[] groupIDs;

  public OrderedGroupIDs(GroupID[] gids) {
    this.groupIDs = gids;
    Arrays.sort(groupIDs);
  }

  public Collection<GroupID> getGroupIDSet() {
    return Arrays.asList(groupIDs);
  }

  public GroupID[] getGroupIDs() {
    return this.groupIDs;
  }

  public int length() {
    return this.groupIDs.length;
  }

  public GroupID getGroup(int i) {
    return this.groupIDs[i];
  }

  public int getGroupIDIndex(GroupID gid) {
    return Arrays.binarySearch(this.groupIDs, gid);
  }

  public GroupID getActiveCoordinatorGroup() {
    // This assumption that index 0 is coordinator group should not be exposed anywhere else.
    return this.groupIDs[0];
  }

}
