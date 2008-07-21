/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.l1.L1InfoMBean;
import com.tc.object.DistributedObjectClient;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.api.LockRequest;
import com.tc.runtime.JVMMemoryManager;
import com.tc.runtime.MemoryUsage;
import com.tc.runtime.TCRuntime;
import com.tc.statistics.StatisticData;
import com.tc.statistics.StatisticRetrievalAction;
import com.tc.util.runtime.ThreadDumpUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.management.NotCompliantMBeanException;

public class L1Info extends AbstractTerracottaMBean implements L1InfoMBean {
  private static final TCLogger    logger                   = TCLogging.getLogger(L1Info.class);
  private final String             rawConfigText;
  private final ClientLockManager  lockManager;
  private final JVMMemoryManager   manager;
  private StatisticRetrievalAction cpuSRA;
  private String[]                 cpuNames;
  private Map                      heldLockByThreadIDMap    = new HashMap();
  private Map                      pendingLockByThreadIDMap = new HashMap();

  public L1Info(DistributedObjectClient client, String rawConfigText) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, false);

    this.manager = TCRuntime.getJVMMemoryManager();
    this.rawConfigText = rawConfigText;
    this.lockManager = client.getLockManager();
    try {
      Class sraCpuType = Class.forName("com.tc.statistics.retrieval.actions.SRACpuCombined");
      if (sraCpuType != null) {
        this.cpuSRA = (StatisticRetrievalAction) sraCpuType.newInstance();
      }
    } catch (LinkageError e) {
      /**
       * it's ok not output any errors or warnings here since when the CVT is initialized, it will notify about the
       * incapacity of leading Sigar-based SRAs.
       */
    } catch (Exception e) {
      /**/
    }
  }

  // for tests
  public L1Info(ClientLockManager lockManager) throws NotCompliantMBeanException {
    super(L1InfoMBean.class, false);
    this.rawConfigText = null;
    this.manager = TCRuntime.getJVMMemoryManager();
    this.lockManager = lockManager;
  }

  public String getEnvironment() {
    StringBuffer sb = new StringBuffer();
    Properties env = System.getProperties();
    Enumeration keys = env.propertyNames();
    ArrayList l = new ArrayList();

    while (keys.hasMoreElements()) {
      Object o = keys.nextElement();
      if (o instanceof String) {
        String key = (String) o;
        l.add(key);
      }
    }

    String[] props = (String[]) l.toArray(new String[0]);
    Arrays.sort(props);
    l.clear();
    l.addAll(Arrays.asList(props));

    int maxKeyLen = 0;
    Iterator iter = l.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      maxKeyLen = Math.max(key.length(), maxKeyLen);
    }

    iter = l.iterator();
    while (iter.hasNext()) {
      String key = (String) iter.next();
      sb.append(key);
      sb.append(":");
      int spaceLen = maxKeyLen - key.length() + 1;
      for (int i = 0; i < spaceLen; i++) {
        sb.append(" ");
      }
      sb.append(env.getProperty(key));
      sb.append("\n");
    }

    return sb.toString();
  }

  public String getConfig() {
    return rawConfigText;
  }

  public String takeThreadDump(long requestMillis) {
    Set heldLockSet = new HashSet();
    Set pendingLockSet = new HashSet();

    synchronized (this.lockManager) {
      boolean paused = false;
      if (!this.lockManager.isStarting()) {
        paused = true;
        this.lockManager.pause();
        this.lockManager.starting();
      }

      this.lockManager.addAllHeldLocksTo(heldLockSet);
      this.lockManager.addAllPendingLockRequestsTo(pendingLockSet);

      if (paused) {
        this.lockManager.unpause();
      }
    }

    heldLockByThreadIDMap.clear();
    for (Iterator i = heldLockSet.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      Long threadID = new Long(request.threadID().toLong());
      String lockInfo = (String) heldLockByThreadIDMap.get(threadID);
      if (lockInfo == null) {
        heldLockByThreadIDMap.put(threadID, request.lockID().toString());
      } else {
        heldLockByThreadIDMap.put(threadID, lockInfo + "; " + request.lockID().toString());
      }
    }

    pendingLockByThreadIDMap.clear();
    for (Iterator i = pendingLockSet.iterator(); i.hasNext();) {
      LockRequest request = (LockRequest) i.next();
      Long threadID = new Long(request.threadID().toLong());
      String lockInfo = (String) pendingLockByThreadIDMap.get(threadID);
      if (lockInfo == null) {
        pendingLockByThreadIDMap.put(threadID, request.lockID().toString());
      } else {
        pendingLockByThreadIDMap.put(threadID, lockInfo + "; " + request.lockID().toString());
      }
    }

    String text = ThreadDumpUtil.getThreadDump(heldLockByThreadIDMap, pendingLockByThreadIDMap);
    logger.info(text);
    return text;
  }

  // for test
  public Map getHeldLocksByThreadIDMap() {
    return this.heldLockByThreadIDMap;
  }

  // for test
  public Map getPendingLocksByThreadIDMap() {
    return this.pendingLockByThreadIDMap;
  }

  public String[] getCpuStatNames() {
    if (cpuNames != null) return cpuNames;
    if (cpuSRA == null) return cpuNames = new String[0];

    List list = new ArrayList();
    StatisticData[] statsData = cpuSRA.retrieveStatisticData();
    if (statsData != null) {
      for (int i = 0; i < statsData.length; i++) {
        list.add(statsData[i].getElement());
      }
    }
    return cpuNames = (String[]) list.toArray(new String[0]);
  }

  public Map getStatistics() {
    HashMap map = new HashMap();
    MemoryUsage usage = manager.getMemoryUsage();

    map.put(MEMORY_USED, new Long(usage.getUsedMemory()));
    map.put(MEMORY_MAX, new Long(usage.getMaxMemory()));

    if (cpuSRA != null) {
      StatisticData[] statsData = getCpuUsage();
      if (statsData != null) {
        map.put(CPU_USAGE, statsData);
      }
    }

    return map;
  }

  public StatisticData[] getCpuUsage() {
    if (cpuSRA != null) { return cpuSRA.retrieveStatisticData(); }
    return null;
  }

  public void reset() {
    /**/
  }

}
