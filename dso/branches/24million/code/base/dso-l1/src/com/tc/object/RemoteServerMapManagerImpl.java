/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerTCMapRequestMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class RemoteServerMapManagerImpl implements RemoteServerMapManager {

  private final GroupID                 groupID;
  private final ServerMapMessageFactory smmFactory;
  private final TCLogger                logger;
  private final SessionManager          sessionManager;
  private final Map                     valueMappingRequests = new HashMap();

  private final static boolean          ENABLE_LOGGING       = TCPropertiesImpl
                                                                 .getProperties()
                                                                 .getBoolean(
                                                                             TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED);

  private State                         state                = State.RUNNING;

  private static enum State {
    PAUSED, RUNNING, STARTING, STOPPED
  }

  public RemoteServerMapManagerImpl(final GroupID groupID, final TCLogger logger,
                                    final ServerMapMessageFactory smmFactory,
                                    final SessionManager sessionManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.smmFactory = smmFactory;
    this.sessionManager = sessionManager;
  }

  public ObjectID getMappingForKey(ObjectID oid, Object portableKey) {
    boolean isInterrupted = false;
    if (oid.getGroupID() != this.groupID.toInt()) {
      //
      throw new AssertionError("Looking up in the wrong Remote Manager : " + this.groupID + " id : " + oid
                               + " portableKey : " + portableKey);
    }

    final ServerTCMapRequestContext context = getOrCreateRequestValueMappingContext(oid, portableKey);
    context.incrementLookupCount();
    ObjectID valueID;
    while (true) {
      waitUntilRunning();
      context.sendRequestIfNecessary(this.smmFactory);
      valueID = context.getValueMappingObjectID();
      if (valueID != null) {
        context.decrementLookupCount();
        cleanupRequestValueMappingContextIfNecessary(context);
        break;
      }
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return valueID;
  }

  public long size(ObjectID oid) {

    return 0;
  }

  synchronized void requestOutstanding() {

    for (final Iterator i = this.valueMappingRequests.entrySet().iterator(); i.hasNext();) {
      final ServerTCMapRequestContext context = (ServerTCMapRequestContext) i.next();
      context.sendRequest(this.smmFactory);
    }
  }

  private void cleanupRequestValueMappingContextIfNecessary(final ServerTCMapRequestContext context) {
    if (context.lookupComplete()) {
      this.valueMappingRequests.remove(context.getCompositeKey());
    }
  }

  private ServerTCMapRequestContext getOrCreateRequestValueMappingContext(final ObjectID oid, final Object portableKey) {
    final String comboKey = oid.toString() + "::" + portableKey.toString();
    ServerTCMapRequestContext context = (ServerTCMapRequestContext) this.valueMappingRequests.get(comboKey);
    if (context == null) {
      context = new ServerTCMapRequestContext(oid, portableKey, this.groupID, comboKey);
      this.valueMappingRequests.put(comboKey, context);
    }
    return context;
  }

  public synchronized void addResponseForKeyValueMapping(final SessionID sessionID, final ObjectID mapID,
                                                         final Object portableKey, final Object portableValue,
                                                         final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring addResponseForKeyValueMapping " + mapID + " , " + portableKey + " , " + portableValue
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    final ServerTCMapRequestContext context = getRequestValueMappingContext(mapID, portableKey);
    if (context != null) {
      context.setValueForKey(mapID, portableKey, portableValue);
    } else {
      this.logger.warn("Key Value Mapping Context is null for " + mapID + " key : " + portableKey + " value : "
                       + portableValue);
    }
    notifyAll();
  }

  private ServerTCMapRequestContext getRequestValueMappingContext(final ObjectID oid, final Object portableKey) {
    final String comboKey = oid.toString() + "::" + portableKey.toString();
    return (ServerTCMapRequestContext) this.valueMappingRequests.get(comboKey);
  }

  private void waitUntilRunning() {
    boolean isInterrupted = false;
    while (this.state != State.RUNNING) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
  }

  public synchronized void pause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotPaused("Attempt to pause while PAUSED");
    this.state = State.PAUSED;
    notifyAll();
  }

  public void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
                                  final ClientHandshakeMessage handshakeMessage) {
    if (isStopped()) { return; }
    assertPaused("Attempt to init handshake while not PAUSED");
    this.state = State.STARTING;
  }

  public synchronized void unpause(final NodeID remote, final int disconnected) {
    if (isStopped()) { return; }
    assertNotRunning("Attempt to unpause while not PAUSED");
    this.state = State.RUNNING;
    requestOutstanding();
    notifyAll();
  }

  public synchronized void shutdown() {
    this.state = State.STOPPED;
  }

  private boolean isStopped() {
    return this.state == State.STOPPED;
  }

  private void assertPaused(final String message) {
    if (this.state != State.PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotPaused(final String message) {
    if (this.state == State.PAUSED) { throw new AssertionError(message + ": " + this.state); }
  }

  private void assertNotRunning(final String message) {
    if (this.state == State.RUNNING) { throw new AssertionError(message + ": " + this.state); }
  }

  // TODO::FIXME::This will go when we do batching
  private static class ServerTCMapRequestContext {

    private final static TCLogger logger      = TCLogging.getLogger(ServerTCMapRequestContext.class);

    private final ObjectID        oid;
    private final Object          portableKey;
    private final GroupID         groupID;
    private final String          comboKey;

    private boolean               requestSent = false;
    private int                   count;
    private ObjectID              valueID;

    public ServerTCMapRequestContext(final ObjectID oid, final Object portableKey, final GroupID groupID,
                                     final String comboKey) {
      this.oid = oid;
      this.portableKey = portableKey;
      this.groupID = groupID;
      this.comboKey = comboKey;
    }

    public void setValueForKey(final ObjectID mapID, final Object pKey, final Object pValue) {

      if (ENABLE_LOGGING) {
        logger.info("Received response for Map : " + this.oid + " key : " + this.portableKey + " value : " + pValue);
      }
      if (pValue instanceof ObjectID) {
        this.valueID = (ObjectID) pValue;
      } else {
        throw new AssertionError("Unsupported now");
      }
    }

    public Object getCompositeKey() {
      return this.comboKey;
    }

    public boolean lookupComplete() {
      return (this.count == 0);
    }

    public void decrementLookupCount() {
      this.count--;
    }

    public ObjectID getValueMappingObjectID() {
      return this.valueID;
    }

    public void incrementLookupCount() {
      this.count++;
    }

    // TODO::Change / Batch
    public void sendRequestIfNecessary(final ServerMapMessageFactory factory) {
      if (!this.requestSent) {
        this.requestSent = true;
        if (ENABLE_LOGGING) {
          logger.info("Sending request for Map : " + this.oid + " key : " + this.portableKey);
        }
        sendRequest(factory);
      }
    }

    // TODO::Change / Batch
    public void sendRequest(final ServerMapMessageFactory factory) {
      final ServerTCMapRequestMessage mappingMessage = factory.newServerTCMapRequestMessage(this.groupID);
      mappingMessage.initialize(this.oid, this.portableKey);
      mappingMessage.send();
    }
  }

}
