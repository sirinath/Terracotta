/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net;

import java.util.Arrays;
import java.util.TreeSet;

/**
 * This class's sole purpose is to give a definite order to a set of GroupIDs, that way you don't have to encode the
 * GroupIDs mapping in many places.
 */
public class OrderedGroupIDs {

  private final GroupID[] groupIDs;

  public OrderedGroupIDs(GroupID[] gids) {
    TreeSet<GroupID> sorted = new TreeSet<GroupID>(Arrays.asList(gids));
    groupIDs = sorted.toArray(new GroupID[sorted.size()]);

  }

  public GroupID[] getGroupIDs() {
    return groupIDs;
  }

  public int length() {
    return groupIDs.length;
  }

  public GroupID getGroup(int i) {
    return groupIDs[i];
  }

}
