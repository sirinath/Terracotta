package com.tc.objectserver.event;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerEventListener;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Sends L2 cache events to all interested L1 clients within the same cluster.
 *
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifier implements ServerEventListener, DSOChannelManagerEventListener,
    ServerEventRegistry {

  private static final TCLogger LOG = TCLogging.getLogger(InClusterServerEventNotifier.class);

  private final Map<ServerEventType, Map<ClientID, Set<String>>> registry = Maps.newEnumMap(ServerEventType.class);
  private final Map<GlobalTransactionID, Multimap<ClientID, ServerEvent>> eventMap = Maps.newHashMap(); //TODO: Do I need a concurrentmap
  private final ReadWriteLock lock = new ReentrantReadWriteLock();

  private final DSOChannelManager channelManager;
  private final ServerEventBatcher batcher;

  public InClusterServerEventNotifier(final DSOChannelManager channelManager, final ServerEventBatcher batcher) {
    this.batcher = batcher;
    this.channelManager = channelManager;
    this.channelManager.addEventListener(this);
  }


  @Override
  public final void handleServerEvent(final ServerEventWrapper eventWrapper) {
    switch (eventWrapper.getType()) {
      case BEGIN:
        Multimap<ClientID, ServerEvent> clientToEventMap = ArrayListMultimap.create();
        eventMap.put(eventWrapper.getGtxId(), clientToEventMap);
        break;

      case SERVER_EVENT:
        ServerEvent event = eventWrapper.getEvent();
        lock.readLock().lock();
        try {
          final Map<ClientID, Set<String>> clients = registry.get(event.getType());
          if (clients != null) {
            for (Map.Entry<ClientID, Set<String>> entry : clients.entrySet()) {
              final Set<String> destinations = entry.getValue();
              if (destinations.contains(event.getCacheName())) {
                eventMap.get(eventWrapper.getGtxId()).put(entry.getKey(), event);
              }
            }
          }
        } finally {
          lock.readLock().unlock();
        }
        break;

      case END: // TODO: Handle when node is Passive
        Multimap<ClientID, ServerEvent> eventsForTransaction = eventMap.get(eventWrapper.getGtxId());
        for (ClientID clientId : eventsForTransaction.keySet()) {
          List<ServerEvent> events = (List<ServerEvent>) eventsForTransaction.get(clientId);
          batcher.add(eventWrapper.getGtxId(), clientId, events); // TODO: special handling required for methods like
                                                                  // clear() which can generate enormous number of
                                                                  // events for a single transaction
        }

    }
  }

  /**
   * Registration is relatively rare operation comparing to event firing.
   * So we can loop through the registry instead of maintaining a reversed data structure.
   */
  @Override
  public final void register(final ClientID clientId, final String destination, final Set<ServerEventType> eventTypes) {
    LOG.error("############# Register Client: " + clientId + " for destination: " + destination + " and EventType: "
              + eventTypes);
    lock.writeLock().lock();
    try {
      for (ServerEventType eventType : eventTypes) {
        doRegister(clientId, destination, eventType);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void doRegister(final ClientID clientId, final String destination, final ServerEventType eventType) {
    Map<ClientID, Set<String>> clients = registry.get(eventType);
    if (clients == null) {
      clients = Maps.newHashMap();
      registry.put(eventType, clients);
    }

    Set<String> destinations = clients.get(clientId);
    if (destinations == null) {
      destinations = Sets.newHashSet();
      clients.put(clientId, destinations);
    }
    destinations.add(destination);

    if (LOG.isDebugEnabled()) {
      LOG.debug("Client '" + clientId + "' has registered server event listener for cache '"
                + destination + "' and event type '" + eventType + "'");
    }
  }

  @Override
  public final void unregister(final ClientID clientId, final String destination, final Set<ServerEventType> eventTypes) {
    lock.writeLock().lock();
    try {
      for (ServerEventType eventType : eventTypes) {
        doUnregister(clientId, destination, eventType);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  private void doUnregister(final ClientID clientId, final String destination, final ServerEventType eventType) {
    final Map<ClientID, Set<String>> clients = registry.get(eventType);
    if (clients != null) {
      final Set<String> destinations = clients.get(clientId);
      if (destinations != null) {
        destinations.remove(destination);

        // potential cascading removal
        if (destinations.isEmpty()) {
          clients.remove(clientId);
          if (clients.isEmpty()) {
            registry.remove(eventType);
          }
        }
      }
    }

    if (LOG.isDebugEnabled()) {
      LOG.debug("Client '" + clientId + "' has unregistered server event listener for cache '"
                + destination + "' and event type '" + eventType + "'");
    }
  }

  @Override
  public void channelCreated(final MessageChannel channel) {
    // ignore
  }

  @Override
  public void channelRemoved(final MessageChannel channel) {
    final ClientID clientId = channelManager.getClientIDFor(channel.getChannelID());
    if (clientId != null) {
      unregisterClient(clientId);
    }
  }

  private void unregisterClient(final ClientID clientId) {
    lock.writeLock().lock();
    try {
      for (Map<ClientID, Set<String>> clientToDestMap : registry.values()) {
        clientToDestMap.remove(clientId);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  public void acknowledgement(final Set<GlobalTransactionID> acknowledgedGtxIds) {
    for (GlobalTransactionID gtxId : acknowledgedGtxIds) {
      eventMap.remove(gtxId);
    }

    // TODO: Relay to Passive the acked serverevents
  }

}
