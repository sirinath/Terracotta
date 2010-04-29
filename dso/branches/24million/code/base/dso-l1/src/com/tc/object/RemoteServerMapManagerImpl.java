/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.logging.TCLogger;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.GetSizeServerMapRequestMessage;
import com.tc.object.msg.GetValueServerMapRequestMessage;
import com.tc.object.msg.ServerMapMessageFactory;
import com.tc.object.msg.ServerMapRequestMessage;
import com.tc.object.session.SessionID;
import com.tc.object.session.SessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Util;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Map.Entry;

public class RemoteServerMapManagerImpl implements RemoteServerMapManager {

  // TODO::Make its own property
  private static final int                                               MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(
                                                                                                                               TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_MAX_REQUEST_SENT_IMMEDIATELY);
  private static final long                                              BATCH_LOOKUP_TIME_PERIOD                  = TCPropertiesImpl
                                                                                                                       .getProperties()
                                                                                                                       .getInt(
                                                                                                                               TCPropertiesConsts.L1_OBJECTMANAGER_REMOTE_BATCH_LOOKUP_TIME_PERIOD);

  private final GroupID                                                  groupID;
  private final ServerMapMessageFactory                                  smmFactory;
  private final TCLogger                                                 logger;
  private final SessionManager                                           sessionManager;
  private final Map<ServerMapRequestID, AbstractServerMapRequestContext> outstandingRequests                       = new HashMap<ServerMapRequestID, AbstractServerMapRequestContext>();

  private final Timer                                                    requestTimer                              = new Timer(
                                                                                                                               "RemoteServerMapManager Request Scheduler",
                                                                                                                               true);

  private State                                                          state                                     = State.RUNNING;

  private long                                                           requestIDCounter                          = 0;

  private boolean                                                        pendingSendTaskScheduled                  = false;

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
    assertSameGroupID(oid);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createRequestContext(oid, portableKey);
    context.makeLookupRequest();
    sendRequest(context);
    Object value;
    while (true) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
      value = context.getResult();
      if (value != null) {
        removeRequestContext(context);
        break;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return value;
  }

  private void assertSameGroupID(final ObjectID oid) {
    if (oid.getGroupID() != this.groupID.toInt()) { throw new AssertionError(
                                                                             "Looking up in the wrong Remote Manager : "
                                                                                 + this.groupID + " id : " + oid); }
  }

  public synchronized int getSize(final ObjectID mapId) {
    boolean isInterrupted = false;
    assertSameGroupID(mapId);
    waitUntilRunning();

    final AbstractServerMapRequestContext context = createSizeRequestContext(mapId);
    context.makeLookupRequest();
    sendRequestNow(context); // Get size requests are not batched for now
    Integer size;
    while (true) {
      try {
        wait();
      } catch (final InterruptedException e) {
        isInterrupted = true;
      }
      size = (Integer) context.getResult();
      if (size != null) {
        removeRequestContext(context);
        break;
      }
    }
    Util.selfInterruptIfNeeded(isInterrupted);
    return size;
  }

  private void sendRequest(final AbstractServerMapRequestContext context) {
    final int size = this.outstandingRequests.size();
    if (size % 5000 == 4999) {
      this.logger.warn("Too many pending requests in the system : objectLookup states size : " + size);
    }
    if (size <= MAX_OUTSTANDING_REQUESTS_SENT_IMMEDIATELY) {
      sendRequestNow(context);
    } else {
      scheduleRequestForLater(context);
    }

  }

  private void scheduleRequestForLater(final AbstractServerMapRequestContext context) {
    context.makePending();
    if (!this.pendingSendTaskScheduled) {
      this.requestTimer.schedule(new SendPendingRequestsTimer(), BATCH_LOOKUP_TIME_PERIOD);
      this.pendingSendTaskScheduled = true;
    }
  }

  private class SendPendingRequestsTimer extends TimerTask {
    @Override
    public void run() {
      sendPendingRequests();
    }
  }

  public synchronized void sendPendingRequests() {
    waitUntilRunning();
    this.pendingSendTaskScheduled = false;
    final Map<ObjectID, Set<AbstractServerMapRequestContext>> segregatedPending = getPendingRequestSegregated();
    if (!segregatedPending.isEmpty()) {
      sendSegregatedPendingRequests(segregatedPending);
    }
  }

  private Map<ObjectID, Set<AbstractServerMapRequestContext>> getPendingRequestSegregated() {
    final HashMap<ObjectID, Set<AbstractServerMapRequestContext>> segregatedPending = new HashMap<ObjectID, Set<AbstractServerMapRequestContext>>();
    for (final AbstractServerMapRequestContext ols : this.outstandingRequests.values()) {
      if (ols.isPending()) {
        ols.makeUnPending();
        final ObjectID key = ols.getMapObjectID();
        Set<AbstractServerMapRequestContext> contexts = segregatedPending.get(key);
        if (contexts == null) {
          contexts = new HashSet<AbstractServerMapRequestContext>();
          segregatedPending.put(key, contexts);
        }
        contexts.add(ols);
      }
    }
    return segregatedPending;
  }

  // TODO:: Do batching here
  private void sendSegregatedPendingRequests(final Map<ObjectID, Set<AbstractServerMapRequestContext>> segregatedPending) {
    for (final Entry<ObjectID, Set<AbstractServerMapRequestContext>> e : segregatedPending.entrySet()) {
      for (final AbstractServerMapRequestContext context : e.getValue()) {
        sendRequestNow(context);
      }
    }
  }

  private void sendRequestNow(final AbstractServerMapRequestContext context) {
    final ServerMapRequestMessage msg = this.smmFactory.newServerTCMapRequestMessage(this.groupID, context
        .getRequestType());
    context.initializeMessage(msg);
    msg.send();
  }

  private AbstractServerMapRequestContext createSizeRequestContext(final ObjectID mapId) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetSizeServerMapRequestContext context = new GetSizeServerMapRequestContext(requestID, mapId, this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  synchronized void requestOutstanding() {
    for (final Object element : this.outstandingRequests.entrySet()) {
      final AbstractServerMapRequestContext context = (AbstractServerMapRequestContext) element;
      sendRequestNow(context);
    }
  }

  private void removeRequestContext(final AbstractServerMapRequestContext context) {
    final Object old = this.outstandingRequests.remove(context.getRequestID());
    if (old != context) { throw new AssertionError("Removed wrong context. context = " + context + " old = " + old); }
  }

  private AbstractServerMapRequestContext createRequestContext(final ObjectID oid, final Object portableKey) {
    final ServerMapRequestID requestID = getNextRequestID();
    final GetValueServerMapRequestContext context = new GetValueServerMapRequestContext(requestID, oid, portableKey,
                                                                                        this.groupID);
    this.outstandingRequests.put(requestID, context);
    return context;
  }

  private AbstractServerMapRequestContext getRequestContext(final ServerMapRequestID requestID) {
    return this.outstandingRequests.get(requestID);
  }

  public synchronized void addResponseForKeyValueMapping(final SessionID sessionID, final ObjectID mapID,
                                                         final ServerMapRequestID requestID,
                                                         final Object portableValue, final NodeID nodeID) {
    setResultForRequest(sessionID, mapID, requestID, portableValue, nodeID);
  }

  public synchronized void addResponseForGetSize(final SessionID sessionID, final ObjectID mapID,
                                                 final ServerMapRequestID requestID, final Integer size,
                                                 final NodeID nodeID) {
    setResultForRequest(sessionID, mapID, requestID, size, nodeID);
  }

  private void setResultForRequest(final SessionID sessionID, final ObjectID mapID, final ServerMapRequestID requestID,
                                   final Object result, final NodeID nodeID) {
    waitUntilRunning();
    if (!this.sessionManager.isCurrentSession(nodeID, sessionID)) {
      this.logger.warn("Ignoring response for Server Map request :  " + mapID + " , " + requestID + " , " + result
                       + " : from a different session: " + sessionID + ", " + this.sessionManager);
      return;
    }
    final AbstractServerMapRequestContext context = getRequestContext(requestID);
    if (context != null) {
      context.setResult(mapID, result);
    } else {
      this.logger.warn("Server Map Request Context is null for " + mapID + " request ID : " + requestID + " result : "
                       + result);
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
  private static abstract class AbstractServerMapRequestContext extends LookupStateTransitionAdaptor {

    // protected final static TCLogger logger = TCLogging.getLogger(AbstractServerMapRequestContext.class);

    protected final ObjectID             oid;
    protected final GroupID              groupID;
    protected final ServerMapRequestID   requestID;
    protected final ServerMapRequestType requestType;
    protected Object                     result;

    public AbstractServerMapRequestContext(final ServerMapRequestType requestType, final ServerMapRequestID requestID,
                                           final ObjectID mapID, final GroupID groupID) {

      this.requestType = requestType;
      this.requestID = requestID;
      this.oid = mapID;
      this.groupID = groupID;
    }

    public ObjectID getMapObjectID() {
      return this.oid;
    }

    public ServerMapRequestID getRequestID() {
      return this.requestID;
    }

    public ServerMapRequestType getRequestType() {
      return this.requestType;
    }

    public void setResult(final ObjectID mapID, final Object result) {
      if (!this.oid.equals(mapID)) { throw new AssertionError("Wrong request to response : this map id : " + this.oid
                                                              + " response is for : " + mapID + " type : "
                                                              + getRequestType()); }
      this.result = result;
    }

    public Object getResult() {
      return this.result;
    }

    @Override
    public int hashCode() {
      return this.requestID.hashCode();
    }

    @Override
    public boolean equals(final Object o) {
      if (o == this) { return true; }
      if (!(o instanceof AbstractServerMapRequestContext)) { return false; }
      final AbstractServerMapRequestContext other = (AbstractServerMapRequestContext) o;
      return (this.requestID.equals(other.requestID) && this.requestType.equals(other.requestType)
              && this.oid.equals(other.oid) && this.groupID.equals(other.groupID));
    }

    public abstract void initializeMessage(ServerMapRequestMessage requestMessage);

  }

  private class GetSizeServerMapRequestContext extends AbstractServerMapRequestContext {

    public GetSizeServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                          final GroupID groupID) {
      super(ServerMapRequestType.GET_SIZE, requestID, mapID, groupID);
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetSizeServerMapRequestMessage) requestMessage).initializeGetSizeRequest(this.requestID, this.oid);
    }

  }

  private class GetValueServerMapRequestContext extends AbstractServerMapRequestContext {

    private final Object portableKey;

    public GetValueServerMapRequestContext(final ServerMapRequestID requestID, final ObjectID mapID,
                                           final Object portableKey, final GroupID groupID) {
      super(ServerMapRequestType.GET_VALUE_FOR_KEY, requestID, mapID, groupID);
      this.portableKey = portableKey;
    }

    @Override
    public void initializeMessage(final ServerMapRequestMessage requestMessage) {
      ((GetValueServerMapRequestMessage) requestMessage).initializeGetValueRequest(this.requestID, this.oid,
                                                                                   this.portableKey);
    }

  }
}
