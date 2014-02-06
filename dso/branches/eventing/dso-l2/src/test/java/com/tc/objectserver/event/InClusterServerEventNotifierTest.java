package com.tc.objectserver.event;

import static com.tc.server.ServerEventType.EVICT;
import static com.tc.server.ServerEventType.EXPIRE;
import static com.tc.server.ServerEventType.PUT;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import org.junit.Before;
import org.junit.Test;

import com.tc.net.ClientID;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.net.DSOChannelManager;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;

import java.util.Arrays;
import java.util.EnumSet;

/**
 * @author Eugene Shelestovich
 */
public class InClusterServerEventNotifierTest {

  private final ClientID clientId1 = new ClientID(1L);
  private final ClientID clientId2 = new ClientID(2L);
  private final ClientID clientId3 = new ClientID(3L);

  private InClusterServerEventNotifier notifier;
  private ServerEventBatcher batcher;

  @Before
  public void setUp() {
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    batcher = mock(ServerEventBatcher.class);
    notifier = new InClusterServerEventNotifier(channelManager, batcher);

    notifier.register(clientId1, "cache1", EnumSet.of(PUT, EVICT));
    notifier.register(clientId1, "cache2", EnumSet.of(PUT));
    notifier.register(clientId2, "cache1", EnumSet.of(EXPIRE));
    notifier.register(clientId2, "cache1", EnumSet.of(EVICT));
    notifier.register(clientId2, "cache3", EnumSet.of(EXPIRE, EVICT));
    notifier.register(clientId3, "cache2", EnumSet.of(PUT, EXPIRE));
  }

  @Test
  public void testShouldCorrectlyRouteEventsAfterRegistration() throws Exception {
    final ServerEvent event1 = new BasicServerEvent(PUT, 1001, "cache1");
    final ServerEvent event11 = new BasicServerEvent(EVICT, 1011, "cache1");
    final ServerEvent event2 = new BasicServerEvent(PUT, 1002, "cache2");
    final ServerEvent event3 = new BasicServerEvent(EXPIRE, 1003, "cache3");
    final ServerEvent event33 = new BasicServerEvent(PUT, 1033, "cache3");

    final GlobalTransactionID gtxId1 = new GlobalTransactionID(1);
    final GlobalTransactionID gtxId2 = new GlobalTransactionID(2);
    final GlobalTransactionID gtxId3 = new GlobalTransactionID(3);

    notifier.handleServerEvent(ServerEventWrapper.createBeginEvent(gtxId1));
    notifier.handleServerEvent(ServerEventWrapper.createBeginEvent(gtxId2));
    notifier.handleServerEvent(ServerEventWrapper.createBeginEvent(gtxId3));

    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId1, event1));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId1, event11));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId2, event2));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId3, event3));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId3, event33));

    notifier.handleServerEvent(ServerEventWrapper.createEndEvent(gtxId1));
    notifier.handleServerEvent(ServerEventWrapper.createEndEvent(gtxId2));
    notifier.handleServerEvent(ServerEventWrapper.createEndEvent(gtxId3));

    verify(batcher).add(gtxId1, clientId1, Arrays.asList(event1, event11));
    verify(batcher).add(gtxId1, clientId2, Arrays.asList(event11));
    verify(batcher).add(gtxId2, clientId1, Arrays.asList(event2));
    verify(batcher).add(gtxId2, clientId3, Arrays.asList(event2));
    verify(batcher).add(gtxId3, clientId2, Arrays.asList(event3));
    verify(batcher, never()).add(any(GlobalTransactionID.class), any(ClientID.class), eq(Arrays.asList(event33)));
    verifyNoMoreInteractions(batcher);
  }

  @Test
  public void testShouldCorrectlyRouteEventsAfterUnregisteration() {
    final ServerEvent event1 = new BasicServerEvent(EVICT, 1001, "cache1");
    final ServerEvent event2 = new BasicServerEvent(EXPIRE, 1002, "cache1");
    final ServerEvent event3 = new BasicServerEvent(PUT, 1003, "cache2");

    final GlobalTransactionID gtxId1 = new GlobalTransactionID(1);
    final GlobalTransactionID gtxId2 = new GlobalTransactionID(2);

    notifier.unregister(clientId2, "cache1", EnumSet.of(EVICT, PUT));
    notifier.unregister(clientId3, "cache2", EnumSet.of(PUT, EXPIRE));

    notifier.handleServerEvent(ServerEventWrapper.createBeginEvent(gtxId1));
    notifier.handleServerEvent(ServerEventWrapper.createBeginEvent(gtxId2));

    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId1, event1));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId1, event2));
    notifier.handleServerEvent(ServerEventWrapper.createServerEventWrapper(gtxId2, event3));

    notifier.handleServerEvent(ServerEventWrapper.createEndEvent(gtxId1));
    notifier.handleServerEvent(ServerEventWrapper.createEndEvent(gtxId2));

    verify(batcher).add(gtxId1, clientId1, Arrays.asList(event1));
    verify(batcher, never()).add(gtxId1, clientId2, Arrays.asList(event1));
    verify(batcher).add(gtxId1, clientId2, Arrays.asList(event2));
    verify(batcher).add(gtxId2, clientId1, Arrays.asList(event3));
    verify(batcher, never()).add(gtxId2, clientId3, Arrays.asList(event3));
    verifyNoMoreInteractions(batcher);
  }

}
