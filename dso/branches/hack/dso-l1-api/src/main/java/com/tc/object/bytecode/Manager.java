/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortedOperationException;
import com.tc.cluster.DsoCluster;
import com.tc.logging.TCLogger;
import com.tc.management.TCManagementEvent;
import com.tc.management.TunneledDomainUpdater;
import com.tc.net.ClientID;
import com.tc.net.GroupID;
import com.tc.object.LogicalOperation;
import com.tc.object.ObjectID;
import com.tc.object.ServerEventDestination;
import com.tc.object.TCObject;
import com.tc.object.locks.LockID;
import com.tc.object.locks.LockLevel;
import com.tc.object.locks.TerracottaLocking;
import com.tc.object.metadata.MetaDataDescriptor;
import com.tc.object.tx.TransactionCompleteListener;
import com.tc.operatorevent.TerracottaOperatorEvent.EventLevel;
import com.tc.operatorevent.TerracottaOperatorEvent.EventSubsystem;
import com.tc.operatorevent.TerracottaOperatorEvent.EventType;
import com.tc.platform.PlatformService;
import com.tc.properties.TCProperties;
import com.tc.search.SearchQueryResults;
import com.tc.search.SearchRequestID;
import com.tc.server.ServerEventType;
import com.tc.util.concurrent.TaskRunner;
import com.terracottatech.search.NVPair;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;

import javax.management.MBeanServer;

/**
 * The Manager interface
 */
public interface Manager extends TerracottaLocking {

  /**
   * Initialize the Manager
   */
  public void init();

  /**
   * Stop the manager
   */
  public void stop();

  /**
   * Look up or create a new root object in the particular group
   * 
   * @param name Root name
   * @param object Root object to use if none exists yet
   * @param gid group id
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupOrCreateRoot(final String name, final Object object, GroupID gid);

  /**
   * Look up a new root object in the particular group
   * 
   * @param name Root name
   * @param gid group id
   * @return The root object actually used, may or may not == object
   * @throws AbortedOperationException
   */
  public Object lookupRoot(final String name, GroupID gid);

  /**
   * Look up object by ID, faulting into the JVM if necessary
   * 
   * @param id Object identifier
   * @return The actual object
   * @throws AbortedOperationException
   */
  public Object lookupObject(ObjectID id) throws ClassNotFoundException, AbortedOperationException;

  /**
   * Prefetch object by ID, faulting into the JVM if necessary, Async lookup and will not cause ObjectNotFoundException
   * like lookupObject. Non-existent objects are ignored by the server.
   * 
   * @param id Object identifier
   * @throws AbortedOperationException
   */
  public void preFetchObject(ObjectID id) throws AbortedOperationException;

  /**
   * Find managed object, which may be null
   * 
   * @param obj The object instance
   * @return The TCObject
   */
  public TCObject lookupExistingOrNull(Object obj);

  public TCObject lookupOrCreate(Object obj, GroupID gid);

  /**
   * Perform invoke on logical managed object
   * 
   * @param object The object
   * @param methodName The method to call
   * @param params The parameters to the method
   */
  public void logicalInvoke(Object object, LogicalOperation method, Object[] params);

  /**
   * @return true if obj is an instance of a {@link com.tc.object.LiteralValues literal type} and is suitable for
   *         cluster-wide locking,
   */
  public boolean isLiteralAutolock(final Object o);

  /**
   * Get JVM Client identifier
   * 
   * @return Client identifier
   */
  public ClientID getClientID();

  /**
   * Get unique Client identifier
   * 
   * @return unique Client identifier
   */
  public String getUUID();

  /**
   * Get the named logger
   * 
   * @param loggerName Logger name
   * @return The logger
   */
  public TCLogger getLogger(String loggerName);

  /**
   * @return TCProperties
   */
  public TCProperties getTCProperties();

  /**
   * Get the TunneledDomainUpdater associated with this Manager
   */
  public TunneledDomainUpdater getTunneledDomainUpdater();

  /**
   * Retrieves the DSO cluster instance.
   * 
   * @return the DSO cluster instance for this manager
   */
  public DsoCluster getDsoCluster();

  /**
   * Retrieves the MBean server that's used by this Terracotta client
   * 
   * @return the MBean server for this client
   */
  public MBeanServer getMBeanServer();

  /**
   * Used by BulkLoad to wait for all current transactions completed
   * 
   * @throws AbortedOperationException
   */
  public void waitForAllCurrentTransactionsToComplete() throws AbortedOperationException;

  /**
   * Registers a hook that will be called before shutting down this client
   */
  public void registerBeforeShutdownHook(Runnable beforeShutdownHook);

  public void unregisterBeforeShutdownHook(Runnable beforeShutdownHook);

  MetaDataDescriptor createMetaDataDescriptor(String category);

  public SearchQueryResults executeQuery(String cachename, List queryStack, boolean includeKeys, boolean includeValues,
                                         Set<String> attributeSet, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, int resultPageSize,
                                         boolean waitForTxn, SearchRequestID reqId) throws AbortedOperationException;

  public SearchQueryResults executeQuery(String cachename, List queryStack, Set<String> attributeSet,
                                         Set<String> groupByAttribues, List<NVPair> sortAttributes,
                                         List<NVPair> aggregators, int maxResults, int batchSize, boolean waitForTxn,
                                         SearchRequestID reqId) throws AbortedOperationException;

  public NVPair createNVPair(String name, Object value);

  void verifyCapability(String capability);

  void fireOperatorEvent(EventLevel eventLevel, EventSubsystem subsystem, EventType eventType, String eventMessage);

  void stopImmediate();

  void initForTests();

  public GroupID[] getGroupIDs();

  void lockIDWait(final LockID lock, final long timeout) throws InterruptedException, AbortedOperationException;

  void lockIDNotifyAll(final LockID lock) throws AbortedOperationException;

  void lockIDNotify(final LockID lock) throws AbortedOperationException;

  /**
   * Register an object with given name if null is mapped currently to the name. Otherwise returns old mapped object.
   * 
   * @param name Name to use for registering the object
   * @param object Object to register
   * @return the previous value associated with the specified name, or same 'object' if there was no mapping for the
   *         name
   */
  <T> T registerObjectByNameIfAbsent(String name, T object);

  /**
   * Lookup and return an already registered object by name if it exists, otherwise null.
   * 
   * @return lookup and return an already registered object by name if it exists, otherwise null
   * @throws ClassCastException if a mapping exists for name, but is of different type other than expectedType
   */
  <T> T lookupRegisteredObjectByName(String name, Class<T> expectedType);

  void addTransactionCompleteListener(TransactionCompleteListener listener);

  AbortableOperationManager getAbortableOperationManager();

  PlatformService getPlatformService();

  void throttlePutIfNecessary(ObjectID object) throws AbortedOperationException;

  void beginAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException;

  void commitAtomicTransaction(LockID lock, LockLevel level) throws AbortedOperationException;

  void registerServerEventListener(ServerEventDestination destination, Set<ServerEventType> listenTo);

  void unregisterServerEventListener(ServerEventDestination destination, final Set<ServerEventType> listenTo);

  int getRejoinCount();

  boolean isRejoinInProgress();

  TaskRunner getTaskRunner();

  public long getLockAwardIDFor(LockID lock);

  public boolean isLockAwardValid(LockID lock, long awardID);

  Object registerManagementService(Object service, ExecutorService executorService);

  void unregisterManagementService(Object serviceID);

  void sendEvent(TCManagementEvent event);
}
