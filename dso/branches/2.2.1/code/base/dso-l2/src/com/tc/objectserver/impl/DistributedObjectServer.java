/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.impl;

import bsh.EvalError;
import bsh.Interpreter;

import com.sleepycat.je.DatabaseException;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.async.impl.NullSink;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.exception.TCRuntimeException;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.io.TCRandomFileAccessImpl;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.L2Management;
import com.tc.management.beans.TCServerInfoMBean;
import com.tc.management.remote.connect.ClientConnectEventHandler;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.net.NIOWorkarounds;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.NetworkListener;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.cache.EvictionPolicy;
import com.tc.object.cache.LFUConfigImpl;
import com.tc.object.cache.LFUEvictionPolicy;
import com.tc.object.cache.LRUEvictionPolicy;
import com.tc.object.cache.NullCache;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.object.config.schema.PersistenceMode;
import com.tc.object.msg.AcknowledgeTransactionMessageImpl;
import com.tc.object.msg.BatchTransactionAcknowledgeMessageImpl;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.MessageRecycler;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.msg.RequestRootMessageImpl;
import com.tc.object.msg.RequestRootResponseMessage;
import com.tc.object.net.ChannelStats;
import com.tc.object.net.ChannelStatsImpl;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.DSOChannelManagerImpl;
import com.tc.object.net.DSOChannelManagerMBean;
import com.tc.object.session.NullSessionManager;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.objectserver.DSOApplicationEvents;
import com.tc.objectserver.api.ObjectManagerMBean;
import com.tc.objectserver.api.ObjectRequestManager;
import com.tc.objectserver.core.api.DSOGlobalServerStats;
import com.tc.objectserver.core.api.DSOGlobalServerStatsImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.MarkAndSweepGarbageCollector;
import com.tc.objectserver.core.impl.ServerConfigurationContextImpl;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.gtx.ServerGlobalTransactionManager;
import com.tc.objectserver.gtx.ServerGlobalTransactionManagerImpl;
import com.tc.objectserver.handler.ApplyTransactionChangeHandler;
import com.tc.objectserver.handler.TransactionLookupHandler;
import com.tc.objectserver.handler.BroadcastChangeHandler;
import com.tc.objectserver.handler.ChannelLifeCycleHandler;
import com.tc.objectserver.handler.ClientHandshakeHandler;
import com.tc.objectserver.handler.CommitTransactionChangeHandler;
import com.tc.objectserver.handler.JMXEventsHandler;
import com.tc.objectserver.handler.ManagedObjectFaultHandler;
import com.tc.objectserver.handler.ManagedObjectFlushHandler;
import com.tc.objectserver.handler.ManagedObjectRequestHandler;
import com.tc.objectserver.handler.ProcessTransactionHandler;
import com.tc.objectserver.handler.RequestLockUnLockHandler;
import com.tc.objectserver.handler.RequestObjectIDBatchHandler;
import com.tc.objectserver.handler.RequestRootHandler;
import com.tc.objectserver.handler.RespondToObjectRequestHandler;
import com.tc.objectserver.handler.RespondToRequestLockHandler;
import com.tc.objectserver.handler.TransactionAcknowledgementHandler;
import com.tc.objectserver.handshakemanager.ServerClientHandshakeManager;
import com.tc.objectserver.l1.api.ClientStateManager;
import com.tc.objectserver.l1.impl.ClientStateManagerImpl;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeAction;
import com.tc.objectserver.l1.impl.TransactionAcknowledgeActionImpl;
import com.tc.objectserver.lockmanager.api.LockManager;
import com.tc.objectserver.lockmanager.api.LockManagerMBean;
import com.tc.objectserver.lockmanager.impl.LockManagerImpl;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProviderImpl;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.ManagedObjectStore;
import com.tc.objectserver.persistence.api.PersistenceTransactionProvider;
import com.tc.objectserver.persistence.api.Persistor;
import com.tc.objectserver.persistence.api.TransactionPersistor;
import com.tc.objectserver.persistence.api.TransactionStore;
import com.tc.objectserver.persistence.impl.InMemoryPersistor;
import com.tc.objectserver.persistence.impl.NullPersistenceTransactionProvider;
import com.tc.objectserver.persistence.impl.NullTransactionPersistor;
import com.tc.objectserver.persistence.impl.PersistentBatchSequenceProvider;
import com.tc.objectserver.persistence.impl.TransactionStoreImpl;
import com.tc.objectserver.persistence.sleepycat.CustomSerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.DBEnvironment;
import com.tc.objectserver.persistence.sleepycat.DBException;
import com.tc.objectserver.persistence.sleepycat.SerializationAdapterFactory;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.tx.CommitTransactionMessageRecycler;
import com.tc.objectserver.tx.CommitTransactionMessageToTransactionBatchReader;
import com.tc.objectserver.tx.ServerTransactionManager;
import com.tc.objectserver.tx.ServerTransactionManagerImpl;
import com.tc.objectserver.tx.ServerTransactionManagerMBean;
import com.tc.objectserver.tx.TransactionBatchManager;
import com.tc.objectserver.tx.TransactionBatchManagerImpl;
import com.tc.objectserver.tx.TransactionSequencer;
import com.tc.objectserver.tx.TransactionalObjectManagerImpl;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.stats.counter.sampled.SampledCounterManager;
import com.tc.stats.counter.sampled.SampledCounterManagerImpl;
import com.tc.util.SequenceValidator;
import com.tc.util.StartupLock;
import com.tc.util.TCTimeoutException;
import com.tc.util.TCTimerImpl;
import com.tc.util.io.FileUtils;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.ObjectIDSequence;
import com.tc.util.sequence.ObjectIDSequenceProvider;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;
import com.tc.util.startuplock.FileNotCreatedException;
import com.tc.util.startuplock.LocationNotCreatedException;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

/**
 * Startup and shutdown point. Builds and starts the server. This is a quick and dirty dirty way of doing this stuff
 * 
 * @author steve
 */
public class DistributedObjectServer extends SEDA {
  private final ConnectionPolicy               connectionPolicy;

  private static final TCLogger                logger        = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                consoleLogger = CustomerLogging.getConsoleLogger();

  private final L2TVSConfigurationSetupManager configSetupManager;
  private final Sink                           httpSink;
  private NetworkListener                      lsnr;
  private CommunicationsManager                communicationsManager;
  private ServerConfigurationContext           context;
  private ObjectManagerImpl                    objectManager;
  private SampledCounterManager                sampledCounterManager;
  private LockManager                          lockManager;
  private ServerManagementContext              managementContext;
  private StartupLock                          startupLock;

  private ClientStateManagerImpl               clientStateManager;

  private ManagedObjectStore                   objectStore;
  private Persistor                            persistor;
  private ServerTransactionManager             transactionManager;

  private CacheManager                         cacheManager;

  private final TCServerInfoMBean              tcServerInfoMBean;
  private L2Management                         l2Management;

  private TCProperties                         l2Properties;

  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, TCServerInfoMBean tcServerInfoMBean) {
    this(configSetupManager, threadGroup, connectionPolicy, new NullSink(), tcServerInfoMBean);
  }

  public DistributedObjectServer(L2TVSConfigurationSetupManager configSetupManager, TCThreadGroup threadGroup,
                                 ConnectionPolicy connectionPolicy, Sink httpSink, TCServerInfoMBean tcServerInfoMBean) {
    super(threadGroup);
    this.configSetupManager = configSetupManager;
    this.connectionPolicy = connectionPolicy;
    this.httpSink = httpSink;
    this.tcServerInfoMBean = tcServerInfoMBean;
  }

  public void dump() {
    if (this.lockManager != null) {
      this.lockManager.dump();
    }

    if (this.objectManager != null) {
      this.objectManager.dump();
    }

    if (this.transactionManager != null) {
      this.transactionManager.dump();
    }
  }

  public synchronized void start() throws IOException, DatabaseException, LocationNotCreatedException,
      FileNotCreatedException {

    try {
      startJMXServer();
    } catch (Throwable t) {
      logger.error("Error starting jmx server", t);
    }

    NIOWorkarounds.solaris10Workaround();

    this.configSetupManager.commonl2Config().changesInItemIgnored(this.configSetupManager.commonl2Config().dataPath());
    NewL2DSOConfig l2DSOConfig = this.configSetupManager.dsoL2Config();
    l2DSOConfig.changesInItemIgnored(l2DSOConfig.persistenceMode());
    PersistenceMode persistenceMode = (PersistenceMode) l2DSOConfig.persistenceMode().getObject();

    final boolean swapEnabled = true; // 2006-01-31 andrew -- no longer possible to use in-memory only; DSO folks say
    // it's broken
    final boolean persistent = persistenceMode.equals(PersistenceMode.PERMANENT_STORE);

    TCFile location = new TCFileImpl(this.configSetupManager.commonl2Config().dataPath().getFile());
    this.startupLock = new StartupLock(location);

    if (!startupLock.canProceed(new TCRandomFileAccessImpl(), persistent)) {
      consoleLogger.error("Another L2 process is using the directory " + location + " as data directory.");
      if (!persistent) {
        consoleLogger.error("This is not allowed with persistence mode set to temporary-swap-only.");
      }
      consoleLogger.error("Exiting...");
      System.exit(1);
    }

    int maxStageSize = 5000;

    StageManager stageManager = getStageManager();
    SessionManager sessionManager = new NullSessionManager();
    SessionProvider sessionProvider = (SessionProvider) sessionManager;
    l2Properties = TCPropertiesImpl.getProperties().getPropertiesFor("l2");

    EvictionPolicy swapCache;
    final ClientStatePersistor clientStateStore;
    final PersistenceTransactionProvider persistenceTransactionProvider;
    final TransactionPersistor transactionPersistor;
    final Sequence globalTransactionIDSequence;
    logger.debug("server swap enabled: " + swapEnabled);
    final ManagedObjectChangeListenerProviderImpl managedObjectChangeListenerProvider = new ManagedObjectChangeListenerProviderImpl();
    if (swapEnabled) {
      File dbhome = new File(this.configSetupManager.commonl2Config().dataPath().getFile(), "objectdb");

      logger.debug("persistent: " + persistent);

      if (!persistent) {
        if (dbhome.exists()) {
          logger.debug("deleting persistence database...");
          FileUtils.forceDelete(dbhome, "jdb");
          logger.debug("persistence database deleted.");
        }
      }
      logger.debug("persistence database home: " + dbhome);

      DBEnvironment dbenv = new DBEnvironment(persistent, dbhome);
      SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
      persistor = new SleepycatPersistor(TCLogging.getLogger(SleepycatPersistor.class), dbenv,
                                         serializationAdapterFactory);

      String cachePolicy = l2Properties.getProperty("objectmanager.cachePolicy").toUpperCase();
      if (cachePolicy.equals("LRU")) {
        swapCache = new LRUEvictionPolicy(-1);
      } else if (cachePolicy.equals("LFU")) {
        swapCache = new LFUEvictionPolicy(-1, new LFUConfigImpl(l2Properties.getPropertiesFor("lfu")));
      } else {
        throw new AssertionError("Unknown Cache Policy : " + cachePolicy
                                 + " Accepted Values are : <LRU>/<LFU> Please check tc.properties");
      }
      objectStore = new PersistentManagedObjectStore(persistor.getManagedObjectPersistor());
    } else {
      persistor = new InMemoryPersistor();
      swapCache = new NullCache();
      objectStore = new InMemoryManagedObjectStore(new HashMap());
    }

    persistenceTransactionProvider = persistor.getPersistenceTransactionProvider();
    PersistenceTransactionProvider nullPersistenceTransactionProvider = new NullPersistenceTransactionProvider();
    PersistenceTransactionProvider transactionStorePTP;
    ObjectIDSequence objectIDSequenceProvider;
    if (persistent) {
      // XXX: This construction/initialization order is pretty lame. Perhaps
      // making the sequence provider its own
      // handler isn't the right thing to do.
      PersistentBatchSequenceProvider sequenceProvider = new PersistentBatchSequenceProvider(persistor
          .getGlobalTransactionIDSequence());
      Stage requestBatchStage = stageManager
          .createStage(ServerConfigurationContext.REQUEST_BATCH_GLOBAL_TRANSACTION_ID_SEQUENCE_STAGE, sequenceProvider,
                       1, maxStageSize);
      sequenceProvider.setRequestBatchSink(requestBatchStage.getSink());
      globalTransactionIDSequence = new BatchSequence(sequenceProvider, 1000);

      transactionPersistor = persistor.getTransactionPersistor();
      transactionStorePTP = persistenceTransactionProvider;
      objectIDSequenceProvider = objectStore;
    } else {
      transactionPersistor = new NullTransactionPersistor();
      transactionStorePTP = nullPersistenceTransactionProvider;
      globalTransactionIDSequence = new SimpleSequence();
      objectIDSequenceProvider = new ObjectIDSequenceProvider(1000);
    }

    clientStateStore = persistor.getClientStatePersistor();

    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);

    this.communicationsManager = new CommunicationsManagerImpl(new NullMessageMonitor(),
                                                               new PlainNetworkStackHarnessFactory(),
                                                               this.connectionPolicy);

    final DSOApplicationEvents appEvents;
    try {
      appEvents = new DSOApplicationEvents();
    } catch (NotCompliantMBeanException ncmbe) {
      throw new TCRuntimeException("Unable to construct the " + DSOApplicationEvents.class.getName()
                                   + " MBean; this is a programming error. Please go fix that class.", ncmbe);
    }

    clientStateManager = new ClientStateManagerImpl(TCLogging.getLogger(ClientStateManager.class), clientStateStore);
    final Set previouslyConnectedClients = new HashSet(clientStateManager.getAllClientIDs());

    Set initialConnectionIDs = clientStateStore.loadConnectionIDs();

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionEnabled());
    boolean gcEnabled = l2DSOConfig.garbageCollectionEnabled().getBoolean();
    logger.debug("GC enabled: " + gcEnabled);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionInterval());
    long gcInterval = l2DSOConfig.garbageCollectionInterval().getInt();
    if (gcEnabled) logger.debug("GC interval: " + gcInterval + " seconds");

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.garbageCollectionVerbose());
    boolean verboseGC = l2DSOConfig.garbageCollectionVerbose().getBoolean();
    if (gcEnabled) logger.debug("Verbose GC enabled: " + verboseGC);
    sampledCounterManager = new SampledCounterManagerImpl();
    SampledCounter objectCreationRate = sampledCounterManager.createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter objectFaultRate = sampledCounterManager.createCounter(new SampledCounterConfig(1, 900, true, 0L));
    ObjectManagerStatsImpl objMgrStats = new ObjectManagerStatsImpl(objectCreationRate, objectFaultRate);

    SequenceValidator sequenceValidator = new SequenceValidator(0);
    ManagedObjectFaultHandler managedObjectFaultHandler = new ManagedObjectFaultHandler();
    // Server initiated request processing queues shouldn't have any max queue size.
    Stage faultManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FAULT_STAGE,
                                                             managedObjectFaultHandler, 4, -1);
    ManagedObjectFlushHandler managedObjectFlushHandler = new ManagedObjectFlushHandler();
    Stage flushManagedObjectStage = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_FLUSH_STAGE,
                                                             managedObjectFlushHandler, (persistent ? 1 : 4), -1);

    TCProperties objManagerProperties = l2Properties.getPropertiesFor("objectmanager");

    objectManager = new ObjectManagerImpl(new ObjectManagerConfig(gcInterval * 1000, gcEnabled, verboseGC, persistent,
                                                                  objManagerProperties.getInt("deleteBatchSize")),
                                          getThreadGroup(), clientStateManager, objectStore, swapCache,
                                          persistenceTransactionProvider, faultManagedObjectStage.getSink(),
                                          flushManagedObjectStage.getSink(), l2Management
                                              .findObjectManagementMonitorMBean());
    objectManager.setStatsListener(objMgrStats);
    objectManager.setGarbageCollector(new MarkAndSweepGarbageCollector(objectManager, clientStateManager, verboseGC));
    managedObjectChangeListenerProvider.setListener(objectManager);

    TCProperties cacheManagerProperties = l2Properties.getPropertiesFor("cachemanager");
    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(objectManager, new CacheConfigImpl(cacheManagerProperties));
      if (logger.isDebugEnabled()) {
        logger.debug("CacheManager Enabled : " + cacheManager);
      }
    } else {
      logger.warn("CacheManager is Disabled");
    }

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.listenPort());
    int serverPort = l2DSOConfig.listenPort().getInt();
    lsnr = communicationsManager.createListener(sessionProvider, new TCSocketAddress(TCSocketAddress.WILDCARD_ADDR,
                                                                                     serverPort), true,
                                                initialConnectionIDs, clientStateStore.getConnectionIDFactory(),
                                                httpSink);

    ClientTunnelingEventHandler cteh = new ClientTunnelingEventHandler();
    lsnr.getChannelManager().addEventListener(cteh);

    DSOChannelManager channelManager = new DSOChannelManagerImpl(lsnr.getChannelManager());

    ChannelStats channelStats = new ChannelStatsImpl(sampledCounterManager, channelManager);

    lockManager = new LockManagerImpl(channelManager);
    TransactionAcknowledgeAction taa = new TransactionAcknowledgeActionImpl(channelManager);
    ObjectInstanceMonitorImpl instanceMonitor = new ObjectInstanceMonitorImpl();
    TransactionBatchManager transactionBatchManager = new TransactionBatchManagerImpl();
    SampledCounter globalTxnCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300, true, 0L));

    final TransactionStore transactionStore = new TransactionStoreImpl(transactionPersistor,
                                                                       globalTransactionIDSequence);
    ServerGlobalTransactionManager gtxm = new ServerGlobalTransactionManagerImpl(sequenceValidator, transactionStore,
                                                                                 transactionStorePTP);
    transactionManager = new ServerTransactionManagerImpl(gtxm, transactionStore, lockManager, clientStateManager,
                                                          objectManager, taa, globalTxnCounter, channelStats);
    MessageRecycler recycler = new CommitTransactionMessageRecycler(transactionManager);

    Stage batchTxLookupStage = stageManager.createStage(ServerConfigurationContext.TRANSACTION_LOOKUP_STAGE,
                                                        new TransactionLookupHandler(), 1, maxStageSize);
    TransactionalObjectManagerImpl txnObjectManager = new TransactionalObjectManagerImpl(objectManager,
                                                                                         new TransactionSequencer(),
                                                                                         gtxm, batchTxLookupStage
                                                                                             .getSink());
    Stage processTx = stageManager.createStage(ServerConfigurationContext.PROCESS_TRANSACTION_STAGE,
                                               new ProcessTransactionHandler(transactionBatchManager, txnObjectManager,
                                                                             sequenceValidator, recycler), 1,
                                               maxStageSize);

    Stage rootRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_ROOT_REQUEST_STAGE,
                                                 new RequestRootHandler(), 1, maxStageSize);

    // Lookup stage should never be blocked trying to add to apply stage
    stageManager.createStage(ServerConfigurationContext.APPLY_CHANGES_STAGE,
                             new ApplyTransactionChangeHandler(instanceMonitor, gtxm), 1, -1);
    stageManager.createStage(ServerConfigurationContext.COMMIT_CHANGES_STAGE,
                             new CommitTransactionChangeHandler(gtxm, transactionStorePTP), (persistent ? 4 : 1),
                             maxStageSize);
    stageManager.createStage(ServerConfigurationContext.BROADCAST_CHANGES_STAGE,
                             new BroadcastChangeHandler(transactionBatchManager), 1, maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE,
                             new RespondToRequestLockHandler(), 1, maxStageSize);
    Stage requestLock = stageManager.createStage(ServerConfigurationContext.REQUEST_LOCK_STAGE,
                                                 new RequestLockUnLockHandler(), 1, maxStageSize);
    stageManager.createStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE,
                             new ChannelLifeCycleHandler(communicationsManager, transactionManager,
                                                         transactionBatchManager), 1, maxStageSize);
    SampledCounter globalObjectFaultCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300,
                                                                                                           true, 0L));
    SampledCounter globalObjectFlushCounter = sampledCounterManager.createCounter(new SampledCounterConfig(1, 300,
                                                                                                           true, 0L));
    Stage objectRequest = stageManager.createStage(ServerConfigurationContext.MANAGED_OBJECT_REQUEST_STAGE,
                                                   new ManagedObjectRequestHandler(globalObjectFaultCounter,
                                                                                   globalObjectFlushCounter), 1,
                                                   maxStageSize);
    stageManager.createStage(ServerConfigurationContext.RESPOND_TO_OBJECT_REQUEST_STAGE,
                             new RespondToObjectRequestHandler(), 4, maxStageSize);
    Stage oidRequest = stageManager.createStage(ServerConfigurationContext.OBJECT_ID_BATCH_REQUEST_STAGE,
                                                new RequestObjectIDBatchHandler(objectIDSequenceProvider), 1,
                                                maxStageSize);
    Stage transactionAck = stageManager.createStage(ServerConfigurationContext.TRANSACTION_ACKNOWLEDGEMENT_STAGE,
                                                    new TransactionAcknowledgementHandler(), 1, maxStageSize);
    Stage clientHandshake = stageManager.createStage(ServerConfigurationContext.CLIENT_HANDSHAKE_STAGE,
                                                     new ClientHandshakeHandler(), 1, maxStageSize);
    Stage hydrateStage = stageManager.createStage(ServerConfigurationContext.HYDRATE_MESSAGE_SINK,
                                                  new HydrateHandler(), 1, maxStageSize);

    Stage jmxEventsStage = stageManager.createStage(ServerConfigurationContext.JMX_EVENTS_STAGE,
                                                    new JMXEventsHandler(appEvents), 1, maxStageSize);

    final Stage jmxRemoteConnectStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_CONNECT_STAGE,
                                                                 new ClientConnectEventHandler(), 1, maxStageSize);
    cteh.setConnectStageSink(jmxRemoteConnectStage.getSink());
    final Stage jmxRemoteTunnelStage = stageManager.createStage(ServerConfigurationContext.JMXREMOTE_TUNNEL_STAGE,
                                                                cteh, 1, maxStageSize);

    lsnr.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, BatchTransactionAcknowledgeMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    lsnr.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                         RequestManagedObjectResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    lsnr.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                         ObjectIDBatchRequestResponseMessage.class);
    lsnr.addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    lsnr.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    lsnr.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);

    Sink hydrateSink = hydrateStage.getSink();
    lsnr.routeMessageType(TCMessageType.COMMIT_TRANSACTION_MESSAGE, processTx.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.LOCK_REQUEST_MESSAGE, requestLock.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.REQUEST_ROOT_MESSAGE, rootRequest.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, objectRequest.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, oidRequest.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionAck.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, clientHandshake.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.JMX_MESSAGE, jmxEventsStage.getSink(), hydrateSink);
    lsnr.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                          hydrateSink);

    Sink stateChangeSink = stageManager.getStage(ServerConfigurationContext.CHANNEL_LIFE_CYCLE_STAGE).getSink();
    lsnr.getChannelManager().routeChannelStateChanges(stateChangeSink);

    ObjectRequestManager objectRequestManager = new ObjectRequestManagerImpl(objectManager, transactionManager);

    l2DSOConfig.changesInItemIgnored(l2DSOConfig.clientReconnectWindow());
    long reconnectTimeout = l2DSOConfig.clientReconnectWindow().getInt();
    logger.debug("Client Reconnect Window: " + reconnectTimeout + " seconds");
    reconnectTimeout *= 1000;
    ServerClientHandshakeManager clientHandshakeManager = new ServerClientHandshakeManager(
                                                                                           TCLogging
                                                                                               .getLogger(ServerClientHandshakeManager.class),
                                                                                           channelManager,
                                                                                           objectManager,
                                                                                           sequenceValidator,
                                                                                           clientStateManager,
                                                                                           previouslyConnectedClients,
                                                                                           lockManager,
                                                                                           transactionManager,
                                                                                           stageManager
                                                                                               .getStage(
                                                                                                         ServerConfigurationContext.RESPOND_TO_LOCK_REQUEST_STAGE)
                                                                                               .getSink(),
                                                                                           objectIDSequenceProvider,
                                                                                           new TCTimerImpl(
                                                                                                           "Reconnect timer",
                                                                                                           true),
                                                                                           reconnectTimeout, persistent);
    context = new ServerConfigurationContextImpl(stageManager, objectManager, objectRequestManager, objectStore,
                                                 lockManager, channelManager, clientStateManager, transactionManager,
                                                 txnObjectManager, clientHandshakeManager, channelStats,
                                                 new CommitTransactionMessageToTransactionBatchReader());

    stageManager.startAll(context);

    lsnr.start();

    DSOGlobalServerStats serverStats = new DSOGlobalServerStatsImpl(globalObjectFlushCounter, globalObjectFaultCounter,
                                                                    globalTxnCounter, objMgrStats);

    // XXX: yucky casts
    managementContext = new ServerManagementContext((ServerTransactionManagerMBean) transactionManager,
                                                    (ObjectManagerMBean) objectManager, (LockManagerMBean) lockManager,
                                                    (DSOChannelManagerMBean) channelManager, serverStats, channelStats,
                                                    instanceMonitor, appEvents);

    consoleLogger.info("DSO Server started on port " + lsnr.getBindPort() + ".");
    if (l2Properties.getBoolean("beanshell.enabled")) startBeanShell(l2Properties.getInt("beanshell.port"));
  }

  private void startBeanShell(int port) {
    try {
      Interpreter i = new Interpreter();
      i.set("dsoserver", this);
      i.set("objectmanager", objectManager);
      i.set("portnum", port);
      i.eval("setAccessibility(true)"); // turn off access restrictions
      i.eval("server(portnum)");
      consoleLogger.info("Bean shell is started on port " + port);
    } catch (EvalError e) {
      e.printStackTrace();
    }
  }

  public int getListenPort() {
    return this.lsnr.getBindPort();
  }

  public synchronized void stop() {
    try {
      if (lockManager != null) lockManager.stop();
    } catch (InterruptedException e) {
      logger.error(e);
    }

    getStageManager().stopAll();

    if (lsnr != null) {
      try {
        lsnr.stop(5000);
      } catch (TCTimeoutException e) {
        logger.warn("timeout trying to stop listener: " + e.getMessage());
      }
    }

    if ((communicationsManager != null)) {
      communicationsManager.shutdown();
    }

    if (objectManager != null) {
      try {
        objectManager.stop();
      } catch (Throwable e) {
        logger.error(e);
      }
    }

    clientStateManager.stop();

    try {
      objectStore.shutdown();
    } catch (Throwable e) {
      logger.warn(e);
    }

    try {
      persistor.close();
    } catch (DBException e) {
      logger.warn(e);
    }

    if (sampledCounterManager != null) {
      try {
        sampledCounterManager.shutdown();
      } catch (Exception e) {
        logger.error(e);
      }
    }

    try {
      stopJMXServer();
    } catch (Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    basicStop();
  }

  public void quickStop() {
    try {
      stopJMXServer();
    } catch (Throwable t) {
      logger.error("Error shutting down jmx server", t);
    }

    // XXX: not calling basicStop() here, it creates a race condition with the Sleepycat's own writer lock (see
    // LKC-3239) Provided we ever fix graceful server shutdown, we'll want to uncommnet this at that time and/or get rid
    // of this method completely

    // basicStop();
  }

  private void basicStop() {
    if (this.startupLock != null) {
      this.startupLock.release();
    }
  }

  public ServerConfigurationContext getContext() {
    return context;
  }

  public ServerManagementContext getManagementContext() {
    return this.managementContext;
  }

  public MBeanServer getMBeanServer() {
    return l2Management.getMBeanServer();
  }

  private void startJMXServer() throws Exception {
    // Make sure we don't require authentication for JMX, unless someone's overridden it
    if (System.getProperty("com.sun.management.jmxremote.authenticate") == null) {
      System.setProperty("com.sun.management.jmxremote.authenticate", "false");
    }
    l2Management = new L2Management(tcServerInfoMBean, configSetupManager);
    l2Management.start();
  }

  private void stopJMXServer() throws Exception {
    try {
      if (l2Management != null) {
        l2Management.stop();
      }
    } finally {
      l2Management = null;
    }
  }
}
