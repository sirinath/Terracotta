/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
import com.tc.logging.NullTCLogger;
import com.tc.logging.TCLogger;
import com.tc.management.beans.sessions.SessionMonitor;
import com.tc.object.ObjectID;
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

  private static final Manager               INSTANCE              = new NullManager();

  private static final InstrumentationLogger instrumentationLogger = new NullInstrumentationLogger();

  /**
   * Get instance of the null manager
   * 
   * @return NullManager
   */
  public static Manager getInstance() {
    return INSTANCE;
  }

  NullManager() {
    //
  }

  public final void init() {
    //
  }

  public final void initForTests() {
    //
  }

  public final void stop() {
    //
  }

  public final Object lookupOrCreateRoot(final String name, final Object object) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupOrCreateRootNoDepth(final String name, final Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object createOrReplaceRoot(final String name, final Object object) {
    throw new UnsupportedOperationException();
  }

  public final void logicalInvoke(final Object object, final String methodName, final Object[] params) {
    //
  }

  public final boolean distributedMethodCall(final Object receiver, final String method, final Object[] params,
                                             final boolean runOnAllNodes) {
    return true;
  }

  public final void distributedMethodCallCommit() {
    //
  }

  public final void checkWriteAccess(final Object context) {
    //
  }

  public final boolean isManaged(final Object object) {
    return false;
  }

  public final boolean isLiteralInstance(final Object object) {
    return false;
  }

  public final int calculateDsoHashCode(final Object object) {
    return 0;
  }

  public final boolean isLogical(final Object object) {
    throw new UnsupportedOperationException();
  }

  public final boolean isRoot(final Field field) {
    return false;
  }

  public final Object lookupRoot(final String name) {
    throw new UnsupportedOperationException();
  }

  public final void logicalInvokeWithTransaction(final Object object, final Object lockObject, final String methodName,
                                                 final Object[] params) {
    throw new UnsupportedOperationException();
  }

  public final boolean isPhysicallyInstrumented(final Class clazz) {
    return false;
  }

  public final String getClientID() {
    // XXX: even though this should *probably* throw UnsupportedOperationException, because some innocent tests use
    // ManagerUtil (e.g. ConfigPropertiesTest), it was decided to return "" from this method.
    return "";
  }

  public final TCLogger getLogger(final String loggerName) {
    return new NullTCLogger();
  }

  public final SessionMonitor getHttpSessionMonitor() {
    throw new UnsupportedOperationException();
  }

  public final Object lookupObject(final ObjectID id) {
    throw new UnsupportedOperationException();
  }

  public final Object lookupObject(final ObjectID id, final ObjectID parentContext) {
    throw new UnsupportedOperationException();
  }

  public final TCProperties getTCProperties() {
    throw new UnsupportedOperationException();
  }

  public final boolean isDsoMonitored(final Object obj) {
    return false;
  }

  public final boolean isDsoMonitorEntered(final Object obj) {
    return false;
  }

  public final boolean isFieldPortableByOffset(final Object pojo, final long fieldOffset) {
    throw new UnsupportedOperationException();
  }

  public final InstrumentationLogger getInstrumentationLogger() {
    return instrumentationLogger;
  }

  public final boolean overridesHashCode(final Object obj) {
    throw new UnsupportedOperationException();
  }

  public final void registerNamedLoader(final NamedClassLoader loader, final String webAppName) {
    throw new UnsupportedOperationException();
  }

  public final ClassProvider getClassProvider() {
    throw new UnsupportedOperationException();
  }

  public final DsoCluster getDsoCluster() {
    throw new UnsupportedOperationException();
  }

  public final MBeanServer getMBeanServer() {
    return null;
  }

  public final void preFetchObject(final ObjectID id) {
    return;
  }

  public final StatisticRetrievalAction getStatisticRetrievalActionInstance(final String name) {
    return null;
  }

  public final Object getChangeApplicator(final Class clazz) {
    return null;
  }

  public final LockID generateLockIdentifier(String str) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public final LockID generateLockIdentifier(Object obj) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public final LockID generateLockIdentifier(Object obj, String field) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  public final int globalHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final int globalPendingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public final int globalWaitingCount(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public final boolean isLocked(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final int localHoldCount(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final void lock(LockID lock, LockLevel level) {
    //
  }

  public final void lockInterruptibly(LockID lock, LockLevel level) {
    //
  }

  public final Notify notify(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notify();
    }
    return null;
  }

  public final Notify notifyAll(LockID lock, Object waitObject) {
    if (waitObject != null) {
      waitObject.notifyAll();
    }
    return null;
  }

  public final boolean tryLock(LockID lock, LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final boolean tryLock(LockID lock, LockLevel level, long timeout) {
    throw new UnsupportedOperationException();
  }

  public final void unlock(LockID lock, LockLevel level) {
    //
  }

  public final void wait(LockID lock, Object waitObject) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait();
    }
  }

  public final void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    if (waitObject != null) {
      waitObject.wait(timeout);
    }
  }

  public final void pinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public final void unpinLock(LockID lock) {
    throw new UnsupportedOperationException();
  }

  public final boolean isLockedByCurrentThread(LockLevel level) {
    throw new UnsupportedOperationException();
  }

  public final void monitorEnter(LockID lock, LockLevel level) {
    //
  }

  public final void monitorExit(LockID lock, LockLevel level) {
    //
  }

  public final String getUUID() {
    return null;
  }

  public final SessionConfiguration getSessionConfiguration(String appName) {
    throw new UnsupportedOperationException();
  }

  public final void waitForAllCurrentTransactionsToComplete() {
    throw new UnsupportedOperationException();
  }

  public final void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    throw new UnsupportedOperationException();
  }

  public final void objectFieldChangedByOffset(Object obj, long fieldOffset, Object update) {
    //
  }

  public final ObjectID lookupObjectIdFor(Object obj) {
    throw new UnsupportedOperationException();
  }

  public final Object arrayGet(Object array, int index) {
    if (array == null) throw new NullPointerException("array is null");

    if (array instanceof boolean[]) return ((boolean[]) array)[index] ? Boolean.TRUE : Boolean.FALSE;
    if (array instanceof byte[]) return new Byte(((byte[]) array)[index]);
    if (array instanceof char[]) return new Character(((char[]) array)[index]);
    if (array instanceof short[]) return new Short(((short[]) array)[index]);
    if (array instanceof int[]) return new Integer(((int[]) array)[index]);
    if (array instanceof long[]) return new Long(((long[]) array)[index]);
    if (array instanceof float[]) return new Float(((float[]) array)[index]);
    if (array instanceof double[]) return new Double(((double[]) array)[index]);

    if (array instanceof Object[]) { return ((Object[]) array)[index]; }

    throw new IllegalArgumentException("Not an array type: " + array.getClass().getName());
  }

  public final void arraycopy(Object src, int srcPos, Object dest, int destPos, int length) {
    System.arraycopy(src, srcPos, dest, destPos, length);
  }

  public final void byteOrBooleanArrayChanged(Object array, int index, byte b) {
    if (array == null) throw new NullPointerException("array is null");

    char type = array.getClass().getName().charAt(1);

    if (type == 'Z') {
      ((boolean[]) array)[index] = (b == 1);
    } else if (type == 'B') {
      ((byte[]) array)[index] = b;
    } else {
      throw new AssertionError(array.getClass().getName());
    }
  }

  public final void charArrayChanged(char[] array, int index, char c) {
    array[index] = c;
  }

  public final void charArrayCopy(char[] src, int srcPos, char[] dest, int destPos, int length) {
    System.arraycopy(src, srcPos, dest, destPos, length);
  }

  public final void doubleArrayChanged(double[] array, int index, double d) {
    array[index] = d;
  }

  public final void floatArrayChanged(float[] array, int index, float f) {
    array[index] = f;
  }

  public final void intArrayChanged(int[] array, int index, int i) {
    array[index] = i;
  }

  public final void longArrayChanged(long[] array, int index, long l) {
    array[index] = l;
  }

  public final void objectArrayChanged(Object[] array, int index, Object value) {
    array[index] = value;
  }

  public final void shortArrayChanged(short[] array, int index, short s) {
    array[index] = s;
  }
}
