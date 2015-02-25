package com.tc.object.bytecode;

import com.tc.cluster.DsoCluster;
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
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.statistics.StatisticRetrievalAction;
import com.terracottatech.search.NVPair;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;

import javax.management.MBeanServer;

public class NullManagerInternal implements ManagerInternal {
  private static final Manager NULL_MANAGER = NullManager.getInstance();

  @Override
  public void lock(LockID lock, LockLevel level) {
    NULL_MANAGER.lock(lock, level);
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level) {
    return NULL_MANAGER.tryLock(lock, level);
  }

  @Override
  public boolean tryLock(LockID lock, LockLevel level, long timeout) throws InterruptedException {
    return NULL_MANAGER.tryLock(lock, level, timeout);
  }

  @Override
  public void lockInterruptibly(LockID lock, LockLevel level) throws InterruptedException {
    NULL_MANAGER.lockInterruptibly(lock, level);
  }

  @Override
  public boolean isPhysicallyInstrumented(Class clazz) {
    return NULL_MANAGER.isPhysicallyInstrumented(clazz);
  }

  @Override
  public void unlock(LockID lock, LockLevel level) {
    NULL_MANAGER.unlock(lock, level);
  }

  @Override
  public void init() {
    NULL_MANAGER.init();
  }

  @Override
  public void initForTests() {
    NULL_MANAGER.initForTests();
  }

  @Override
  public void initForTests(CountDownLatch latch) {
    //
  }

  @Override
  public Notify notify(LockID lock, Object waitObject) {
    return NULL_MANAGER.notify(lock, waitObject);
  }

  @Override
  public void stop() {
    NULL_MANAGER.stop();
  }

  @Override
  public Object lookupOrCreateRoot(String name, Object object) {
    return NULL_MANAGER.lookupOrCreateRoot(name, object);
  }

  @Override
  public Notify notifyAll(LockID lock, Object waitObject) {
    return NULL_MANAGER.notifyAll(lock, waitObject);
  }

  @Override
  public Object lookupOrCreateRootNoDepth(String name, Object obj) {
    return NULL_MANAGER.lookupOrCreateRootNoDepth(name, obj);
  }

  @Override
  public void wait(LockID lock, Object waitObject) throws InterruptedException {
    NULL_MANAGER.wait(lock, waitObject);
  }

  @Override
  public Object createOrReplaceRoot(String rootName, Object object) {
    return NULL_MANAGER.createOrReplaceRoot(rootName, object);
  }

  @Override
  public Object lookupObject(ObjectID id) throws ClassNotFoundException {
    return NULL_MANAGER.lookupObject(id);
  }

  @Override
  public void wait(LockID lock, Object waitObject, long timeout) throws InterruptedException {
    NULL_MANAGER.wait(lock, waitObject, timeout);
  }

  @Override
  public void preFetchObject(ObjectID id) {
    NULL_MANAGER.preFetchObject(id);
  }

  @Override
  public boolean isLocked(LockID lock, LockLevel level) {
    return NULL_MANAGER.isLocked(lock, level);
  }

  @Override
  public Object lookupObject(ObjectID id, ObjectID parentContext) throws ClassNotFoundException {
    return NULL_MANAGER.lookupObject(id, parentContext);
  }

  @Override
  public boolean isLockedByCurrentThread(LockID lock, LockLevel level) {
    return NULL_MANAGER.isLockedByCurrentThread(lock, level);
  }

  @Override
  public TCObjectExternal lookupExistingOrNull(Object obj) {
    return NULL_MANAGER.lookupExistingOrNull(obj);
  }

  @Override
  public TCObjectExternal lookupOrCreate(Object obj) {
    return NULL_MANAGER.lookupOrCreate(obj);
  }

  @Override
  public boolean isLockedByCurrentThread(LockLevel level) {
    return NULL_MANAGER.isLockedByCurrentThread(level);
  }

  @Override
  public void logicalInvoke(Object object, String methodName, Object[] params) {
    NULL_MANAGER.logicalInvoke(object, methodName, params);
  }

  @Override
  public void logicalInvokeWithTransaction(Object object, Object lockObject, String methodName, Object[] params) {
    NULL_MANAGER.logicalInvokeWithTransaction(object, lockObject, methodName, params);
  }

  @Override
  public int localHoldCount(LockID lock, LockLevel level) {
    return NULL_MANAGER.localHoldCount(lock, level);
  }

  @Override
  public int globalHoldCount(LockID lock, LockLevel level) {
    return NULL_MANAGER.globalHoldCount(lock, level);
  }

  @Override
  public boolean distributedMethodCall(Object receiver, String method, Object[] params, boolean runOnAllNodes) {
    return NULL_MANAGER.distributedMethodCall(receiver, method, params, runOnAllNodes);
  }

  @Override
  public int globalPendingCount(LockID lock) {
    return NULL_MANAGER.globalPendingCount(lock);
  }

  @Override
  public void distributedMethodCallCommit() {
    NULL_MANAGER.distributedMethodCallCommit();
  }

  @Override
  public Object lookupRoot(String name) {
    return NULL_MANAGER.lookupRoot(name);
  }

  @Override
  public int globalWaitingCount(LockID lock) {
    return NULL_MANAGER.globalWaitingCount(lock);
  }

  @Override
  public void checkWriteAccess(Object context) {
    NULL_MANAGER.checkWriteAccess(context);
  }

  @Override
  public void pinLock(LockID lock) {
    NULL_MANAGER.pinLock(lock);
  }

  @Override
  public int calculateDsoHashCode(Object obj) {
    return NULL_MANAGER.calculateDsoHashCode(obj);
  }

  @Override
  public void unpinLock(LockID lock) {
    NULL_MANAGER.unpinLock(lock);
  }

  @Override
  public LockID generateLockIdentifier(String str) {
    return NULL_MANAGER.generateLockIdentifier(str);
  }

  @Override
  public LockID generateLockIdentifier(Object obj) {
    return NULL_MANAGER.generateLockIdentifier(obj);
  }

  @Override
  public LockID generateLockIdentifier(Object obj, String field) {
    return NULL_MANAGER.generateLockIdentifier(obj, field);
  }

  @Override
  public boolean isLiteralInstance(Object obj) {
    return NULL_MANAGER.isLiteralInstance(obj);
  }

  @Override
  public boolean isManaged(Object object) {
    return NULL_MANAGER.isManaged(object);
  }

  @Override
  public boolean isLiteralAutolock(Object o) {
    return NULL_MANAGER.isLiteralAutolock(o);
  }

  @Override
  public boolean isDsoMonitored(Object obj) {
    return NULL_MANAGER.isDsoMonitored(obj);
  }

  @Override
  public boolean isDsoMonitorEntered(Object obj) {
    return NULL_MANAGER.isDsoMonitorEntered(obj);
  }

  @Override
  public Object getChangeApplicator(Class clazz) {
    return NULL_MANAGER.getChangeApplicator(clazz);
  }

  @Override
  public boolean isLogical(Object object) {
    return NULL_MANAGER.isLogical(object);
  }

  @Override
  public boolean isRoot(Field field) {
    return NULL_MANAGER.isRoot(field);
  }

  @Override
  public String getClientID() {
    return NULL_MANAGER.getClientID();
  }

  @Override
  public String getUUID() {
    return NULL_MANAGER.getUUID();
  }

  @Override
  public TCLogger getLogger(String loggerName) {
    return NULL_MANAGER.getLogger(loggerName);
  }

  @Override
  public InstrumentationLogger getInstrumentationLogger() {
    return NULL_MANAGER.getInstrumentationLogger();
  }

  @Override
  public TCProperties getTCProperties() {
    return NULL_MANAGER.getTCProperties();
  }

  @Override
  public boolean isFieldPortableByOffset(Object pojo, long fieldOffset) {
    return NULL_MANAGER.isFieldPortableByOffset(pojo, fieldOffset);
  }

  @Override
  public boolean overridesHashCode(Object obj) {
    return NULL_MANAGER.overridesHashCode(obj);
  }

  @Override
  public void registerNamedLoader(NamedClassLoader loader, String webAppName) {
    NULL_MANAGER.registerNamedLoader(loader, webAppName);
  }

  @Override
  public ClassProvider getClassProvider() {
    return NULL_MANAGER.getClassProvider();
  }

  @Override
  public TunneledDomainUpdater getTunneledDomainUpdater() {
    return NULL_MANAGER.getTunneledDomainUpdater();
  }

  @Override
  public DsoCluster getDsoCluster() {
    return NULL_MANAGER.getDsoCluster();
  }

  @Override
  public MBeanServer getMBeanServer() {
    return NULL_MANAGER.getMBeanServer();
  }

  @Override
  public StatisticRetrievalAction getStatisticRetrievalActionInstance(String name) {
    return NULL_MANAGER.getStatisticRetrievalActionInstance(name);
  }

  @Override
  public void registerStatisticRetrievalAction(StatisticRetrievalAction sra) {
    NULL_MANAGER.registerStatisticRetrievalAction(sra);
  }

  @Override
  public void monitorEnter(LockID lock, LockLevel level) {
    NULL_MANAGER.monitorEnter(lock, level);
  }

  @Override
  public void monitorExit(LockID lock, LockLevel level) {
    NULL_MANAGER.monitorExit(lock, level);
  }

  @Override
  public SessionConfiguration getSessionConfiguration(String appName) {
    return NULL_MANAGER.getSessionConfiguration(appName);
  }

  @Override
  public void waitForAllCurrentTransactionsToComplete() {
    NULL_MANAGER.waitForAllCurrentTransactionsToComplete();
  }

  @Override
  public void registerBeforeShutdownHook(Runnable beforeShutdownHook) {
    NULL_MANAGER.registerBeforeShutdownHook(beforeShutdownHook);
  }

  @Override
  public LockID generateLockIdentifier(long lockId) {
    return UnclusteredLockID.UNCLUSTERED_LOCK_ID;
  }

  @Override
  public MetaDataDescriptor createMetaDataDescriptor(String category) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn) {
    throw new UnsupportedOperationException();
  }

  @Override
  public NVPair createNVPair(String name, Object value) {
    throw new UnsupportedOperationException();
  }

  @Override
  public void verifyCapability(String capability) {
    // do nothing
  }

  @Override
  public void fireOperatorEvent(EventType eventLevel, EventSubsystem eventSubsystem, String eventMessage) {
    //
  }

  @Override
  public void stopImmediate() {
    //
  }

  public void addTransactionCompleteListener(TransactionCompleteListener listener) {
    //
  }

  public void skipBroadcastForCurrentTransaction(ObjectID objectID) {
    //
  }
}
