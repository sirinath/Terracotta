/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.async.impl;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.async.api.SpecializedEventContext;
import com.tc.net.NodeID;

public class InBandMoveToNextSink implements SpecializedEventContext {

  private final EventContext event;
  private final Sink         sink;
  private final NodeID       nodeID;

  public InBandMoveToNextSink(EventContext event, Sink sink, NodeID nodeID) {
    this.event = event;
    this.sink = sink;
    this.nodeID = nodeID;
  }

  @Override
  public void execute() {
    sink.add(event);
  }

  @Override
  public Object getKey() {
    return nodeID;
  }

}
