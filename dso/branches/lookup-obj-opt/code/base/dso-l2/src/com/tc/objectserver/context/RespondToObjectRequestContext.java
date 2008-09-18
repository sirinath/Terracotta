/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.EventContext;
import com.tc.net.groups.ClientID;

import java.util.Collection;
import java.util.Set;

public interface RespondToObjectRequestContext extends EventContext {

  public ClientID getRequestedNodeID();

  public Collection getObjs();

  public Set getRequestedObjectIDs();

  public Set getMissingObjectIDs();

  public boolean isServerInitiated();

}
