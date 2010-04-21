/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.context;

import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.ServerMapRequestType;
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Map;

public final class ServerMapRequestContext implements ObjectManagerResultsContext {

  private final static TCLogger      logger    = TCLogging.getLogger(ServerMapRequestContext.class);

  private final ServerMapRequestType requestType;
  private final ObjectIDSet          lookupIDs = new ObjectIDSet();
  private final ServerMapRequestID   requestID;
  private final ObjectID             mapID;
  private final Object               portableKey;
  private final ClientID             clientID;
  private final Sink                 destinationSink;

  public ServerMapRequestContext(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                                 final Object portableKey, final Sink destinationSink) {
    this(ServerMapRequestType.GET_VALUE_FOR_KEY, requestID, clientID, mapID, destinationSink, portableKey);
  }

  public ServerMapRequestContext(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                                 final Sink destinationSink) {
    this(ServerMapRequestType.GET_SIZE, requestID, clientID, mapID, destinationSink, null);
  }

  private ServerMapRequestContext(final ServerMapRequestType requestType, final ServerMapRequestID requestID,
                                  final ClientID clientID, final ObjectID mapID, final Sink destinationSink,
                                  final Object portableKey) {
    this.requestType = requestType;
    this.requestID = requestID;
    this.clientID = clientID;
    this.mapID = mapID;
    this.destinationSink = destinationSink;
    this.portableKey = portableKey;
    this.lookupIDs.add(mapID);
  }

  public ServerMapRequestType getRequestType() {
    return this.requestType;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public ClientID getClientID() {
    return this.clientID;
  }

  public Object getPortableKey() {
    if (this.portableKey == null) { throw new AssertionError("Key is null : " + this); }
    return this.portableKey;
  }

  public ObjectID getServerTCMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "RequestEntryForKeyContext [  mapID = " + this.mapID + " clientID : " + this.clientID + " requestType : "
           + this.requestType + " requestID : " + this.requestID + " key : " + this.portableKey + "]";
  }

  public ObjectIDSet getLookupIDs() {
    return this.lookupIDs;
  }

  public ObjectIDSet getNewObjectIDs() {
    return TCCollections.EMPTY_OBJECT_ID_SET;
  }

  public void setResults(final ObjectManagerLookupResults results) {
    final Map<ObjectID, ManagedObject> objects = results.getObjects();
    final ObjectIDSet missingObjects = results.getMissingObjectIDs();

    if (!missingObjects.isEmpty()) {
      logger.error("Ignoring Missing ObjectIDs : " + missingObjects + " Map ID : " + this.mapID + " portable key : "
                   + this.portableKey);
      // TODO:: Fix this
      return;
    }
    if (objects.size() != 1) { throw new AssertionError("Asked for 1, got more or less"); }

    final Collection<ManagedObject> mobjs = objects.values();
    final ManagedObject mo = mobjs.iterator().next();

    if (!mo.getID().equals(this.mapID)) { throw new AssertionError("ServerTCMap (mapID " + this.mapID
                                                                   + " ) is not looked up "); }

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(mo, this.mapID);
    this.destinationSink.add(responseContext);
  }

  public boolean updateStats() {
    return true;
  }

  @Override
  public boolean equals(final Object obj) {
    if (obj instanceof ServerMapRequestContext) {
      final ServerMapRequestContext compareTo = (ServerMapRequestContext) obj;
      return this.clientID.equals(compareTo.getClientID()) && this.mapID.equals(compareTo.getServerTCMapID())
             && this.portableKey.equals(compareTo.getPortableKey());
    }
    return false;
  }
}