/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.TunneledDomainUpdater;
import com.tc.object.ObjectID;
import com.tc.object.TCObjectExternal;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.NamedClassLoader;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.Notify;
import com.tc.object.locks.UnclusteredLockID;
import com.tc.object.logging.InstrumentationLogger;
import com.tc.object.logging.NullInstrumentationLogger;
import com.tc.properties.TCProperties;
import com.tc.statistics.StatisticRetrievalAction;

import java.lang.reflect.Field;

import javax.management.MBeanServer;

/**
 * Null implementation of the manager.
 */
public class NullManager implements Manager {

  public static final String                 CLASS                 = "com/tc/object/bytecode/NullManager";
  public static final String                 TYPE                  = "L" + CLASS + ";";

  private static final InstrumentationLogger instrumentationLogger = new NullInstrumentationLogger();
  private static final Manager               INSTANCE              = new NullManager();

  /**
   * Get instance of the null manager
   * 
   * @return NullManager
   */
  public static Manager getInstance() {
    return INSTANCE;
  }

  private NullManager() {
    //
  }

  public void init() {
    //
  }

  public void initForTests() {
    //
  }

  public void stop() {
    //
  }

  public Object lookupOrCreateRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  public Object lookupOrCreateRootNoDepth(String name, Object obj) {
    throw new UnsupportedOperationException();
  }

  public Object createOrReplaceRoot(String name, Object object) {
    throw new UnsupportedOperationException();
  }

  public TCObjectExternal lookupExistingOrNull(Object obj) {
    return null;
  }

  public void logicalInvoke(Object object, String methodName, Object[] params) {
    //
  }

  public boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
    return true;
  }

  public void distributedMethodCallCommit() {
    //
  }

  public void checkWriteAccess(Object context) {
    //
  }

  public boolean isManaged(Object object) {
    return false;
  }

  public boolean isLiteralInstance(Object object) {
    return false;
  }

  public boolean isLiteralAutolock(Object o) {
    return false;
  }

  public int calculateDsoHashCode(Object object) {
    return 0;
  }

  public boolean isLogical(Object object) {
    throw new UnsupportedOperationException();
  }

  public boolean isRoot(Field field) {
    return false;
  }

  public Object lookupRoot(String name) {
    throw new UnsupportedOperationException();
  }

  public boolean isLockHeldByCurrentThread(String lockId, int lockLevel) {
    return false;
  }

  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    throw new UnsupportedOperationException();
  }

  public boolean isPhysicallyInstrumented(Class clazz) {
    return false;
  }

  public String getClientID() {
    // XXX: even though this should *probably* throw UnsupportedOperationException, because some innocent tests use
    // ManagerUtil (e.g. ConfigPropertiesTest), it was decided to return "" from this method.
    return "";
  }

  public TCLogger getLogger(String loggerName) {
    return new NullTCLogger();
  }

  public TCObjectExternal lookupOrCreate(Object obj) {
    throw new UnsupportedOperationException();
  }

  public Object lookupObject(ObjectID id) {
    throw new UnsupportedOperationException();
  }

  public Object lookupObject(ObjectID id, ObjectID parentContext) {
    throw new UnsupportedOperationException();
  }

  public TCProperties getTCProperties() {
    throw new UnsupportedOperationException();
  }

  public int localHeldCount(Object obj, int lockLevel) {
    throw new UnsupportedOperationException();
  }

  public boolean isDsoMonitored(Object obj) {
    return false;
  }

  public boolean isDsoMonitorEntered(Object obj) {
    return false;
  }

  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset) {
    throw new UnsupportedOperationException();
  }

  public InstrumentationLogger getInstrumentationLogger() {
    return instrumentationLogger;
  }

  public boolean overridesHashCode(Object obj) {
    throw new UnsupportedOperationException();
  }

  public void registerNamedLoader(NamedClassLoader loader, String webAppName) {
    throw new UnsupportedOperationException();
  }

  public ClassProvider getClassProvider() {
    throw new UnsupportedOperationException();
  }

  public TunneledDomainUpdater getTunneledDomainUpdater() {
    throw new UnsupportedOperationException();
  }

  public DsoCluster getDsoCluster() {
    throw new UnsupportedOperationException();
  }

  public MBeanServer getMBeanServer() {
    return null;
  }

  public void preFetchObject(ObjectID id) {
    return;
  }

  public StatisticRetrievalAction getStatisticRetrievalActionInstance(String name) {
    return null;
  }

  public Object getChangeApplicator(Class clazz) {
    return null;
  }

  public LockID generateLockIdentifier(String str) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public LockID generateLockIdentifier(Object obj) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public LockID generateLockIdentifier(Object obj, String field) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public int globalHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public int globalPendingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public int globalWaitingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public boolean isLocked(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public int localHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public void lock(LockID lock, LockLevel level) {
    //
  }

  public void lockInterruptibly(LockID lock, LockLevel level) {
    //
  }

  public Notify notify(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notify();
    }
    return null;
  }

  public Notify notifyAll(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notifyAll();
    }
    return null;
  }

  public boolean tryLock(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public boolean tryLock(LockID lock, LockLevel level, long timeout) {
    throw new UnsupportedOperationException();
  }

  public void unlock(LockID lock, LockLevel level) {
    //
  }

  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait();
    }
  }

  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait(timeout);
    }
  }

  public void pinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public void unpinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public boolean isLockedByCurrentThread(LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public void monitorEnter(LockID lock, LockLevel level) {
    //
  }

  public void monitorExit(LockID lock, LockLevel level) {
    //
  }

  public String getUUID() {
    return null;
  }

  public SessionConfiguration getSessionConfiguration(String appName) {
    throw new UnsupportedOperationException();
  }

  public void waitForAllCurrentTransactionsToComplete() {
    throw new UnsupportedOperationException();
  }

  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    throw new UnsupportedOperationException();
  }

  public void registerStatisticRetrievalAction(StatisticRetrievalAction sra) {
    //
  }
}
