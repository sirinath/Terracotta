/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l2.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.l2.context.ManagedObjectSyncContext;
import com.tc.l2.context.SyncObjectsRequest;
import com.tc.l2.objectserver.L2ObjectStateManager;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;

public class L2ObjectSyncRequestHandler extends AbstractEventHandler {

  private static final TCLogger      logger                        = TCLogging
                                                                       .getLogger(L2ObjectSyncRequestHandler.class);
  private static final int           L2_OBJECT_SYNC_BATCH_SIZE     = TCPropertiesImpl
                                                                       .getProperties()
                                                                       .getInt(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE);

  private static final int           MAX_L2_OBJECT_SYNC_BATCH_SIZE = 5000;
  private final L2ObjectStateManager l2ObjectStateMgr;
  private Sink                       sendSink;

  private static final int           L2_OBJECT_SYNC_CONCURRENCY    = TCPropertiesImpl.getProperties()
                                                                       .getInt("object.sync.concurrency", 10);

  public L2ObjectSyncRequestHandler(final L2ObjectStateManager objectStateManager) {
    this.l2ObjectStateMgr = objectStateManager;

    if (L2_OBJECT_SYNC_BATCH_SIZE <= 0) {
      throw new AssertionError(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE
                               + " cant be less than or equal to zero.");
    } else if (L2_OBJECT_SYNC_BATCH_SIZE > MAX_L2_OBJECT_SYNC_BATCH_SIZE) {
      logger.warn(TCPropertiesConsts.L2_OBJECTMANAGER_PASSIVE_SYNC_BATCH_SIZE + " set too high : "
                  + L2_OBJECT_SYNC_BATCH_SIZE);
    }
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof SyncObjectsRequest) {
      doSyncObjectsRequest((SyncObjectsRequest) context);
    } else {
      throw new AssertionError("Unknown event context " + context);
    }
  }

  private void doSyncObjectsRequest(SyncObjectsRequest request) {
    if (request.isInitial()) {
      for (int i = 0; i < L2_OBJECT_SYNC_CONCURRENCY * 3; i++) {
        if (!addSyncDehydrationContext(request)) {
          break;
        }
      }
    } else {
      addSyncDehydrationContext(request);
    }
  }

  private boolean addSyncDehydrationContext(SyncObjectsRequest request) {
    ManagedObjectSyncContext mosc = l2ObjectStateMgr.getSomeObjectsToSyncContext(request.getNodeID(),
                                                                                 L2_OBJECT_SYNC_BATCH_SIZE);
    if (mosc == null) {
      logger.info("ingnoring request for node id " + request.getNodeID());
      return false;
    }
    // logger.info("Adding mosc for processing " + mosc + request);
    this.sendSink.add(mosc);
    return mosc.hasMore();
  }

  @Override
  public void initialize(final ConfigurationContext context) {
    super.initialize(context);
    final ServerConfigurationContext oscc = (ServerConfigurationContext) context;
    this.sendSink = oscc.getStage(ServerConfigurationContext.OBJECTS_SYNC_DEHYDRATE_STAGE).getSink();
  }
}
