/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime.logging;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.runtime.MemoryEventsListener;
import com.tc.runtime.MemoryUsage;

public class LongGCLogger implements MemoryEventsListener {

  private static final TCLogger logger = TCLogging.getLogger(LongGCLogger.class);
  private final long            gcTimeout;
  private MemoryUsage           lastMemoryUsage;

  public LongGCLogger(final long gcTimeOut) {
    this.gcTimeout = gcTimeOut;
  }

  @Override
  public void memoryUsed(MemoryUsage currentUsage) {
    if (lastMemoryUsage == null) {
      lastMemoryUsage = currentUsage;
      return;
    }
    long countDiff = currentUsage.getCollectionCount() - lastMemoryUsage.getCollectionCount();
    long timeDiff = currentUsage.getCollectionTime() - lastMemoryUsage.getCollectionTime();
    if (countDiff > 0 && timeDiff > gcTimeout) {

      TerracottaOperatorEvent tcEvent = TerracottaOperatorEventFactory.createLongGCOperatorEvent(
          new Object[] { gcTimeout, countDiff, timeDiff });

      fireLongGCEvent(tcEvent);
    }
    lastMemoryUsage = currentUsage;
  }

  protected void fireLongGCEvent(TerracottaOperatorEvent tcEvent) {
    logger.warn(tcEvent.getEventMessage());
  }
}
