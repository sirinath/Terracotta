/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.buffer.StatisticsBuffer;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferException;
import com.tc.statistics.buffer.exceptions.TCStatisticsBufferNotReadyException;
import com.tc.statistics.buffer.h2.H2StatisticsBufferImpl;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.TCAssertionError;

import java.io.File;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;

import junit.framework.TestCase;

public class H2StatisticsBufferTest extends TestCase {
  private StatisticsBuffer buffer;
  
  public void setUp() throws Exception {
    File tmpDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    buffer = new H2StatisticsBufferImpl(tmpDir);
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

    File tmpDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmpDir.delete();
    try {
      new H2StatisticsBufferImpl(tmpDir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir doesn't exist
    }

    tmpDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmpDir.delete();
    tmpDir.createNewFile();
    try {
      new H2StatisticsBufferImpl(tmpDir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // path is not a dir
    } finally {
      tmpDir.delete();
    }
    
    tmpDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmpDir.setReadOnly();
    try {
      new H2StatisticsBufferImpl(tmpDir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir is not writable
    } finally {
      tmpDir.delete();
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
    
    File tmpDir = new TempDirectoryHelper(getClass(), true).getDirectory();
    StatisticsBuffer newBuffer = new H2StatisticsBufferImpl(tmpDir);
    newBuffer.close(); // should not throw an exception
  }
  
  public void testCreateCaptureSessionNullDate() throws Exception {
    try {
      buffer.createCaptureSession(null);
      fail("expected exception");
    } catch (NullPointerException e) {
      // start can't be null
    }
  }
  
  public void testCreateCaptureSessionUnopenedBuffer() throws Exception {
    buffer.close();
    try {
      buffer.createCaptureSession(new Date());
      fail("expected exception");
    } catch (TCStatisticsBufferNotReadyException e) {
      // expected
    }
  }
  
  public void testCreateCaptureSession() throws Exception {
    assertEquals(1L, buffer.createCaptureSession(new Date()));
    assertEquals(2L, buffer.createCaptureSession(new Date()));
    assertEquals(3L, buffer.createCaptureSession(new Date()));
  }
  
  public void testStoretatisticsDataNullAgentIp() throws Exception {
    long sessionid = buffer.createCaptureSession(new Date());
    try {
      buffer.storeStatistic(sessionid, new StatisticData());
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentIp can't be null
    }
  }
  
  public void testStoretatisticsDataNullData() throws Exception {
    long sessionid = buffer.createCaptureSession(new Date());
    try {
      buffer.storeStatistic(sessionid, new StatisticData()
        .agentIp(InetAddress.getLocalHost().getHostAddress()));
      fail("expected exception");
    } catch (NullPointerException e) {
      // data can't be null
    }
  }
  
  public void testStoreStatisticsUnopenedBuffer() throws Exception {
    long sessionid = buffer.createCaptureSession(new Date());

    buffer.close();
    try {
      buffer.storeStatistic(sessionid, new StatisticData()
        .agentIp(InetAddress.getLocalHost().getHostAddress())
        .data("test"));
      fail("expected exception");
    } catch (TCStatisticsBufferNotReadyException e) {
      // expected
    }
  }
  
  public void testStoreStatistics() throws Exception {
    long sessionid1 = buffer.createCaptureSession(new Date());
    
    long statid1 = buffer.storeStatistic(sessionid1, new StatisticData()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .element(0)
      .name("the stat")
      .data("stuff"));
    assertEquals(1, statid1);
    
    long statid2 = buffer.storeStatistic(sessionid1, new StatisticData()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .element(0)
      .name("the stat")
      .data("stuff2"));
    assertEquals(2, statid2);

    long sessionid2 = buffer.createCaptureSession(new Date());
    
    long statid3 = buffer.storeStatistic(sessionid2, new StatisticData()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .element(0)
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
    long sessionid = buffer.createCaptureSession(new Date());

    buffer.close();
    try {
      buffer.consumeStatistics(sessionid, new TestStaticticConsumer());
      fail("expected exception");
    } catch (TCStatisticsBufferNotReadyException e) {
      // expected
    }
  }
  
  public void testConsumeStatistics() throws Exception {
    long sessionid1 = buffer.createCaptureSession(new Date());
    long sessionid2 = buffer.createCaptureSession(new Date());
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
    long sessionid1 = buffer.createCaptureSession(new Date());
    long sessionid2 = buffer.createCaptureSession(new Date());
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
    long sessionid1 = buffer.createCaptureSession(new Date());
    long sessionid2 = buffer.createCaptureSession(new Date());
    populateBufferWithStatistics(sessionid1, sessionid2);
    
    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1).limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer1);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    
    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countOffset1(1).countLimit1(98).limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer2);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer2.ensureCorrectCounts(98, 0);
    
    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countOffset1(99).countLimit2(20).limitWithExceptions(true);
    try {
      buffer.consumeStatistics(sessionid1, consumer3);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat2 limited", e.getMessage());
    }
    consumer3.ensureCorrectCounts(1, 20);
    
    TestStaticticConsumer consumer4 = new TestStaticticConsumer().countOffset1(100).countOffset2(20).limitWithExceptions(true);
    buffer.consumeStatistics(sessionid1, consumer4);
    consumer4.ensureCorrectCounts(0, 30);
  }

  private long populateBufferWithStatistics(long sessionid1, long sessionid2) throws TCStatisticsBufferException, UnknownHostException {
    String ip = InetAddress.getLocalHost().getHostAddress();
    for (int i = 1; i <= 100; i++) {
      buffer.storeStatistic(sessionid1, new StatisticData()
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .data(new Long(i)));
    }
    for (int i = 1; i <= 50; i++) {
      buffer.storeStatistic(sessionid1, new StatisticData()
        .agentIp(ip)
        .moment(new Date())
        .name("stat2")
        .data(String.valueOf(i)));
    }
    
    for (int i = 1; i <= 70; i++) {
      buffer.storeStatistic(sessionid2, new StatisticData()
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .data(new Long(i)));
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
    
    public boolean consumeStatisticData(long sessionId, StatisticData data) {
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
        assertEquals(((Long)data.getData()).longValue(), statCount1+countOffset1);
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
        assertEquals(String.valueOf(data.getData()), String.valueOf(statCount2+countOffset2));
      }
      return true;
    }
    
    public void ensureCorrectCounts(int count1, int count2) {
      assertEquals(count1, statCount1);
      assertEquals(count2, statCount2);
    }
  }
}