/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.statistics.store.h2;

import com.tc.statistics.StatisticData;
import com.tc.statistics.buffer.StatisticsConsumer;
import com.tc.statistics.database.exceptions.TCStatisticsDatabaseNotReadyException;
import com.tc.statistics.store.StatisticsRetrievalCriteria;
import com.tc.statistics.store.StatisticsStore;
import com.tc.statistics.store.exceptions.TCStatisticsStoreException;
import com.tc.statistics.store.h2.H2StatisticsStoreImpl;
import com.tc.test.TempDirectoryHelper;
import com.tc.util.TCAssertionError;

import java.io.File;
import java.math.BigDecimal;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

public class H2StatisticsStoreTest extends TestCase {
  private StatisticsStore store;

  public void setUp() throws Exception {
    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    store = new H2StatisticsStoreImpl(tmp_dir);
    store.open();
  }

  public void tearDown() throws Exception {
    store.close();
  }

  public void testInvalidBufferDirectory() throws Exception {
    try {
      new H2StatisticsStoreImpl(null);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir can't be null
    }

    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.delete();
    try {
      new H2StatisticsStoreImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir doesn't exist
    }

    tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.delete();
    tmp_dir.createNewFile();
    try {
      new H2StatisticsStoreImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // path is not a dir
    } finally {
      tmp_dir.delete();
    }

    tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    tmp_dir.setReadOnly();
    try {
      new H2StatisticsStoreImpl(tmp_dir);
      fail("expected exception");
    } catch (TCAssertionError e) {
      // dir is not writable
    } finally {
      tmp_dir.delete();
    }
  }

  public void testOpenClose() throws Exception {
    // several opens and closes are silently detected
    store.open();
    store.open();
    store.close();
    store.close();
  }

  public void testCloseUnopenedBuffer() throws Exception {
    store.close();

    File tmp_dir = new TempDirectoryHelper(getClass(), true).getDirectory();
    StatisticsStore newStore = new H2StatisticsStoreImpl(tmp_dir);
    newStore.close(); // should not throw an exception
  }

  public void testStoreStatisticsDataNullSessionId() throws Exception {
    try {
      store.storeStatistic(new StatisticData());
      fail("expected exception");
    } catch (NullPointerException e) {
      // sessionId can't be null
    }
  }

  public void testStoreStatisticsDataNullAgentIp() throws Exception {
    try {
      store.storeStatistic(new StatisticData().sessionId(new Long(374938L)));
      fail("expected exception");
    } catch (NullPointerException e) {
      // agentIp can't be null
    }
  }

  public void testStoreStatisticsDataNullData() throws Exception {
    try {
      store.storeStatistic(new StatisticData()
        .sessionId(new Long(374938L))
        .agentIp(InetAddress.getLocalHost().getHostAddress()));
      fail("expected exception");
    } catch (NullPointerException e) {
      // data can't be null
    }
  }

  public void testStoreStatisticsUnopenedBuffer() throws Exception {
    store.close();
    try {
      store.storeStatistic(new StatisticData()
        .sessionId(new Long(342L))
        .agentIp(InetAddress.getLocalHost().getHostAddress())
        .data("test"));
      fail("expected exception");
    } catch (TCStatisticsStoreException e) {
      // expected
      assertTrue(e.getCause() instanceof TCStatisticsDatabaseNotReadyException);
    }
  }

  public void testStoreStatistics() throws Exception {
    long statid1 = store.storeStatistic(new StatisticData()
      .sessionId(new Long(376487L))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat")
      .data("stuff"));
    assertEquals(1, statid1);

    long statid2 = store.storeStatistic(new StatisticData()
      .sessionId(new Long(376487L))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat")
      .data("stuff2"));
    assertEquals(2, statid2);

    long statid3 = store.storeStatistic(new StatisticData()
      .sessionId(new Long(2232L))
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .moment(new Date())
      .name("the stat 2")
      .data("stuff3"));
    assertEquals(3, statid3);
  }

  public void testRetrieveStatistics() throws Exception {
    Long sessionid1 = new Long(34987L);
    Long sessionid2 = new Long(9367L);
    
    Date before = new Date();
    Thread.sleep(500);
    populateBufferWithStatistics(sessionid1.longValue(), sessionid2.longValue());
    Thread.sleep(500);
    Date after = new Date();

    TestStaticticConsumer consumer1 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria(), consumer1);
    consumer1.ensureCorrectCounts(170, 50);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .sessionId(sessionid1)
      .addName("stat1"), consumer2);
    consumer2.ensureCorrectCounts(100, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .agentIp(InetAddress.getLocalHost().getHostAddress())
      .sessionId(sessionid1)
      .addName("stat1")
      .addName("stat2"), consumer3);
    consumer3.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .agentIp("unknown")
      .sessionId(sessionid2), consumer4);
    consumer4.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer5 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .sessionId(sessionid2)
      .addName("stat1")
      .addElement("element1"), consumer5);
    consumer5.ensureCorrectCounts(70, 0);

    TestStaticticConsumer consumer6 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .sessionId(sessionid1)
      .addName("stat1")
      .addElement("element1"), consumer6);
    consumer6.ensureCorrectCounts(100, 0);

    TestStaticticConsumer consumer7 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .sessionId(sessionid1)
      .addElement("element1")
      .addElement("element2"), consumer7);
    consumer7.ensureCorrectCounts(100, 50);

    TestStaticticConsumer consumer8 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .start(before), consumer8);
    consumer8.ensureCorrectCounts(170, 50);

    TestStaticticConsumer consumer9 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .stop(before), consumer9);
    consumer9.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer10 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .start(after), consumer10);
    consumer10.ensureCorrectCounts(0, 0);

    TestStaticticConsumer consumer11 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria()
      .start(before)
      .stop(after), consumer11);
    consumer11.ensureCorrectCounts(170, 50);
  }

  public void testConsumeStatisticsInterruptions() throws Exception {
    Long sessionid1 = new Long(34987L);
    Long sessionid2 = new Long(9367L);
    populateBufferWithStatistics(sessionid1.longValue(), sessionid2.longValue());

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer1);
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer().countLimit1(98);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer2);
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer().countLimit2(20);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer3);
    consumer3.ensureCorrectCounts(100, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer();
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer4);
    consumer4.ensureCorrectCounts(100, 50);
  }

  public void testConsumeStatisticsExceptions() throws Exception {
    Long sessionid1 = new Long(34987L);
    Long sessionid2 = new Long(9367L);
    populateBufferWithStatistics(sessionid1.longValue(), sessionid2.longValue());

    TestStaticticConsumer consumer1 = new TestStaticticConsumer().countLimit1(1).limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer1);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer1.ensureCorrectCounts(1, 0);

    TestStaticticConsumer consumer2 = new TestStaticticConsumer()
      .countLimit1(98)
      .limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer2);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat1 limited", e.getMessage());
    }
    consumer2.ensureCorrectCounts(98, 0);

    TestStaticticConsumer consumer3 = new TestStaticticConsumer()
      .countLimit2(20)
      .limitWithExceptions(true);
    try {
      store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer3);
      fail("expected exception");
    } catch (RuntimeException e) {
      assertEquals("stat2 limited", e.getMessage());
    }
    consumer3.ensureCorrectCounts(100, 20);

    TestStaticticConsumer consumer4 = new TestStaticticConsumer()
      .limitWithExceptions(true);
    store.retrieveStatistics(new StatisticsRetrievalCriteria().sessionId(sessionid1), consumer4);
    consumer4.ensureCorrectCounts(100, 50);
  }

  private long populateBufferWithStatistics(long sessionid1, long sessionid2) throws TCStatisticsStoreException, UnknownHostException {
    String ip = InetAddress.getLocalHost().getHostAddress();
    for (int i = 1; i <= 100; i++) {
      store.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid1))
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .element("element1")
        .data(new Long(i)));
    }
    for (int i = 1; i <= 50; i++) {
      store.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid1))
        .agentIp(ip)
        .moment(new Date())
        .name("stat2")
        .element("element2")
        .data(String.valueOf(i)));
    }

    for (int i = 1; i <= 70; i++) {
      store.storeStatistic(new StatisticData()
        .sessionId(new Long(sessionid2))
        .agentIp(ip)
        .moment(new Date())
        .name("stat1")
        .element("element1")
        .data(new BigDecimal(String.valueOf(i+".0"))));
    }
    return sessionid1;
  }

  private class TestStaticticConsumer implements StatisticsConsumer {
    private int statCount1 = 0;
    private int statCount2 = 0;

    private int countLimit1 = 0;
    private int countLimit2 = 0;

    private Map lastDataPerSession = new HashMap();

    private boolean limitWithExceptions = false;

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
      StatisticData previous = (StatisticData)lastDataPerSession.get(data.getSessionId());
      if (previous != null) {
        assertTrue(previous.getMoment().compareTo(data.getMoment()) <= 0);
      }

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
      }

      lastDataPerSession.put(data.getSessionId(), data);

      return true;
    }

    public void ensureCorrectCounts(int count1, int count2) {
      assertEquals(count1, statCount1);
      assertEquals(count2, statCount2);
    }
  }

}
