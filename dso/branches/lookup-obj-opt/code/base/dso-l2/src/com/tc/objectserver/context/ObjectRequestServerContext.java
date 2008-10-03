/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;

import java.util.Set;

public interface ObjectRequestServerContext {
  
  public ObjectRequestID getRequestID();
  
  public int getMaxRequestDepth();
 
  public boolean isServerInitiated();
  
  public Set<ObjectID> getLookupIDs();

  public ClientID getRequestedNodeID();

  public String getRequestingThreadName();
  
}
