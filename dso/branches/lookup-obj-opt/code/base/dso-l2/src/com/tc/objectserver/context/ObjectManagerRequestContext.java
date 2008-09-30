/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.ClientID;
import com.tc.object.ObjectRequestID;

public interface ObjectManagerRequestContext extends ObjectManagerResultsContext {
  
  public ObjectRequestID getRequestID();
  
  public int getMaxRequestDepth();
 
  public boolean isServerInitiated();

  public ClientID getRequestedNodeID();

  public String getRequestingThreadName();

}
