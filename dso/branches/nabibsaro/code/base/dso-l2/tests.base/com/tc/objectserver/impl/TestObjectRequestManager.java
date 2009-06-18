/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.concurrent.LinkedBlockingQueue;

public class TestObjectRequestManager implements ObjectRequestManager {

  public LinkedBlockingQueue<ObjectRequestServerContext> requestedObjects = new LinkedBlockingQueue<ObjectRequestServerContext>();

  public void requestObjects(final ObjectRequestServerContext requestContext, final Sink destination) {
    try {
      this.requestedObjects.put(requestContext);
    } catch (InterruptedException e) {
      throw new AssertionError(e);
    }

  }

  public void sendObjects(final ClientID requestedNodeID, final Collection objs, final ObjectIDSet requestedObjectIDs,
                          final ObjectIDSet missingObjectIDs, final boolean isServerInitiated,
                          final boolean isPrefetched, final int maxRequestDepth) {
    // not implemented
  }

}
