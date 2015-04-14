package com.tc.operatorevent;

import org.junit.Assert;
import org.junit.Test;

/**
 * Created by rism on 9/19/14.
 */
public class TerracottaOperatorEventFactoryTest {
  @Test
  public void testZapRequestReceivedEventEventType() {
    TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createZapRequestReceivedEvent(null);
    Assert.assertEquals(event.getEventType(), TerracottaOperatorEvent.EventType.WARN);
  }

  @Test
  public void testZapRequestAcceptedEventEventType() {
    TerracottaOperatorEvent event = TerracottaOperatorEventFactory.createZapRequestAcceptedEvent(null);
    Assert.assertEquals(event.getEventType(), TerracottaOperatorEvent.EventType.WARN);
  }
}