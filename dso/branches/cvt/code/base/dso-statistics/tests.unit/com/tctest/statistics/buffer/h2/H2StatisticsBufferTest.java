/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.buffer.h2;

import com.tc.statistics.CaptureSession;
import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.StatisticsBufferListener;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.h2.H2StatisticsBufferImpl;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseNotReadyException;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.TCAssertionError;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import junit.framework.TestCase;

public class H2StatisticsBufferTest extends TestCase {
  private StatisticsBuffer buffer;

  public void setUp() throws Exception {
    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    buffer = new H2StatisticsBufferImpl(tmp_dir);
    buffer.open();
  }

  public void tearDown() throws Exception {
    buffer.close();
  }

  public void testInvalidBufferDirectory() throws Exception {
    try {
      new H2StatisticsBufferImpl(null);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir can't be null
    }

    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.delete();
    try {
      new H2StatisticsBufferImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir doesn't exist
    }

    tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.delete();
    tmp_dir.createNewFile();
    try {
      new H2StatisticsBufferImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // path is not a dir
    } finally {
      tmp_dir.delete();
    }

    tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.setReadOnly();
    try {
      new H2StatisticsBufferImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir is not writable
    } finally {
      tmp_dir.delete();
    }
  }

  public void testOpenClose() throws Exception {
    // several opens and closes are silently detected
    buffer.open();
    buffer.open();
    buffer.close();
    buffer.close();
  }

  public void testCloseUnopenedBuffer() throws Exception {
    buffer.close();

    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    StatisticsBuffer newBuffer = new H2StatisticsBufferImpl(tmp_dir);
    newBuffer.close(); // should not throw an exception
  }

  public void testCreateCaptureSessionUnopenedBuffer() throws Exception {
    buffer.close();
    try {
      buffer.createCaptureSession();
      fail("expected exception");
    } catch (TCStatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof TCStatisticsDatabaseNotReadyException);
    }
  }

  public void testCreateCaptureSession() throws Exception {
    CaptureSession session1 = buffer.createCaptureSession();
    assertEquals(1L, session1.getId());
    assertNotNull(session1.getRetriever());
    assertEquals(1L, session1.getRetriever().getSessionId());

    CaptureSession session2 = buffer.createCaptureSession();
    assertEquals(2L, session2.getId());
    assertNotNull(session2.getRetriever());
    assertEquals(2L, session2.getRetriever().getSessionId());

    CaptureSession session3 = buffer.createCaptureSession();
    assertEquals(3L, session3.getId());
    assertNotNull(session3.getRetriever());
    assertEquals(3L, session3.getRetriever().getSessionId());
  }

  public void testStoreStatisticsDataNullSessionId() throws Exception {
    long sessionid = buffer.createCaptureSession().getId();
    try {
      buffer.storeStatistic(new StatisticData());
      fail("expected exception");
    } catch (NullPointerException e) {
      // sessionId can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentIp() throws Exception {
    long sessionid = buffer.createCaptureSession().getId();
    try {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid)));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentIp can't be null
    }
  }

  public void testStoreStatisticsDataNullData() throws Exception {
    long sessionid = buffer.createCaptureSession().getId();
    try {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid))
        .agentIp(InetAddress.getLocalHost().getHostAddress()));
      fail("expected exception");
    } catch (NullPointerException e) {
      // data can't be null
    }
  }

  public void testStoreStatisticsUnopenedBuffer() throws Exception {
    long sessionid = buffer.createCaptureSession().getId();

    buffer.close();
    try {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid))
        .agentIp(InetAddress.getLocalHost().getHostAddress())
        .data("test"));
      fail("expected exception");
    } catch (TCStatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof TCStatisticsDatabaseNotReadyException);
    }
  }

  public void testStoreStatistics() throws Exception {
    long sessionid1 = buffer.createCaptureSession().getId();

    long statid1 = buffer.storeStatistic(new StatisticData()
      .sessionId(new Long(sessionid1))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat")
      .data("stuff"));
    assertEquals(1, statid1);

    long statid2 = buffer.storeStatistic(new StatisticData()
      .sessionId(new Long(sessionid1))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat")
      .data("stuff2"));
    assertEquals(2, statid2);

    long sessionid2 = buffer.createCaptureSession().getId();

    long statid3 = buffer.storeStatistic(new StatisticData()
      .sessionId(new Long(sessionid2))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat 2")
      .data("stuff3"));
    assertEquals(3, statid3);
  }

  public void testConsumeStatisticsInvalidSessionId() throws Exception {
    try {
      buffer.consumeStatistics(0, null);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // session ID has to be positive
    }
  }

  public void testConsumeStatisticsNullConsumer() throws Exception {
    try {
      buffer.consumeStatistics(1, null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // consumer can't be null
    }
  }

  public void testConsumeStatisticsUnopenedBuffer() throws Exception {
    long sessionid = buffer.createCaptureSession().getId();

    buffer.close();
    try {
      buffer.consumeStatistics(sessionid, new TestStaticticConsumer());
      fail("expected exception");
    } catch (TCStatisticsBufferException e) {
      // expected
      assertTrue(e.getCause() instanceof TCStatisticsDatabaseNotReadyException);
    }
  }

  public void testConsumeStatistics() throws Exception {
    long sessionid1 = buffer.createCaptureSession().getId();
    long sessionid2 = buffer.createCaptureSession().getId();
    populateBufferWithStatistics(sessionid1, sessionid2);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    buffer.consumeStatistics(sessionid1, consumer1);
    consumer1.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    buffer.consumeStatistics(sessionid1, consumer2);
    consumer2.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer();
    buffer.consumeStatistics(sessionid2, consumer3);
    consumer3.ensureCorrectCounts(70, 0);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    buffer.consumeStatistics(sessionid2, consumer4);
    consumer4.ensureCorrectCounts(0, 0);
  }

  public void testConsumeStatisticsInterruptions() throws Exception {
    long sessionid1 = buffer.createCaptureSession().getId();
    long sessionid2 = buffer.createCaptureSession().getId();
    populateBufferWithStatistics(sessionid1, sessionid2);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1);
    buffer.consumeStatistics(sessionid1, consumer1);
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countOffset1(1).countLimit1(98);
    buffer.consumeStatistics(sessionid1, consumer2);
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countOffset1(99).countLimit2(20);
    buffer.consumeStatistics(sessionid1, consumer3);
    consumer3.ensureCorrectCounts(1, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer().countOffset1(100).countOffset2(20);
    buffer.consumeStatistics(sessionid1, consumer4);
    consumer4.ensureCorrectCounts(0, 30);
  }

  public void testConsumeStatisticsExceptions() throws Exception {
    long sessionid1 = buffer.createCaptureSession().getId();
    long sessionid2 = buffer.createCaptureSession().getId();
    populateBufferWithStatistics(sessionid1, sessionid2);

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1).limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer1);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countOffset1(1)
      .countLimit1(98)
      .limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer2);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countOffset1(99)
      .countLimit2(20)
      .limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer3);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat2 limited", e.getMessage());
    }
    consumer3.ensureCorrectCounts(1, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer().countOffset1(100)
      .countOffset2(20)
      .limitWithExceptions(true);
    buffer.consumeStatistics(sessionid1, consumer4);
    consumer4.ensureCorrectCounts(0, 30);
  }

  public void testStatisticsBufferListeners() throws Exception {
    final CaptureSession session = buffer.createCaptureSession();
    TestStatisticsBufferListener listener1 = new TestStatisticsBufferListener(session.getId());
    buffer.addListener(listener1);
    TestStatisticsBufferListener listener2 = new TestStatisticsBufferListener(session.getId());
    buffer.addListener(listener2);

    assertFalse(listener1.isStarted());
    assertFalse(listener1.isStopped());

    buffer.startCapturing(session.getId());

    assertTrue(listener1.isStarted());
    assertFalse(listener1.isStopped());

    buffer.stopCapturing(session.getId());

    assertTrue(listener1.isStarted());
    assertTrue(listener1.isStopped());
  }

  public void testStartCapturingException() throws Exception {
    final CaptureSession session = buffer.createCaptureSession();
    buffer.startCapturing(session.getId());
    try {
      buffer.startCapturing(session.getId());
      fail();
    } catch (TCStatisticsBufferException e) {
      // excepted
    }
  }

  public void testStopCapturingException() throws Exception {
    final CaptureSession session = buffer.createCaptureSession();
    try {
      buffer.stopCapturing(session.getId());
      fail();
    } catch (TCStatisticsBufferException e) {
      // excepted
    }
  }

  private long populateBufferWithStatistics(long sessionid1, long sessionid2) throws TCStatisticsBufferException, UnknownHostException {
    String ip = InetAddress.getLocalHost().getHostAddress();
    for (int i = 1; i <= 100; i++) {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid1))
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .data(new Long(i)));
    }
    for (int i = 1; i <= 50; i++) {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid1))
        .agentIp(ip)
        .moment(new Date())
        .name("stat2")
        .data(String.valueOf(i)));
    }

    for (int i = 1; i <= 70; i++) {
      buffer.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid2))
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .data(new BigDecimal(String.valueOf(i+".0"))));
    }
    return sessionid1;
  }

  private class TestStaticticConsumer implements StatisticsConsumer {
    private int statCount1 = 0;
    private int statCount2 = 0;

    private int countOffset1 = 0;
    private int countOffset2 = 0;

    private int countLimit1 = 0;
    private int countLimit2 = 0;

    private boolean limitWithExceptions = false;

    public TestStaticticConsumer countOffset1(int countOffset1) {
      this.countOffset1 = countOffset1;
      return this;
    }

    public TestStaticticConsumer countOffset2(int countOffset2) {
      this.countOffset2 = countOffset2;
      return this;
    }

    public TestStaticticConsumer countLimit1(int countLimit1) {
      this.countLimit1 = countLimit1;
      return this;
    }

    public TestStaticticConsumer countLimit2(int countLimit2) {
      this.countLimit2 = countLimit2;
      return this;
    }

    public TestStaticticConsumer limitWithExceptions(boolean limitWithExceptions) {
      this.limitWithExceptions = limitWithExceptions;
      return this;
    }

    public boolean consumeStatisticData(StatisticData data) {
      if (data.getName().equals("stat1")) {
        if (countLimit1 > 0 &&
            countLimit1 == statCount1) {
          if (limitWithExceptions) {
            throw new RuntimeException("stat1 limited");
          } else {
            return false;
          }
        }
        statCount1++;
        assertEquals(((Long)data.getData()).longValue(), statCount1 + countOffset1);
      }
      if (data.getName().equals("stat2")) {
        if (countLimit2 > 0 &&
            countLimit2 == statCount2) {
          if (limitWithExceptions) {
            throw new RuntimeException("stat2 limited");
          } else {
            return false;
          }
        }
        statCount2++;
        assertEquals(String.valueOf(data.getData()), String.valueOf(statCount2 + countOffset2));
      }
      return true;
    }

    public void ensureCorrectCounts(int count1, int count2) {
      assertEquals(count1, statCount1);
      assertEquals(count2, statCount2);
    }
  }

  private class TestStatisticsBufferListener implements StatisticsBufferListener {
    private long sessionId;
    private boolean started = false;

    private boolean stopped = false;

    public TestStatisticsBufferListener(long sessionId) {
      this.sessionId = sessionId;
    }

    public boolean isStarted() {
      return started;
    }

    public boolean isStopped() {
      return stopped;
    }

    public void capturingStarted(long sessionId) {
      assertEquals(false, started);
      assertEquals(this.sessionId, sessionId);
      started = true;
    }

    public void capturingStopped(long sessionId) {
      assertEquals(false, stopped);
      assertEquals(this.sessionId, sessionId);
      stopped = true;
    }
  }
}