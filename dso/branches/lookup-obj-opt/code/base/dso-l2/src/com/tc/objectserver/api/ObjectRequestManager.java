/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.api;

import com.tc.net.groups.ClientID;
import com.tc.object.ObjectRequestID;

import java.util.Collection;
import java.util.Set;

public interface ObjectRequestManager {

  public void requestObjects(ClientID clientID, ObjectRequestID requestID, Set ids, int maxRequestDepth,
                             boolean serverInitiated, String requestingThreadName);

  public void sendObjects(ClientID requestedNodeID, Collection objs, Set requestedObjectIDs, Set missingObjectIDs,
                          boolean isServerInitiated);
  
  public void createAndAddManagedObjectRequestContextsTo(ClientID clientID, ObjectRequestID requestID, Set ids,
                                                         int maxRequestDepth, boolean serverInitiated,
                                                         String requestingThreadName);

}
