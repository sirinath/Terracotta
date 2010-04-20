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
import java.util.Map;

public class RemoteServerMapManagerImpl implements RemoteServerMapManager {

  private final GroupID                                            groupID;
  private final ServerMapMessageFactory                            smmFactory;
  private final TCLogger                                           logger;
  private final SessionManager                                     sessionManager;
  private final Map<ServerMapRequestID, ServerTCMapRequestContext> outstandingRequests = new HashMap();

  private final static boolean                                     ENABLE_LOGGING      = TCPropertiesImpl
                                                                                           .getProperties()
                                                                                           .getBoolean(
                                                                                                       TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_LOGGING_ENABLED);

  private State                                                    state               = State.RUNNING;

  private long                                                     requestIDCounter    = 0;

  private static enum State {
    PAUSED, RUNNING, STARTING, STOPPED
  }

  public RemoteServerMapManagerImpl(final GroupID groupID, final TCLogger logger,
                                    final ServerMapMessageFactory smmFactory, final SessionManager sessionManager) {
    this.groupID = groupID;
    this.logger = logger;
    this.smmFactory = smmFactory;
    this.sessionManager = sessionManager;
  }

  public synchronized Object getMappingForKey(final ObjectID oid, final Object portableKey) {
    boolean isInterrupted = false;
    if (oid.getGroupID() != this.groupID.toInt()) {
      //
      throw new AssertionError("Looking up in the wrong Remote Manager : " + this.groupID + " id : " + oid
                               + " portableKey : " + portableKey);
    }

    waitUntilRunning();

    final ServerTCMapRequestContext context = createRequestContext(oid, portableKey);
    Object value;
    context.sendRequest(this.smmFactory);
    while (true) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
      value = context.getValueMappingObjectID();
      if (value != null) {
        removeRequestContext(context);
        break;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return value;
  }

  // TODO::
  public int getSize(final ObjectID mapId) {
    return 0;
  }

  synchronized void requestOutstanding() {
    for (final Object element : this.outstandingRequests.entrySet()) {
      final ServerTCMapRequestContext context = (ServerTCMapRequestContext) element;
      context.sendRequest(this.smmFactory);
    }
  }

  private void removeRequestContext(final ServerTCMapRequestContext context) {
    final Object old = this.outstandingRequests.remove(context.getRequestID());
    if (old != context) { throw new AssertionError("Removed wrong context. context = " + context + " old = " + old); }
  }

  private ServerTCMapRequestContext createRequestContext(final ObjectID oid, final Object portableKey) {
    final ServerMapRequestID requestID = getNextRequestID();
    final ServerTCMapRequestContext context = new ServerTCMapRequestContext(requestID, oid, portableKey, this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  private ServerTCMapRequestContext getRequestContext(final ServerMapRequestID requestID) {
    return this.outstandingRequests.get(requestID);
  }

  public synchronized void addResponseForKeyValueMapping(final SessionID sessionID, final ObjectID mapID,
                                                         final ServerMapRequestID requestID,
                                                         final Object portableValue, final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring addResponseForKeyValueMapping " + mapID + " , " + requestID + " , " + portableValue
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    final ServerTCMapRequestContext context = getRequestContext(requestID);
    if (context != null) {
      context.setValueForKey(mapID, portableValue);
    } else {
      this.logger.warn("Key Value Mapping Context is null for " + mapID + " key : " + requestID + " value : "
                       + portableValue);
    }
    notifyAll();
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

  public synchronized void initializeHandshake(final NodeID thisNode, final NodeID remoteNode,
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

  private ServerMapRequestID getNextRequestID() {
    return new ServerMapRequestID(this.requestIDCounter++);
  }

  // TODO::FIXME::This will go when we do batching
  private static class ServerTCMapRequestContext {

    private final static TCLogger    logger = TCLogging.getLogger(ServerTCMapRequestContext.class);

    private final ObjectID           oid;
    private final Object             portableKey;
    private final GroupID            groupID;
    private final ServerMapRequestID requestID;

    private Object                   value;

    public ServerTCMapRequestContext(final ServerMapRequestID requestID, final ObjectID oid, final Object portableKey,
                                     final GroupID groupID) {
      this.requestID = requestID;
      this.oid = oid;
      this.portableKey = portableKey;
      this.groupID = groupID;
    }

    public void setValueForKey(final ObjectID mapID, final Object pValue) {

      if (ENABLE_LOGGING) {
        logger.info("Received response for Map : " + this.oid + " key : " + this.portableKey + " value : " + pValue);
      }
      this.value = pValue;
    }

    public Object getValueMappingObjectID() {
      return this.value;
    }

    public ServerMapRequestID getRequestID() {
      return this.requestID;
    }

    // TODO::Change / Batch
    public void sendRequest(final ServerMapMessageFactory factory) {
      if (ENABLE_LOGGING) {
        logger.info("Sending request for Map : " + this.oid + " key : " + this.portableKey);
      }
      final ServerTCMapRequestMessage mappingMessage = factory.newServerTCMapRequestMessage(this.groupID);
      mappingMessage.initialize(this.requestID, this.oid, this.portableKey);
      mappingMessage.send();
    }
  }

}
