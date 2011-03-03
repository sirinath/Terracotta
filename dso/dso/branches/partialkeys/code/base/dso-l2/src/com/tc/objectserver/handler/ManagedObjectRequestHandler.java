/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.ObjectID;
import com.tc.object.ObjectRequestServerContext;
import com.tc.object.msg.RequestManagedObjectMessage;
import com.tc.object.net.ChannelStats;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.stats.counter.Counter;
import com.tc.util.ObjectIDSet;

import java.util.Collection;
import java.util.Set;

/**
 * Converts the request into a call to the objectManager with the proper next steps initialized I'm not convinced that
 * this stage is necessary. May be able to merge it with another stage.
 */
public class ManagedObjectRequestHandler extends AbstractEventHandler {

  private ClientStateManager    stateManager;
  private ChannelStats          channelStats;

  private final Counter         globalObjectRequestCounter;
  private final Counter         globalObjectFlushCounter;
  private ObjectRequestManager  objectRequestManager;
  private Sink                  respondToObjectRequestStage;

  private static final TCLogger logger = TCLogging.getLogger(ManagedObjectRequestHandler.class);

  public ManagedObjectRequestHandler(final Counter globalObjectRequestCounter, final Counter globalObjectFlushCounter) {
    this.globalObjectRequestCounter = globalObjectRequestCounter;
    this.globalObjectFlushCounter = globalObjectFlushCounter;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof RequestManagedObjectMessage) {
      handleEventFromClient((RequestManagedObjectMessage) context);
    } else if (context instanceof ObjectRequestServerContext) {
      handleEventFromServer((ObjectRequestServerContext) context);
    } else {
      throw new AssertionError("Unknown context type : " + context);
    }
  }

  private void handleEventFromServer(final ObjectRequestServerContext context) {
    Collection<ObjectID> ids = context.getRequestedObjectIDs();
    // XXX::TODO:: Server initiated lookups are not updated to the channel counter for now
    final int numObjectsRequested = ids.size();
    if (numObjectsRequested != 0) {
      this.globalObjectRequestCounter.increment(numObjectsRequested);
    }
    this.objectRequestManager.requestObjects(context, this.respondToObjectRequestStage);
  }

  private void handleEventFromClient(final RequestManagedObjectMessage rmom) {
    MessageChannel channel = rmom.getChannel();
    Set<ObjectID> requestedIDs = rmom.getRequestedObjectIDs();
    ClientID clientID = (ClientID) rmom.getSourceNodeID();
    ObjectIDSet removedIDs = rmom.getRemoved();

    final int numObjectsRequested = requestedIDs.size();
    if (numObjectsRequested != 0) {
      this.globalObjectRequestCounter.increment(numObjectsRequested);
      this.channelStats.notifyObjectRequest(channel, numObjectsRequested);
    }

    final int numObjectsRemoved = removedIDs.size();
    if (numObjectsRemoved != 0) {
      this.globalObjectFlushCounter.increment(numObjectsRemoved);
      this.channelStats.notifyObjectRemove(channel, numObjectsRemoved);
    }

    long t = System.currentTimeMillis();
    this.stateManager.removeReferences(clientID, removedIDs);
    t = System.currentTimeMillis() - t;
    if (t > 1000 || numObjectsRemoved > 100000) {
      logger.warn("Time to Remove " + numObjectsRemoved + " is " + t + " ms");
    }
    if (numObjectsRequested > 0) {
      this.objectRequestManager.requestObjects(rmom, this.respondToObjectRequestStage);
    }
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.stateManager = oscc.getClientStateManager();
    this.channelStats = oscc.getChannelStats();
    this.objectRequestManager = oscc.getObjectRequestManager();
    this.respondToObjectRequestStage = oscc.getStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE)
        .getSink();
  }
}
