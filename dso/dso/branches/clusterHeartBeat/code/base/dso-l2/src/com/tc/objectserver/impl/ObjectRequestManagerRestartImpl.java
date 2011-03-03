/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import com.tc.heartbeat.ClusterHeartBeat;
import com.tc.heartbeat.ClusterHeartBeatListener;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.logging.TerracottaOperatorEventLogger;
import com.tc.logging.TerracottaOperatorEventLogging;
import com.tc.net.ClientID;
import com.tc.net.NodeID;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.tx.ServerTransactionID;
import com.tc.objectserver.api.NoSuchObjectException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.mgmt.ManagedObjectFacade;
import com.tc.objectserver.tx.AbstractServerTransactionListener;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.text.PrettyPrinter;
import com.tc.util.ObjectIDSet;
import com.tc.util.State;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;

public class ObjectRequestManagerRestartImpl extends AbstractServerTransactionListener implements ObjectRequestManager {

  private final static State                      INIT                                    = new State("INITIAL");
  private final static State                      STARTING                                = new State("STARTING");
  private final static State                      STARTED                                 = new State("STARTED");

  private final static TCLogger                   logger                                  = TCLogging
                                                                                              .getLogger(ObjectRequestManagerRestartImpl.class);

  private final ObjectRequestManager              delegate;
  private final ServerTransactionManager          transactionManager;
  private final ObjectManager                     objectManager;

  private final Set                               resentTransactionIDs                    = Collections
                                                                                              .synchronizedSet(new HashSet());
  private final Queue<ObjectRequestServerContext> pendingRequests                         = new LinkedBlockingQueue<ObjectRequestServerContext>();
  private volatile State                          state                                   = INIT;
  private static final int                        CLUSTER_HEART_BEAT_OBJREQUEST_THRESHOLD = TCPropertiesImpl
                                                                                              .getProperties()
                                                                                              .getInt(
                                                                                                      TCPropertiesConsts.CLUSTER_HEART_BEAT_OBJREQUEST_THRESHOLD,
                                                                                                      6);

  public ObjectRequestManagerRestartImpl(final ObjectManager objectMgr,
                                         final ServerTransactionManager transactionManager,
                                         final ObjectRequestManager delegate, final ClusterHeartBeat clusterHeartBeat) {
    this.objectManager = objectMgr;
    this.delegate = delegate;
    this.transactionManager = transactionManager;
    clusterHeartBeat.register(new ObjectRequestHeartBeatListener());
    transactionManager.addTransactionListener(this);
  }

  private class ObjectRequestHeartBeatListener implements ClusterHeartBeatListener {
    private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
    private final int                           beatThreshold;

    public ObjectRequestHeartBeatListener() {
      beatThreshold = CLUSTER_HEART_BEAT_OBJREQUEST_THRESHOLD;
    }

    public void heartbeat() {
      for (final ObjectRequestServerContext objReqServerContext : pendingRequests) {
        if (objReqServerContext.heartbeat() > this.beatThreshold) {
          logger.error("Stuck object request from " + objReqServerContext.getClientID() + " "
                       + objReqServerContext.getRequestID() + " " + objReqServerContext.getRequestedObjectIDs());
          operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory
              .createHeartBeatObjectRequestEvent(new Object[] { objReqServerContext.getClientID(),
                  objReqServerContext.getRequestID() }));
        }
      }
    }
  }

  @Override
  public void transactionManagerStarted(final Set cids) {
    this.state = STARTING;
    this.objectManager.start();
    moveToStartedIfPossible();
  }

  private void moveToStartedIfPossible() {
    if (this.state == STARTING && this.resentTransactionIDs.isEmpty()) {
      this.state = STARTED;
      this.transactionManager.removeTransactionListener(this);
      processPending();
    }
  }

  @Override
  public void addResentServerTransactionIDs(final Collection sTxIDs) {
    if (this.state != INIT) { throw new AssertionError("Cant add Resent transactions after start up ! " + sTxIDs.size()
                                                       + "Txns : " + this.state); }
    this.resentTransactionIDs.addAll(sTxIDs);
    logger.info("resentTransactions = " + this.resentTransactionIDs.size());
  }

  @Override
  public void transactionApplied(final ServerTransactionID stxID, final ObjectIDSet newObjectsCreated) {
    processResentTxnComplete(stxID);
  }

  protected void processResentTxnComplete(final ServerTransactionID stxID) {
    if (this.state != STARTING) { return; } // Skips in passive
    this.resentTransactionIDs.remove(stxID);
    moveToStartedIfPossible();
  }

  protected void processResentTxnComplete(final Set<ServerTransactionID> incomingTxnIds) {
    if (this.state == STARTED) { return; }
    for (final ServerTransactionID txnID : incomingTxnIds) {
      processResentTxnComplete(txnID);
    }
  }

  @Override
  public void clearAllTransactionsFor(final NodeID client) {
    if (this.state == STARTED) { return; }
    synchronized (this.resentTransactionIDs) {
      for (final Iterator iter = this.resentTransactionIDs.iterator(); iter.hasNext();) {
        final ServerTransactionID stxID = (ServerTransactionID) iter.next();
        if (stxID.getSourceID().equals(client)) {
          iter.remove();
        }
      }
    }
    moveToStartedIfPossible();
  }

  private void processPending() {
    logger.info("Processing Pending Lookups = " + this.pendingRequests.size());
    ObjectRequestServerContext lookupContext;
    while ((lookupContext = this.pendingRequests.poll()) != null) {
      logger.info("Processing pending Looking up : " + toString(lookupContext));
      this.delegate.requestObjects(lookupContext);
    }
  }

  public String toString(final ObjectRequestServerContext c) {
    return c.getClass().getName() + " [ " + c.getClientID() + " :  " + c.getRequestedObjectIDs() + " : "
           + c.getRequestID() + " : " + c.getRequestDepth() + " : " + c.getRequestingThreadName() + "] ";
  }

  public void requestObjects(final ObjectRequestServerContext requestContext) {
    if (this.state != STARTED) {
      this.pendingRequests.add(requestContext);
      if (logger.isDebugEnabled()) {
        logger.debug("RequestObjectManager is not started, lookup has been added to pending request: "
                     + toString(requestContext));
      }
      return;
    }
    this.delegate.requestObjects(requestContext);
  }

  public void sendObjects(final ClientID requestedNodeID, final Collection objs, final ObjectIDSet requestedObjectIDs,
                          final ObjectIDSet missingObjectIDs, final boolean isServerInitiated, final int maxRequestDepth) {

    this.delegate.sendObjects(requestedNodeID, objs, requestedObjectIDs, missingObjectIDs, isServerInitiated,
                              maxRequestDepth);

  }

  public PrettyPrinter prettyPrint(final PrettyPrinter out) {
    out.print(this.getClass().getSimpleName()).flush();
    out.indent().print("State: " + this.state).flush();
    out.indent().print("ResentTransactionIDs : " + this.resentTransactionIDs).flush();
    out.indent().print("PendingRequests: " + this.pendingRequests.size()).flush();
    for (final ObjectRequestServerContext objReqServerContext : this.pendingRequests) {
      out.duplicateAndIndent().indent().print(toString(objReqServerContext)).flush();
    }
    out.indent().print("ObjectRequestManager: ").visit(this.delegate).flush();
    return out;
  }

  // delegating all ObjectManagerMbean requests to the object manager
  public int getCachedObjectCount() {
    return this.objectManager.getCachedObjectCount();
  }

  public int getLiveObjectCount() {
    return this.objectManager.getLiveObjectCount();
  }

  public Iterator getRootNames() {
    return this.objectManager.getRootNames();
  }

  public Iterator getRoots() {
    return this.objectManager.getRoots();
  }

  public ManagedObjectFacade lookupFacade(final ObjectID id, final int limit) throws NoSuchObjectException {
    return this.objectManager.lookupFacade(id, limit);
  }

  public ObjectID lookupRootID(final String name) {
    return this.objectManager.lookupRootID(name);
  }

}
