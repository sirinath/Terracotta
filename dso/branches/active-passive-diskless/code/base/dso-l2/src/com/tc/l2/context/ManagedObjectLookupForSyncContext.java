/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.l2.context;

import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ObjectManagerResultsContext;

import java.util.Collection;
import java.util.Set;

public class ManagedObjectLookupForSyncContext implements ObjectManagerResultsContext {

  private final NodeID nodeID;
  private final Set oids;
  private final boolean more;
  
  private boolean isPending = false;

  public ManagedObjectLookupForSyncContext(NodeID nodeID, Set oids, boolean more) {
    this.nodeID = nodeID;
    this.oids = oids;
    this.more = more;
  }

  public boolean isPendingRequest() {
    return this.isPending;
  }

  //TODO:: Remove ChannelID from this interface
  public void makePending(ChannelID channelID, Collection ids) {
  }

  //TODO:: Remove ChannelID from this interface
  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
  }

}
