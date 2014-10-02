/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.runtime;

import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.operatorevent.TerracottaOperatorEvent;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.operatorevent.TerracottaOperatorEventCallback;
import com.tc.operatorevent.TerracottaOperatorEventLogging;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.test.TCTestCase;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class LongGCLoggerTest extends TCTestCase {

  private final int      LOOP_COUNT   = 20;
  private final int      OBJECT_COUNT = 1000;
  private CountDownLatch latch;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    latch = new CountDownLatch(1);
  }

  public void testBasic() throws Exception {
    TerracottaOperatorEventLogging.getEventLogger().registerEventCallback(new TerracottaOperatorEventCallback() {

      TCLogger logger = TCLogging.getLogger(LongGCLoggerTest.class);

      @Override
      public void logOperatorEvent(TerracottaOperatorEvent event) {
        if (event.getEventType() == EventType.WARN) {
          logger.warn(event);
          if (event.toString().contains("Frequent long GC")) {
            latch.countDown();
          } else {
            logger.warn("testBasic: latch not counted down");
          }
        } else {
          logger.info(event);
        }
      }
    });
    register();
    // Create some data for GC in a diff thread
    createThreadAndCollectGarbage();
    // wait in a thread to get notified
    latch.await();
  }

  private void createThreadAndCollectGarbage() {
    Runnable runnable = new Runnable() {
      @Override
      public void run() {
        createGarbage();
      }
    };
    new Thread(runnable).start();
  }

  private void createGarbage() {
    byte[][] byteArray = null;
    byteArray = new byte[10][];

    for (int i = 0; i < LOOP_COUNT; i++) {
      if (i != 0) System.gc();

      for (int j = 0; j < 10; j++) {
        int length = getByteArraySize() / 10;
        byteArray[j] = new byte[length];
      }

      addObjectsToArrayList();
    }
  }

  private void addObjectsToArrayList() {
    List<Integer> list = new ArrayList<Integer>();

    for (int i = 0; i < OBJECT_COUNT; i++) {
      list.add(new Integer(i));
    }
  }

  private int getByteArraySize() {
    Runtime runtime = Runtime.getRuntime();
    long max_memory = runtime.maxMemory();
    if (max_memory == Long.MAX_VALUE) {
      // With no upperbound it is possible that this test wont pass
      throw new AssertionError("This test is memory sensitive. Please specify the max memory using -Xmx option. "
                               + "Currently Max Memory is " + max_memory);
    }
    System.err.println("Max memory is " + max_memory);
    int blockSize = (int) ((max_memory) / 4);
    System.err.println("Memory block size is " + blockSize);
    return blockSize;
  }

  private void register() {
    TCThreadGroup thrdGrp = new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(LongGCLoggerTest.class)));
    TCMemoryManagerImpl tcMemManager = new TCMemoryManagerImpl(1, 2, thrdGrp);
    LongGCLogger logger = new TestLongGCLogger(1);
    tcMemManager.registerForMemoryEvents(logger);
  }

  private static class TestLongGCLogger extends LongGCLogger {

    public TestLongGCLogger(long gcTimeOut) {
      super(gcTimeOut);
    }

    @Override
    protected void fireLongGCEvent(TerracottaOperatorEvent tcEvent) {
      super.fireLongGCEvent(tcEvent);
      TerracottaOperatorEventLogging.getEventLogger().fireOperatorEvent(tcEvent);
    }
  }
}
