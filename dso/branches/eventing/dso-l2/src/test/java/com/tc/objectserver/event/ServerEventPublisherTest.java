/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */

package com.tc.objectserver.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.common.eventbus.EventBus;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.BasicServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventPublisherTest {

  @Test
  public void testShouldPostEventsToListeners() {
    final String cacheName = "test-cache";
    final ServerEventPublisher publisher = new ServerEventPublisher(new EventBus("test-bus"));
    final ServerEventDumper consumer1 = new ServerEventDumper();
    final TypedServerEventDumper consumer2 = new TypedServerEventDumper();
    publisher.register(consumer1);
    publisher.register(consumer2);

    final ServerEvent event1 = new BasicServerEvent(ServerEventType.PUT, 1, new byte[] { 101 }, cacheName);
    final ServerEvent event2 = new BasicServerEvent(ServerEventType.REMOVE, 1, cacheName);
    final ServerEvent event3 = new BasicServerEvent(ServerEventType.EVICT, 2, cacheName);
    final GlobalTransactionID gtxId = new GlobalTransactionID(1);

    publisher.post(ServerEventWrapper.createServerEventWrapper(gtxId, event1));
    publisher.post(ServerEventWrapper.createServerEventWrapper(gtxId, event2));
    publisher.post(ServerEventWrapper.createServerEventWrapper(gtxId, event3));

    assertConsumer(consumer1);
    assertConsumer(consumer2);
  }

  private void assertConsumer(final Consumer consumer) {
    assertEquals(3, consumer.getEvents().size());
    assertTrue(consumer.getEvents().get(0).getEvent().getType() == ServerEventType.PUT);
    assertTrue(consumer.getEvents().get(1).getEvent().getType() == ServerEventType.REMOVE);
    assertTrue(consumer.getEvents().get(2).getEvent().getType() == ServerEventType.EVICT);
  }

  private static final class ServerEventDumper implements ServerEventListener, Consumer {
    private final List<ServerEventWrapper> events = new ArrayList<ServerEventWrapper>();

    @Override
    public void handleServerEvent(final ServerEventWrapper event) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + event);
      events.add(event);
    }

    @Override
    public List<ServerEventWrapper> getEvents() {
      return events;
    }
  }

  private static final class TypedServerEventDumper implements ServerEventListener, Consumer {
    private final List<ServerEventWrapper> events = new ArrayList<ServerEventWrapper>();

    @Override
    public List<ServerEventWrapper> getEvents() {
      return events;
    }

    @Override
    public void handleServerEvent(final ServerEventWrapper event) {
      System.out.println(getClass().getSimpleName() + " has received a new message: " + event);
      events.add(event);
    }
  }

  private interface Consumer {
    List<ServerEventWrapper> getEvents();
  }
}
