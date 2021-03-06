/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import EDU.oswego.cs.dl.util.concurrent.BoundedLinkedQueue;

import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.cluster.Cluster;
import com.tc.config.schema.dynamic.ConfigItem;
import com.tc.lang.TCThreadGroup;
import com.tc.logging.CallbackDumpAdapter;
import com.tc.logging.ChannelIDLogger;
import com.tc.logging.ChannelIDLoggerProvider;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.beans.sessions.SessionMonitorMBean;
import com.tc.management.lock.stats.ClientLockStatisticsManagerImpl;
import com.tc.management.lock.stats.LockStatisticsMessage;
import com.tc.management.lock.stats.LockStatisticsResponseMessage;
import com.tc.management.remote.protocol.terracotta.JmxRemoteTunnelMessage;
import com.tc.management.remote.protocol.terracotta.L1JmxReady;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.MaxConnectionsExceededException;
import com.tc.net.core.ConnectionAddressProvider;
import com.tc.net.core.ConnectionInfo;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.PlainNetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OOOEventHandler;
import com.tc.net.protocol.delivery.OOONetworkStackHarnessFactory;
import com.tc.net.protocol.delivery.OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.CommunicationsManagerImpl;
import com.tc.net.protocol.tcm.HydrateHandler;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.MessageMonitorImpl;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.HealthCheckerConfigImpl;
import com.tc.net.protocol.transport.NullConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeMessage;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.cache.CacheConfigImpl;
import com.tc.object.cache.CacheManager;
import com.tc.object.cache.ClockEvictionPolicy;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.event.DmiManager;
import com.tc.object.event.DmiManagerImpl;
import com.tc.object.field.TCFieldFactory;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.ClientGlobalTransactionManagerImpl;
import com.tc.object.handler.BatchTransactionAckHandler;
import com.tc.object.handler.ClientCoordinationHandler;
import com.tc.object.handler.DmiHandler;
import com.tc.object.handler.LockResponseHandler;
import com.tc.object.handler.LockStatisticsEnableDisableHandler;
import com.tc.object.handler.LockStatisticsResponseHandler;
import com.tc.object.handler.ReceiveObjectHandler;
import com.tc.object.handler.ReceiveRootIDHandler;
import com.tc.object.handler.ReceiveTransactionCompleteHandler;
import com.tc.object.handler.ReceiveTransactionHandler;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDProviderImpl;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.lockmanager.api.ClientLockManager;
import com.tc.object.lockmanager.impl.ClientLockManagerConfigImpl;
import com.tc.object.lockmanager.impl.ClientLockManagerImpl;
import com.tc.object.lockmanager.impl.RemoteLockManagerImpl;
import com.tc.object.lockmanager.impl.ThreadLockManagerImpl;
import com.tc.object.logging.RuntimeLogger;
import com.tc.object.logging.RuntimeLoggerImpl;
import com.tc.object.msg.AcknowledgeTransactionMessageImpl;
import com.tc.object.msg.BatchTransactionAcknowledgeMessageImpl;
import com.tc.object.msg.BroadcastTransactionMessageImpl;
import com.tc.object.msg.ClientHandshakeAckMessageImpl;
import com.tc.object.msg.ClientHandshakeMessageImpl;
import com.tc.object.msg.ClusterMembershipMessage;
import com.tc.object.msg.CommitTransactionMessageImpl;
import com.tc.object.msg.CompletedTransactionLowWaterMarkMessage;
import com.tc.object.msg.JMXMessage;
import com.tc.object.msg.LockRequestMessage;
import com.tc.object.msg.LockResponseMessage;
import com.tc.object.msg.ObjectIDBatchRequestMessage;
import com.tc.object.msg.ObjectIDBatchRequestResponseMessage;
import com.tc.object.msg.ObjectsNotFoundMessage;
import com.tc.object.msg.RequestManagedObjectMessageImpl;
import com.tc.object.msg.RequestManagedObjectResponseMessage;
import com.tc.object.msg.RequestRootMessageImpl;
import com.tc.object.msg.RequestRootResponseMessage;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionManagerImpl;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionFactory;
import com.tc.object.tx.ClientTransactionFactoryImpl;
import com.tc.object.tx.ClientTransactionManager;
import com.tc.object.tx.ClientTransactionManagerImpl;
import com.tc.object.tx.LockAccounting;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.RemoteTransactionManagerImpl;
import com.tc.object.tx.TransactionBatchAccounting;
import com.tc.object.tx.TransactionBatchFactory;
import com.tc.object.tx.TransactionBatchWriterFactory;
import com.tc.object.tx.TransactionBatchWriter.FoldingConfig;
import com.tc.properties.ReconnectConfig;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.statistics.StatisticsAgentSubSystem;
import com.tc.statistics.StatisticsAgentSubSystemImpl;
import com.tc.statistics.retrieval.StatisticsRetrievalRegistry;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvictRequest;
import com.tc.statistics.retrieval.actions.SRACacheObjectsEvicted;
import com.tc.statistics.retrieval.actions.SRAL1OutstandingBatches;
import com.tc.statistics.retrieval.actions.SRAL1PendingTransactionsSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionSize;
import com.tc.statistics.retrieval.actions.SRAL1TransactionsPerBatch;
import com.tc.statistics.retrieval.actions.SRAMemoryUsage;
import com.tc.statistics.retrieval.actions.SRAMessages;
import com.tc.statistics.retrieval.actions.SRAStageQueueDepths;
import com.tc.statistics.retrieval.actions.SRASystemProperties;
import com.tc.stats.counter.CounterManager;
import com.tc.stats.counter.CounterManagerImpl;
import com.tc.stats.counter.sampled.SampledCounter;
import com.tc.stats.counter.sampled.SampledCounterConfig;
import com.tc.util.Assert;
import com.tc.util.CommonShutDownHook;
import com.tc.util.ProductInfo;
import com.tc.util.TCTimeoutException;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.Sequence;
import com.tc.util.sequence.SimpleSequence;

import java.io.IOException;
import java.net.ConnectException;
import java.util.Collection;
import java.util.Collections;

/**
 * This is the main point of entry into the DSO client.
 */
public class DistributedObjectClient extends SEDA {

  private static final TCLogger                    logger                     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger                    consoleLogger              = CustomerLogging.getConsoleLogger();

  private final DSOClientConfigHelper              config;
  private final ClassProvider                      classProvider;
  private final PreparedComponentsFromL2Connection connectionComponents;
  private final Manager                            manager;
  private final Cluster                            cluster;
  private final TCThreadGroup                      threadGroup;
  private final StatisticsAgentSubSystemImpl       statisticsAgentSubSystem;

  private DSOClientMessageChannel                  channel;
  private ClientLockManager                        lockManager;
  private ClientObjectManagerImpl                  objectManager;
  private ClientTransactionManager                 txManager;
  private CommunicationsManager                    communicationsManager;
  private RemoteTransactionManager                 rtxManager;
  private PauseListener                            pauseListener;
  private ClientHandshakeManager                   clientHandshakeManager;
  private RuntimeLogger                            runtimeLogger;
  private CacheManager                             cacheManager;
  private L1Management                             l1Management;
  private TCProperties                             l1Properties;
  private DmiManager                               dmiManager;
  private boolean                                  createDedicatedMBeanServer = false;
  private CounterManager                           sampledCounterManager;

  public DistributedObjectClient(DSOClientConfigHelper config, TCThreadGroup threadGroup, ClassProvider classProvider,
                                 PreparedComponentsFromL2Connection connectionComponents, Manager manager,
                                 Cluster cluster) {
    super(threadGroup, BoundedLinkedQueue.class.getName());
    Assert.assertNotNull(config);
    this.config = config;
    this.classProvider = classProvider;
    this.connectionComponents = connectionComponents;
    this.pauseListener = new NullPauseListener();
    this.manager = manager;
    this.cluster = cluster;
    this.threadGroup = threadGroup;
    this.statisticsAgentSubSystem = new StatisticsAgentSubSystemImpl();
  }

  public void setCreateDedicatedMBeanServer(boolean createDedicatedMBeanServer) {
    this.createDedicatedMBeanServer = createDedicatedMBeanServer;
  }

  public void setPauseListener(PauseListener pauseListener) {
    this.pauseListener = pauseListener;
  }

  private void populateStatisticsRetrievalRegistry(final StatisticsRetrievalRegistry registry,
                                                   final StageManager stageManager,
                                                   final MessageMonitor messageMonitor,
                                                   final SampledCounter outstandingBatchesCounter,
                                                   final SampledCounter numTransactionCounter,
                                                   final SampledCounter numBatchesCounter,
                                                   final SampledCounter batchSizeCounter,
                                                   final SampledCounter pendingTransactionsSize) {
    registry.registerActionInstance(new SRAMemoryUsage());
    registry.registerActionInstance(new SRASystemProperties());
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRACpu");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRANetworkActivity");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRADiskActivity");
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAThreadDump");
    registry.registerActionInstance(new SRAStageQueueDepths(stageManager));
    registry.registerActionInstance(new SRACacheObjectsEvictRequest());
    registry.registerActionInstance(new SRACacheObjectsEvicted());
    registry.registerActionInstance("com.tc.statistics.retrieval.actions.SRAVmGarbageCollector");
    registry.registerActionInstance(new SRAMessages(messageMonitor));
    registry.registerActionInstance(new SRAL1OutstandingBatches(outstandingBatchesCounter));
    registry.registerActionInstance(new SRAL1TransactionsPerBatch(numTransactionCounter, numBatchesCounter));
    registry.registerActionInstance(new SRAL1TransactionSize(batchSizeCounter, numTransactionCounter));
    registry.registerActionInstance(new SRAL1PendingTransactionsSize(pendingTransactionsSize));
  }

  public synchronized void start() {
    TCProperties tcProperties = TCPropertiesImpl.getProperties();
    l1Properties = tcProperties.getPropertiesFor("l1");
    int maxSize = tcProperties.getInt(TCPropertiesConsts.L1_SEDA_STAGE_SINK_CAPACITY);
    int faultCount = config.getFaultCount();

    final Sequence sessionSequence = new SimpleSequence();
    final SessionManager sessionManager = new SessionManagerImpl(sessionSequence);
    final SessionProvider sessionProvider = (SessionProvider) sessionManager;

    StageManager stageManager = getStageManager();

    // stageManager.turnTracingOn();

    // //////////////////////////////////
    // create NetworkStackHarnessFactory
    ReconnectConfig l1ReconnectConfig = config.getL1ReconnectProperties();
    final boolean useOOOLayer = l1ReconnectConfig.getReconnectEnabled();
    final NetworkStackHarnessFactory networkStackHarnessFactory;
    if (useOOOLayer) {
      final Stage oooStage = stageManager.createStage("OOONetStage", new OOOEventHandler(), 1, maxSize);
      final int sendQueueCap = l1ReconnectConfig.getSendQueueCapacity();
      networkStackHarnessFactory = new OOONetworkStackHarnessFactory(
                                                                     new OnceAndOnlyOnceProtocolNetworkLayerFactoryImpl(),
                                                                     oooStage.getSink(), l1ReconnectConfig,
                                                                     sendQueueCap);
    } else {
      networkStackHarnessFactory = new PlainNetworkStackHarnessFactory();
    }
    // //////////////////////////////////

    sampledCounterManager = new CounterManagerImpl();

    MessageMonitor mm = MessageMonitorImpl.createMonitor(tcProperties, logger);

    communicationsManager = new CommunicationsManagerImpl(mm, networkStackHarnessFactory, new NullConnectionPolicy(),
                                                          new HealthCheckerConfigImpl(l1Properties
                                                              .getPropertiesFor("healthcheck.l2"), "DSO Client"));

    logger.debug("Created CommunicationsManager.");

    ConfigItem connectionInfoItem = this.connectionComponents.createConnectionInfoConfigItem();
    ConnectionInfo[] connectionInfo = (ConnectionInfo[]) connectionInfoItem.getObject();
    ConnectionAddressProvider addrProvider = new ConnectionAddressProvider(connectionInfo);

    String serverHost = connectionInfo[0].getHostname();
    int serverPort = connectionInfo[0].getPort();

    int timeout = tcProperties.getInt(TCPropertiesConsts.L1_SOCKET_CONNECT_TIMEOUT);
    if (timeout < 0) { throw new IllegalArgumentException("invalid socket time value: " + timeout); }

    channel = new DSOClientMessageChannelImpl(communicationsManager
        .createClientChannel(sessionProvider, -1, serverHost, serverPort, timeout, addrProvider,
                             TransportHandshakeMessage.NO_CALLBACK_PORT));
    ChannelIDLoggerProvider cidLoggerProvider = new ChannelIDLoggerProvider(channel.getChannelIDProvider());
    stageManager.setLoggerProvider(cidLoggerProvider);

    ClientIDProvider clientIDProvider = new ClientIDProviderImpl(channel.getChannelIDProvider());

    this.runtimeLogger = new RuntimeLoggerImpl(config);

    logger.debug("Created channel.");

    ClientTransactionFactory txFactory = new ClientTransactionFactoryImpl(runtimeLogger);

    DNAEncoding encoding = new ApplicatorDNAEncodingImpl(classProvider);
    TransactionBatchFactory txBatchFactory = new TransactionBatchWriterFactory(channel
        .getCommitTransactionMessageFactory(), encoding, FoldingConfig.createFromProperties(tcProperties));

    SampledCounter outstandingBatchesCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter numTransactionCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter numBatchesCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter batchSizeCounter = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));
    SampledCounter pendingTransactionsSize = (SampledCounter) sampledCounterManager
        .createCounter(new SampledCounterConfig(1, 900, true, 0L));

    rtxManager = new RemoteTransactionManagerImpl(new ChannelIDLogger(channel.getChannelIDProvider(), TCLogging
        .getLogger(RemoteTransactionManagerImpl.class)), txBatchFactory, new TransactionBatchAccounting(),
                                                  new LockAccounting(), sessionManager, channel,
                                                  outstandingBatchesCounter, numTransactionCounter, numBatchesCounter,
                                                  batchSizeCounter, pendingTransactionsSize);

    ClientGlobalTransactionManager gtxManager = new ClientGlobalTransactionManagerImpl(rtxManager);

    ClientLockStatManager lockStatManager = new ClientLockStatisticsManagerImpl();

    lockManager = new ClientLockManagerImpl(new ChannelIDLogger(channel.getChannelIDProvider(), TCLogging
        .getLogger(ClientLockManager.class)), new RemoteLockManagerImpl(channel.getLockRequestMessageFactory(),
                                                                        gtxManager), sessionManager, lockStatManager,
                                            new ClientLockManagerConfigImpl(l1Properties
                                                .getPropertiesFor("lockmanager")));
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(lockManager));
    RemoteObjectManager remoteObjectManager = new RemoteObjectManagerImpl(new ChannelIDLogger(channel
        .getChannelIDProvider(), TCLogging.getLogger(RemoteObjectManager.class)), clientIDProvider, channel
        .getRequestRootMessageFactory(), channel.getRequestManagedObjectMessageFactory(),
                                                                          new NullObjectRequestMonitor(), faultCount,
                                                                          sessionManager);

    RemoteObjectIDBatchSequenceProvider remoteIDProvider = new RemoteObjectIDBatchSequenceProvider(channel
        .getObjectIDBatchRequestMessageFactory());
    BatchSequence sequence = new BatchSequence(remoteIDProvider, l1Properties
        .getInt("objectmanager.objectid.request.size"));
    ObjectIDProvider idProvider = new ObjectIDProviderImpl(sequence);
    remoteIDProvider.setBatchSequenceReceiver(sequence);

    TCClassFactory classFactory = new TCClassFactoryImpl(new TCFieldFactory(config), config, classProvider, encoding);
    TCObjectFactory objectFactory = new TCObjectFactoryImpl(classFactory);

    ToggleableReferenceManager toggleRefMgr = new ToggleableReferenceManager();
    toggleRefMgr.start();

    // setup statistics subsystem
    if (statisticsAgentSubSystem.setup(config.getNewCommonL1Config())) {
      populateStatisticsRetrievalRegistry(statisticsAgentSubSystem.getStatisticsRetrievalRegistry(), stageManager, mm,
                                          outstandingBatchesCounter, numTransactionCounter, numBatchesCounter,
                                          batchSizeCounter, pendingTransactionsSize);
    }

    objectManager = new ClientObjectManagerImpl(remoteObjectManager, config, idProvider, new ClockEvictionPolicy(-1),
                                                runtimeLogger, channel.getChannelIDProvider(), classProvider,
                                                classFactory, objectFactory, config.getPortability(), channel,
                                                toggleRefMgr);
    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(objectManager));
    TCProperties cacheManagerProperties = l1Properties.getPropertiesFor("cachemanager");
    if (cacheManagerProperties.getBoolean("enabled")) {
      this.cacheManager = new CacheManager(objectManager, new CacheConfigImpl(cacheManagerProperties),
                                           getThreadGroup(), statisticsAgentSubSystem);
      if (logger.isDebugEnabled()) {
        logger.debug("CacheManager Enabled : " + cacheManager);
      }
    } else {
      logger.warn("CacheManager is Disabled");
    }

    // Set up the JMX management stuff
    final TunnelingEventHandler teh = new TunnelingEventHandler(channel.channel());
    l1Management = new L1Management(teh, statisticsAgentSubSystem, runtimeLogger, manager.getInstrumentationLogger(),
                                    config.rawConfigText());
    l1Management.start(createDedicatedMBeanServer);

    txManager = new ClientTransactionManagerImpl(channel.getChannelIDProvider(), objectManager,
                                                 new ThreadLockManagerImpl(lockManager), txFactory, rtxManager,
                                                 runtimeLogger, l1Management.findClientTxMonitorMBean());

    threadGroup.addCallbackOnExitHandler(new CallbackDumpAdapter(txManager));
    Stage lockResponse = stageManager.createStage(ClientConfigurationContext.LOCK_RESPONSE_STAGE,
                                                  new LockResponseHandler(sessionManager), 1, maxSize);
    Stage receiveRootID = stageManager.createStage(ClientConfigurationContext.RECEIVE_ROOT_ID_STAGE,
                                                   new ReceiveRootIDHandler(), 1, maxSize);
    Stage receiveObject = stageManager.createStage(ClientConfigurationContext.RECEIVE_OBJECT_STAGE,
                                                   new ReceiveObjectHandler(), 1, maxSize);
    this.dmiManager = new DmiManagerImpl(classProvider, objectManager, runtimeLogger);
    Stage dmiStage = stageManager.createStage(ClientConfigurationContext.DMI_STAGE, new DmiHandler(dmiManager), 1,
                                              maxSize);

    Stage receiveTransaction = stageManager
        .createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_STAGE,
                     new ReceiveTransactionHandler(channel.getChannelIDProvider(), channel
                         .getAcknowledgeTransactionMessageFactory(), gtxManager, sessionManager, dmiStage.getSink(),
                                                   dmiManager), 1, maxSize);
    Stage oidRequestResponse = stageManager.createStage(ClientConfigurationContext.OBJECT_ID_REQUEST_RESPONSE_STAGE,
                                                        remoteIDProvider, 1, maxSize);
    Stage transactionResponse = stageManager.createStage(ClientConfigurationContext.RECEIVE_TRANSACTION_COMPLETE_STAGE,
                                                         new ReceiveTransactionCompleteHandler(), 1, maxSize);
    Stage hydrateStage = stageManager.createStage(ClientConfigurationContext.HYDRATE_MESSAGE_STAGE,
                                                  new HydrateHandler(), 1, maxSize);
    Stage batchTxnAckStage = stageManager.createStage(ClientConfigurationContext.BATCH_TXN_ACK_STAGE,
                                                      new BatchTransactionAckHandler(), 1, maxSize);

    // By design this stage needs to be single threaded. If it wasn't then cluster memebership messages could get
    // processed before the client handshake ack, and this client would get a faulty view of the cluster at best, or
    // more likely an AssertionError
    Stage pauseStage = stageManager.createStage(ClientConfigurationContext.CLIENT_COORDINATION_STAGE,
                                                new ClientCoordinationHandler(cluster), 1, maxSize);
    Stage lockStatisticsStage = stageManager.createStage(ClientConfigurationContext.LOCK_STATISTICS_RESPONSE_STAGE,
                                                         new LockStatisticsResponseHandler(), 1, 1);
    final Stage lockStatisticsEnableDisableStage = stageManager
        .createStage(ClientConfigurationContext.LOCK_STATISTICS_ENABLE_DISABLE_STAGE,
                     new LockStatisticsEnableDisableHandler(), 1, 1);
    lockStatManager.start(channel, lockStatisticsStage.getSink());

    final Stage jmxRemoteTunnelStage = stageManager.createStage(ClientConfigurationContext.JMXREMOTE_TUNNEL_STAGE, teh,
                                                                1, maxSize);

    // This set is designed to give the handshake manager an opportunity to pause stages when it is pausing due to
    // disconnect. Unfortunately, the lock response stage can block, which I didn't realize at the time, so it's not
    // being used.
    Collection stagesToPauseOnDisconnect = Collections.EMPTY_LIST;
    ProductInfo pInfo = ProductInfo.getInstance();
    clientHandshakeManager = new ClientHandshakeManager(new ChannelIDLogger(channel.getChannelIDProvider(), TCLogging
        .getLogger(ClientHandshakeManager.class)), clientIDProvider, channel.getClientHandshakeMessageFactory(),
                                                        objectManager, remoteObjectManager, lockManager, rtxManager,
                                                        gtxManager, stagesToPauseOnDisconnect, pauseStage.getSink(),
                                                        sessionManager, pauseListener, sequence, cluster, pInfo
                                                            .version());
    channel.addListener(clientHandshakeManager);

    ClientConfigurationContext cc = new ClientConfigurationContext(stageManager, lockManager, remoteObjectManager,
                                                                   txManager, clientHandshakeManager);
    stageManager.startAll(cc);

    channel.addClassMapping(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, BatchTransactionAcknowledgeMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_ROOT_MESSAGE, RequestRootMessageImpl.class);
    channel.addClassMapping(TCMessageType.LOCK_REQUEST_MESSAGE, LockRequestMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_RESPONSE_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_RECALL_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, LockResponseMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_STAT_MESSAGE, LockStatisticsMessage.class);
    channel.addClassMapping(TCMessageType.LOCK_STATISTICS_RESPONSE_MESSAGE, LockStatisticsResponseMessage.class);
    channel.addClassMapping(TCMessageType.COMMIT_TRANSACTION_MESSAGE, CommitTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, RequestRootResponseMessage.class);
    channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_MESSAGE, RequestManagedObjectMessageImpl.class);
    channel.addClassMapping(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE,
                            RequestManagedObjectResponseMessage.class);
    channel.addClassMapping(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, ObjectsNotFoundMessage.class);
    channel.addClassMapping(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, BroadcastTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_MESSAGE, ObjectIDBatchRequestMessage.class);
    channel.addClassMapping(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE,
                            ObjectIDBatchRequestResponseMessage.class);
    channel.addClassMapping(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, AcknowledgeTransactionMessageImpl.class);
    channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_MESSAGE, ClientHandshakeMessageImpl.class);
    channel.addClassMapping(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, ClientHandshakeAckMessageImpl.class);
    channel.addClassMapping(TCMessageType.JMX_MESSAGE, JMXMessage.class);
    channel.addClassMapping(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, JmxRemoteTunnelMessage.class);
    channel.addClassMapping(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, ClusterMembershipMessage.class);
    channel.addClassMapping(TCMessageType.CLIENT_JMX_READY_MESSAGE, L1JmxReady.class);
    channel.addClassMapping(TCMessageType.COMPLETED_TRANSACTION_LOWWATERMARK_MESSAGE,
                            CompletedTransactionLowWaterMarkMessage.class);

    logger.debug("Added class mappings.");

    Sink hydrateSink = hydrateStage.getSink();
    channel.routeMessageType(TCMessageType.LOCK_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_QUERY_RESPONSE_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_STAT_MESSAGE, lockStatisticsEnableDisableStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.LOCK_RECALL_MESSAGE, lockResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.REQUEST_ROOT_RESPONSE_MESSAGE, receiveRootID.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.REQUEST_MANAGED_OBJECT_RESPONSE_MESSAGE, receiveObject.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.OBJECTS_NOT_FOUND_RESPONSE_MESSAGE, receiveObject.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.BROADCAST_TRANSACTION_MESSAGE, receiveTransaction.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.OBJECT_ID_BATCH_REQUEST_RESPONSE_MESSAGE, oidRequestResponse.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.ACKNOWLEDGE_TRANSACTION_MESSAGE, transactionResponse.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.BATCH_TRANSACTION_ACK_MESSAGE, batchTxnAckStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.CLIENT_HANDSHAKE_ACK_MESSAGE, pauseStage.getSink(), hydrateSink);
    channel.routeMessageType(TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE, jmxRemoteTunnelStage.getSink(),
                             hydrateSink);
    channel.routeMessageType(TCMessageType.CLUSTER_MEMBERSHIP_EVENT_MESSAGE, pauseStage.getSink(), hydrateSink);

    final int maxConnectRetries = l1Properties.getInt("max.connect.retries");
    int i = 0;
    while (maxConnectRetries <= 0 || i < maxConnectRetries) {
      try {
        logger.debug("Trying to open channel....");
        channel.open();
        logger.debug("Channel open");
        break;
      } catch (TCTimeoutException tcte) {
        consoleLogger.warn("Timeout connecting to server: " + serverHost + ":" + serverPort + ". " + tcte.getMessage());
        ThreadUtil.reallySleep(5000);
      } catch (ConnectException e) {
        consoleLogger.warn("Connection refused from server: " + serverHost + ":" + serverPort);
        ThreadUtil.reallySleep(5000);
      } catch (MaxConnectionsExceededException e) {
        consoleLogger.warn("Connection refused MAXIMUM CONNECTIONS TO SERVER EXCEEDED: " + serverHost + ":"
                           + serverPort);
        ThreadUtil.reallySleep(5000);
      } catch (IOException ioe) {
        consoleLogger.warn("IOException connecting to server: " + serverHost + ":" + serverPort + ". "
                           + ioe.getMessage());
        ThreadUtil.reallySleep(5000);
      }
      i++;
    }
    if (i == maxConnectRetries) {
      consoleLogger.error("MaxConnectRetries '" + maxConnectRetries + "' attempted. Exiting.");
      System.exit(-1);
    }
    clientHandshakeManager.waitForHandshake();

    if (statisticsAgentSubSystem.isActive()) {
      statisticsAgentSubSystem.setDefaultAgentDifferentiator("L1/" + channel.channel().getChannelID().toLong());
    }

    cluster.addClusterEventListener(l1Management.getTerracottaCluster());
    setLoggerOnExit();
  }

  private void setLoggerOnExit() {
    CommonShutDownHook.addShutdownHook(new Runnable() {
      public void run() {
        logger.info("L1 Exiting...");
      }
    });
  }

  /**
   * Note that this method shuts down the manager that is associated with this client, this is only used in tests. To
   * properly shut down resources of this client for production, the code should be added to
   * {@link ClientShutdownManager} and not to this method.
   */
  public synchronized void stopForTests() {
    manager.stop();
  }

  public ClientLockManager getLockManager() {
    return lockManager;
  }

  public ClientTransactionManager getTransactionManager() {
    return txManager;
  }

  public ClientObjectManager getObjectManager() {
    return objectManager;
  }

  public RemoteTransactionManager getRemoteTransactionManager() {
    return rtxManager;
  }

  public CommunicationsManager getCommunicationsManager() {
    return communicationsManager;
  }

  public DSOClientMessageChannel getChannel() {
    return channel;
  }

  public ClientHandshakeManager getClientHandshakeManager() {
    return clientHandshakeManager;
  }

  public RuntimeLogger getRuntimeLogger() {
    return runtimeLogger;
  }

  public SessionMonitorMBean getSessionMonitorMBean() {
    return l1Management.findSessionMonitorMBean();
  }

  public DmiManager getDmiManager() {
    return dmiManager;
  }

  public StatisticsAgentSubSystem getStatisticsAgentSubSystem() {
    return statisticsAgentSubSystem;
  }

}
