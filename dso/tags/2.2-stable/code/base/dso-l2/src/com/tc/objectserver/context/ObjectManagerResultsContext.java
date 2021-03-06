/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.objectserver.api.ObjectManagerLookupResults;

import java.util.Collection;
import java.util.Set;

/**
 * @author steve Interface for a context that needs ObjectManager look results
 */
public interface ObjectManagerResultsContext extends EventContext {

  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results);
  
  public void makePending(ChannelID channelID, Collection ids);
  
  public Set getCheckedOutObjectIDs();
  
  // supposed to return true if makePending is already called.
  public boolean isPendingRequest();
  
}