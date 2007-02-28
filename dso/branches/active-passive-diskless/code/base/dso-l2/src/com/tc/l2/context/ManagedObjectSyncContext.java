/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.context;

import com.tc.async.api.Sink;
import com.tc.bytes.TCByteBuffer;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.context.ObjectManagerResultsContext;
import com.tc.util.Assert;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class ManagedObjectSyncContext implements ObjectManagerResultsContext {

  private final NodeID               nodeID;
  private final Set                  oids;
  private final boolean              more;
  private final Sink                 nextSink;

  private boolean                    isPending = false;
  private ObjectManagerLookupResults result;
  private TCByteBuffer[]             dnas;
  private int                        dnaCount;
  private ObjectStringSerializer     serializer;

  public ManagedObjectSyncContext(NodeID nodeID, Set oids, boolean more, Sink sink) {
    this.nodeID = nodeID;
    this.oids = oids;
    this.more = more;
    this.nextSink = sink;
  }

  public boolean isPendingRequest() {
    return this.isPending;
  }

  // TODO:: Remove ChannelID from this interface
  public void makePending(ChannelID channelID, Collection ids) {
  }

  // TODO:: Remove ChannelID from this interface
  public void setResults(ChannelID chID, Collection ids, ObjectManagerLookupResults results) {
    this.result = results;
    nextSink.add(this);
  }

  public Set getOIDs() {
    return oids;
  }

  public Map getObjects() {
    Assert.assertNotNull(result);
    return result.getObjects();
  }

  public void setDehydratedBytes(TCByteBuffer[] buffers, int count, ObjectStringSerializer os) {
    this.dnas = buffers;
    this.dnaCount = count;
    this.serializer = os;
  }

  public NodeID getNodeID() {
    return nodeID;
  }

  public void close() {
    //TODO::change state
  }

}
