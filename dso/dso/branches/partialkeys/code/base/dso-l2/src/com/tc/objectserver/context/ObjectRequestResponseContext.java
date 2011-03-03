/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.util.ObjectIDSet;

import java.util.Collection;

public class ObjectRequestResponseContext implements RespondToObjectRequestContext {

  private final ClientID                   requestedNodeID;
  private final Collection                 objs;
  private final ObjectIDSet                requestedObjectIDs;
  private final ObjectIDSet                missingObjectIDs;
  private final boolean                    serverInitiated;
  private final int                        maxRequestDepth;
  private final boolean                    preFetched;
  private final ObjectRequestServerContext objectRequestServerContext;

  public ObjectRequestResponseContext(final ClientID requestedNodeID, final Collection objs,
                                      final ObjectIDSet requestedObjectIDs, final ObjectIDSet missingObjectIDs,
                                      final boolean serverInitiated, final boolean preFetched, final int maxDepth,
                                      final ObjectRequestServerContext objectRequestServerContext) {
    this.requestedNodeID = requestedNodeID;
    this.objs = objs;
    this.requestedObjectIDs = requestedObjectIDs;
    this.missingObjectIDs = missingObjectIDs;
    this.serverInitiated = serverInitiated;
    this.preFetched = preFetched;
    this.maxRequestDepth = maxDepth;
    this.objectRequestServerContext = objectRequestServerContext;
  }

  public ClientID getRequestedNodeID() {
    return this.requestedNodeID;
  }

  public Collection getObjs() {
    return this.objs;
  }

  public ObjectIDSet getRequestedObjectIDs() {
    return this.requestedObjectIDs;
  }

  public ObjectIDSet getMissingObjectIDs() {
    return this.missingObjectIDs;
  }

  public boolean isServerInitiated() {
    return this.serverInitiated;
  }

  public int getRequestDepth() {
    return this.maxRequestDepth;
  }

  public boolean isPreFetched() {
    return this.preFetched;
  }

  public ObjectRequestServerContext getRequestContext() {
    return this.objectRequestServerContext;
  }

  @Override
  public String toString() {
    return "ResponseContext [ requestNodeID = " + this.requestedNodeID + " , objs.size = " + this.objs.size()
           + " , requestedObjectIDs = " + this.requestedObjectIDs + " , missingObjectIDs = " + this.missingObjectIDs
           + " , serverInitiated = " + this.serverInitiated + " , preFetched = " + this.preFetched + " ] ";
  }
}