package com.tc.objectserver.event;


import com.google.common.collect.Maps;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.server.ServerEvent;
import com.tc.util.Assert;
import com.tc.util.concurrent.TaskRunner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * Accumulates server events in a buffer, periodically drains it and sends batches to clients.
 */
public class ServerEventBatcher implements Runnable {

  private static final TCLogger LOG = TCLogging.getLogger(ServerEventBatcher.class);

  private final DSOChannelManager channelManager;
  // multiple producers, single consumer
  private final BlockingQueue<ClientEnvelope> buffer;

  public ServerEventBatcher(final DSOChannelManager channelManager, final TaskRunner taskRunner) {
    this.channelManager = channelManager;
    final int queueSize = TCPropertiesImpl.getProperties()
        .getInt(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_QUEUE_SIZE, 1024);
    final long interval = TCPropertiesImpl.getProperties()
        .getLong(TCPropertiesConsts.L2_SERVER_EVENT_BATCHER_INTERVAL_MS, 50L);
    buffer = new ArrayBlockingQueue<ClientEnvelope>(queueSize);
    taskRunner.newTimer("Server event queue batcher")
        .scheduleWithFixedDelay(this, 20L, interval, TimeUnit.MILLISECONDS);
  }

  @Override
  public void run() {
    if (!buffer.isEmpty()) {
      drain();
    }
  }

  /**
   * Uses a retention policy similar to {@link java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy}.
   */
  public void add(final GlobalTransactionID gtxId, final ClientID clientId, final List<ServerEvent> events) {
    while (!buffer.offer(new ClientEnvelope(gtxId, clientId, events))) {
      drain();
    }
  }

  private void drain() { // TODO: Looks like it will have concurrency issues
    final List<ClientEnvelope> toProcess = new ArrayList<ClientEnvelope>(buffer.size());
    buffer.drainTo(toProcess);
    final Map<ClientID, Map<GlobalTransactionID, List<ServerEvent>>> groups = partition(toProcess);
    // send batch messages for each client
    for (final Entry<ClientID, Map<GlobalTransactionID, List<ServerEvent>>> entry : groups.entrySet()) {
      send(entry.getKey(), entry.getValue());
    }
  }

  Map<ClientID, Map<GlobalTransactionID, List<ServerEvent>>> partition(final Collection<ClientEnvelope> envelopes) {
    final Map<ClientID, Map<GlobalTransactionID, List<ServerEvent>>> groups = Maps.newHashMap();
    // partition by client id
    for (final ClientEnvelope envelope : envelopes) {
      Map<GlobalTransactionID, List<ServerEvent>> events = groups.get(envelope.clientId);
      if (events == null) {
        events = new HashMap<GlobalTransactionID, List<ServerEvent>>();
        groups.put(envelope.clientId, events);
      }
      List<ServerEvent> previousEventsForTxn = events.put(envelope.gtxId, envelope.events);
      // TODO: This assert will fail when events for one transaction are added in more than one chunk.
      // Ex when we split events for a "clear" transaction in multiple small chunks to be sent.
      Assert.assertNull("Can not add events for one transaction multiple times for one client", previousEventsForTxn);
    }
    return groups;
  }

  void send(final ClientID clientId, final Map<GlobalTransactionID, List<ServerEvent>> events) {
    final MessageChannel channel;
    try {
      channel = channelManager.getActiveChannel(clientId);
    } catch (NoSuchChannelException e) {
      LOG.warn("Cannot find channel for client: " + clientId + ". The client will no longer receive server events.");
      return;
    }
    // combine events in message batches, one batch per client
    final ServerEventBatchMessage msg = (ServerEventBatchMessage)channel.createMessage(TCMessageType.SERVER_EVENT_BATCH_MESSAGE);
    msg.setEvents(events);
    msg.send();

    LOG.error("$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$$ Sending events to client: " + events.size());// TODO: Remove it

    if (LOG.isDebugEnabled()) {
      LOG.debug("ServerEvents have been sent to client '" + clientId + "' for " + events.size() + " transactions.");
    }
  }

  /**
   * Simple holder for buffered events.
   */
  static final class ClientEnvelope {
    final ClientID clientId;
    final List<ServerEvent> events;
    final GlobalTransactionID gtxId;

    ClientEnvelope(final GlobalTransactionID gtxId, final ClientID clientId, final List<ServerEvent> events) {
      this.gtxId = gtxId;
      this.clientId = clientId;
      this.events = events;
    }
  }

}
