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
import com.tc.objectserver.api.ObjectManagerLookupResults;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.util.ObjectIDSet;
import com.tc.util.TCCollections;

import java.util.Collection;
import java.util.Map;

public final class RequestEntryForKeyContext implements ObjectManagerResultsContext {

  private final static TCLogger    logger    = TCLogging.getLogger(RequestEntryForKeyContext.class);

  private final ObjectIDSet        lookupIDs = new ObjectIDSet();
  private final ServerMapRequestID requestID;
  private final ObjectID           mapID;
  private final Object             portableKey;
  private final ClientID           clientID;
  private final Sink               destinationSink;

  public RequestEntryForKeyContext(final ServerMapRequestID requestID, final ClientID clientID, final ObjectID mapID,
                                   final Object portableKey, final Sink destinationSink) {
    this.requestID = requestID;
    this.clientID = clientID;
    this.mapID = mapID;
    this.portableKey = portableKey;
    this.destinationSink = destinationSink;
    this.lookupIDs.add(mapID);
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public ClientID getClientID() {
    return this.clientID;
  }

  public Object getPortableKey() {
    return this.portableKey;
  }

  public ObjectID getServerTCMapID() {
    return this.mapID;
  }

  @Override
  public String toString() {
    return "RequestEntryForKeyContext [  mapID = " + this.mapID + " key : " + this.portableKey + " clientID : "
           + this.clientID + "]";
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
    final ObjectIDSet pendingLookups = results.getLookupPendingObjectIDs();

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

    final EntryForKeyResponseContext responseContext = new EntryForKeyResponseContext(mo, pendingLookups, this.mapID);
    this.destinationSink.add(responseContext);
  }

  public boolean updateStats() {
    return true;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof RequestEntryForKeyContext) {
      RequestEntryForKeyContext compareTo = (RequestEntryForKeyContext) obj;
      return clientID.equals(compareTo.getClientID()) && mapID.equals(compareTo.getServerTCMapID())
             && portableKey.equals(compareTo.getPortableKey());
    }
    return false;
  }
}