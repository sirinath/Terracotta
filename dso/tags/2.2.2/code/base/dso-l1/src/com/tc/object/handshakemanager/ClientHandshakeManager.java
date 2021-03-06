/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.logging.TCLogger;
import com.tc.net.protocol.tcm.ChannelEvent;
import com.tc.net.protocol.tcm.ChannelEventListener;
import com.tc.net.protocol.tcm.ChannelEventType;
import com.tc.net.protocol.tcm.ChannelIDProvider;
import com.tc.object.ClientObjectManager;
import com.tc.object.ObjectID;
import com.tc.object.PauseListener;
import com.tc.object.RemoteObjectManager;
import com.tc.object.context.PauseContext;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockContext;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.object.lockmanager.api.WaitContext;
import com.tc.object.lockmanager.api.WaitLockRequest;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.session.SessionManager;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.util.State;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;

public class ClientHandshakeManager implements ChannelEventListener {
  // private static final State INIT = new State("INIT");
  private static final State                   PAUSED             = new State("PAUSED");
  private static final State                   STARTING           = new State("STARTING");
  private static final State                   RUNNING            = new State("RUNNING");

  private final ClientObjectManager            objectManager;
  private final ClientLockManager              lockManager;
  private final ChannelIDProvider              cidp;
  private final ClientHandshakeMessageFactory  chmf;
  private final RemoteObjectManager            remoteObjectManager;
  private final ClientGlobalTransactionManager gtxManager;
  private final TCLogger                       logger;
  private final Collection                     stagesToPauseOnDisconnect;
  private final Sink                           pauseSink;
  private final SessionManager                 sessionManager;
  private final PauseListener                  pauseListener;
  private final BatchSequenceReceiver          sequenceReceiver;

  private State                                state              = PAUSED;
  private boolean                              stagesPaused       = false;
  private boolean                              serverIsPersistent = false;

  public ClientHandshakeManager(TCLogger logger, ChannelIDProvider cidp, ClientHandshakeMessageFactory chmf,
                                ClientObjectManager objectManager, RemoteObjectManager remoteObjectManager,
                                ClientLockManager lockManager, RemoteTransactionManager remoteTransactionManager,
                                ClientGlobalTransactionManager gtxManager, Collection stagesToPauseOnDisconnect,
                                Sink pauseSink, SessionManager sessionManager, PauseListener pauseListener,
                                BatchSequenceReceiver sequenceReceiver) {
    this.logger = logger;
    this.cidp = cidp;
    this.chmf = chmf;
    this.objectManager = objectManager;
    this.remoteObjectManager = remoteObjectManager;
    this.lockManager = lockManager;
    this.gtxManager = gtxManager;
    this.stagesToPauseOnDisconnect = stagesToPauseOnDisconnect;
    this.pauseSink = pauseSink;
    this.sessionManager = sessionManager;
    this.pauseListener = pauseListener;
    this.sequenceReceiver = sequenceReceiver;
    pauseManagers();
  }

  public void initiateHandshake() {
    logger.debug("Initiating handshake...");
    state = STARTING;
    notifyManagersStarting();

    ClientHandshakeMessage handshakeMessage = chmf.newClientHandshakeMessage();

    handshakeMessage.setTransactionSequenceIDs(gtxManager.getTransactionSequenceIDs());
    handshakeMessage.setResentTransactionIDs(gtxManager.getResentTransactionIDs());

    logger.debug("Getting object ids...");
    for (Iterator i = objectManager.getAllObjectIDsAndClear(new HashSet()).iterator(); i.hasNext();) {
      handshakeMessage.addObjectID((ObjectID) i.next());
    }

    logger.debug("Getting lock holders...");
    for (Iterator i = lockManager.addAllHeldLocksTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), cidp.getChannelID(), request.threadID(), request.lockLevel());
      handshakeMessage.addLockContext(ctxt);
    }

    logger.debug("Getting lock waiters...");
    for (Iterator i = lockManager.addAllWaitersTo(new HashSet()).iterator(); i.hasNext();) {
      WaitLockRequest request = (WaitLockRequest) i.next();
      WaitContext ctxt = new WaitContext(request.lockID(), cidp.getChannelID(), request.threadID(),
                                         request.lockLevel(), request.getWaitInvocation());
      handshakeMessage.addWaitContext(ctxt);
    }

    logger.debug("Getting pending lock requests...");
    for (Iterator i = lockManager.addAllPendingLockRequestsTo(new HashSet()).iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      LockContext ctxt = new LockContext(request.lockID(), cidp.getChannelID(), request.threadID(), request.lockLevel());
      handshakeMessage.addPendingLockContext(ctxt);
    }

    logger.debug("Checking to see if is object ids sequence is needed ...");
    handshakeMessage.setIsObjectIDsRequested(!sequenceReceiver.hasNext());

    logger.debug("Sending handshake message...");
    handshakeMessage.send();
  }

  public void notifyChannelEvent(ChannelEvent event) {
    if (event.getType() == ChannelEventType.TRANSPORT_DISCONNECTED_EVENT) {
      sessionManager.newSession();
      pauseSink.add(PauseContext.PAUSE);
    } else if (event.getType() == ChannelEventType.TRANSPORT_CONNECTED_EVENT) {
      pauseSink.add(PauseContext.UNPAUSE);
    }
  }

  public void pause() {
    logger.info("Pause " + state);
    if (state == PAUSED) {
      logger.warn("pause called while already PAUSED");
      return;
    }
    pauseStages();
    pauseManagers();
    state = PAUSED;
  }

  public void unpause() {
    logger.info("Unpause " + state);
    if (state != PAUSED) {
      logger.warn("unpause called while not PAUSED: " + state);
      return;
    }
    unpauseStages();
    initiateHandshake();
  }

  public void acknowledgeHandshake(long objectIDStart, long objectIDEnd, boolean persistentServer) {
    if (state != STARTING) {
      logger.warn("Handshake acknowledged while not STARTING: " + state);
      return;
    }

    this.serverIsPersistent = persistentServer;

    state = RUNNING;
    if (objectIDStart < objectIDEnd) {
      logger.debug("Setting the ObjectID sequence to: " + objectIDStart + " , " + objectIDEnd);
      sequenceReceiver.setNextBatch(objectIDStart, objectIDEnd);
    }

    logger.debug("Re-requesting outstanding object requests...");
    remoteObjectManager.requestOutstanding();

    logger.debug("Handshake acknowledged.  Resending incomplete transactions...");
    gtxManager.resendOutstandingAndUnpause();
    unpauseManagers();
  }

  private void pauseManagers() {
    lockManager.pause();
    objectManager.pause();
    remoteObjectManager.pause();
    gtxManager.pause();
    pauseListener.notifyPause();
  }

  private void notifyManagersStarting() {
    lockManager.starting();
    objectManager.starting();
    remoteObjectManager.starting();
    gtxManager.starting();
  }

  // XXX:: Note that gtxmanager is actually unpaused outside this method as it
  // has to resend transactions and unpause in a single step.
  private void unpauseManagers() {
    lockManager.unpause();
    objectManager.unpause();
    remoteObjectManager.unpause();
    pauseListener.notifyUnpause();
  }

  private void pauseStages() {
    if (!stagesPaused) {
      logger.debug("Pausing stages...");
      for (Iterator i = stagesToPauseOnDisconnect.iterator(); i.hasNext();) {
        ((Stage) i.next()).pause();
      }
      stagesPaused = true;
    } else {
      logger.debug("pauseStages(): Stages are paused; not pausing stages.");
    }
  }

  private void unpauseStages() {
    if (stagesPaused) {
      logger.debug("Unpausing stages...");
      for (Iterator i = stagesToPauseOnDisconnect.iterator(); i.hasNext();) {
        ((Stage) i.next()).unpause();
      }
      stagesPaused = false;
    } else {
      logger.debug("unpauseStages(): Stages not paused; not unpausing stages.");
    }
  }

  /**
   *
   */
  public boolean serverIsPersistent() {
    return this.serverIsPersistent;
  }

}
