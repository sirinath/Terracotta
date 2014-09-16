/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.async.api.MultiThreadedEventContext;
import com.tc.lang.Recyclable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.util.ObjectIDSet;

public interface RequestManagedObjectMessage extends ObjectRequestServerContext, Recyclable, MultiThreadedEventContext {

  public ObjectIDSet getRemoved();

  public void initialize(ObjectRequestID requestID, ObjectIDSet requestedObjectIDs, int requestDepth,
                         ObjectIDSet removeObjects);

  public void send();

  public MessageChannel getChannel();

  public NodeID getSourceNodeID();

}
