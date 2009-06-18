/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.msg.KeyValueMappingRequestMessage;

import java.util.SortedSet;
import java.util.TreeSet;

public final class RequestEntryForKeyContext implements ObjectRequestServerContext {

  private final SortedSet lookupIDs = new TreeSet();
  private final ObjectID  mapID;
  private final Object    portableKey;
  private final ClientID  clientID;

  public RequestEntryForKeyContext(KeyValueMappingRequestMessage mesg) {
    this.mapID = mesg.getMapID();
    this.portableKey = mesg.getPortableKey();
    this.clientID = mesg.getClientID();
    this.lookupIDs.add(this.mapID);
  }

  public String getRequestingThreadName() {
    return "Not Supported";
  }

  public boolean isServerInitiated() {
    return false;
  }

  public ClientID getClientID() {
    return this.clientID;
  }

  public int getRequestDepth() {
    // Fault only mapID object
    return -1;
  }

  public ObjectRequestID getRequestID() {
    return ObjectRequestID.NULL_ID;
  }

  public SortedSet<ObjectID> getRequestedObjectIDs() {
    return this.lookupIDs;
  }

  public boolean isPrefetched() {
    // TODO:: for now not implemented
    return false;
  }

  public Object getPortableKey() {
    return this.portableKey;
  }

  public ObjectID getPartialKeyMapID() {
    return this.mapID;
  }
}