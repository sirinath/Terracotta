/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.abortable.AbortableOperationManager;
import com.tc.async.api.Sink;
import com.tc.logging.ClientIDLogger;
import com.tc.logging.TCLogger;
import com.tc.management.ClientLockStatManager;
import com.tc.management.L1Management;
import com.tc.management.TCClient;
import com.tc.management.remote.protocol.terracotta.TunneledDomainManager;
import com.tc.management.remote.protocol.terracotta.TunnelingEventHandler;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.tcm.ClientMessageChannel;
import com.tc.net.protocol.tcm.CommunicationsManager;
import com.tc.net.protocol.tcm.GeneratedMessageFactory;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageRouter;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.HealthCheckerConfig;
import com.tc.net.protocol.transport.ReconnectionRejectedHandler;
import com.tc.object.bytecode.Manager;
import com.tc.object.bytecode.hook.impl.PreparedComponentsFromL2Connection;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.DSOMBeanConfig;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.api.DNAEncodingInternal;
import com.tc.object.gtx.ClientGlobalTransactionManager;
import com.tc.object.gtx.PreTransactionFlushCallback;
import com.tc.object.handshakemanager.ClientHandshakeCallback;
import com.tc.object.handshakemanager.ClientHandshakeManager;
import com.tc.object.idprovider.api.ObjectIDProvider;
import com.tc.object.idprovider.impl.ObjectIDClientHandshakeRequester;
import com.tc.object.idprovider.impl.RemoteObjectIDBatchSequenceProvider;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.locks.ClientLockManager;
import com.tc.object.locks.ClientLockManagerConfig;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.KeysForOrphanedValuesMessageFactory;
import com.tc.object.msg.LockRequestMessageFactory;
import com.tc.object.msg.NodeMetaDataMessageFactory;
import com.tc.object.msg.NodesWithKeysMessageFactory;
import com.tc.object.msg.NodesWithObjectsMessageFactory;
import com.tc.object.net.DSOClientMessageChannel;
import com.tc.object.servermap.localcache.L1ServerMapLocalCacheManager;
import com.tc.object.session.SessionManager;
import com.tc.object.session.SessionProvider;
import com.tc.object.tx.ClientTransactionBatchWriter.FoldingConfig;
import com.tc.object.tx.RemoteTransactionManager;
import com.tc.object.tx.TransactionIDGenerator;
import com.tc.runtime.logging.LongGCLogger;
import com.tc.stats.counter.sampled.derived.SampledRateCounter;
import com.tc.util.ToggleableReferenceManager;
import com.tc.util.UUID;
import com.tc.util.concurrent.TaskRunner;
import com.tc.util.runtime.ThreadIDManager;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceReceiver;
import com.tcclient.cluster.DsoClusterInternalEventsGun;

import java.util.Collection;
import java.util.Map;

public interface DSOClientBuilder {

  DSOClientMessageChannel createDSOClientMessageChannel(final CommunicationsManager commMgr,
                                                        final PreparedComponentsFromL2Connection connComp,
                                                        final SessionProvider sessionProvider, int maxReconnectTries,
                                                        int socketConnectTimeout, TCClient client);

  CommunicationsManager createCommunicationsManager(final MessageMonitor monitor,
                                                    TCMessageRouter messageRouter,
                                                    final NetworkStackHarnessFactory stackHarnessFactory,
                                                    final ConnectionPolicy connectionPolicy,
                                                    int workerCommThreads,
                                                    final HealthCheckerConfig hcConfig,
                                                    Map<TCMessageType, Class> messageTypeClassMapping,
                                                    Map<TCMessageType, GeneratedMessageFactory> messageTypeFactoryMapping,
                                                    ReconnectionRejectedHandler reconnectionRejectedBehaviour,
                                                    TCSecurityManager securityManager);

  TunnelingEventHandler createTunnelingEventHandler(ClientMessageChannel ch, DSOMBeanConfig config, UUID uuid);

  TunneledDomainManager createTunneledDomainManager(final ClientMessageChannel ch, final DSOMBeanConfig config,
                                                    final TunnelingEventHandler teh);

  ClientGlobalTransactionManager createClientGlobalTransactionManager(final RemoteTransactionManager remoteTxnMgr,
                                                                      final PreTransactionFlushCallback preTransactionFlushCallback);

  RemoteObjectManager createRemoteObjectManager(final TCLogger logger, final DSOClientMessageChannel dsoChannel,
                                                final int faultCount, final SessionManager sessionManager,
                                                final AbortableOperationManager abortableOperationManager,
                                                final TaskRunner taskRunner);

  RemoteServerMapManager createRemoteServerMapManager(final TCLogger logger, 
                                                      final RemoteObjectManager remote,
                                                      final DSOClientMessageChannel dsoChannel,
                                                      final SessionManager sessionManager,
                                                      final L1ServerMapLocalCacheManager globalLocalCacheManager,
                                                      final AbortableOperationManager abortableOperationManager,
                                                      final TaskRunner taskRunner);

  RemoteSearchRequestManager createRemoteSearchRequestManager(final TCLogger logger,
                                                              final DSOClientMessageChannel dsoChannel,
                                                              final SessionManager sessionManager,
                                                              final AbortableOperationManager abortableOperationManager);

  ClusterMetaDataManager createClusterMetaDataManager(final DSOClientMessageChannel dsoChannel,
                                                      final DNAEncoding encoding,
                                                      final ThreadIDManager threadIDManager,
                                                      final NodesWithObjectsMessageFactory nwoFactory,
                                                      final KeysForOrphanedValuesMessageFactory kfovFactory,
                                                      final NodeMetaDataMessageFactory nmdmFactory,
                                                      final NodesWithKeysMessageFactory nwkmFactory);

  ClientObjectManagerImpl createObjectManager(final RemoteObjectManager remoteObjectManager,
                                              final DSOClientConfigHelper dsoConfig, final ObjectIDProvider idProvider,
                                              final ClientIDProvider clientIDProvider,
                                              final ClassProvider classProviderLocal,
                                              final TCClassFactory classFactory, final TCObjectFactory objectFactory,
                                              final Portability portability, final DSOClientMessageChannel dsoChannel,
                                              final ToggleableReferenceManager toggleRefMgr,
                                              TCObjectSelfStore tcObjectSelfStore,
                                              AbortableOperationManager abortableOperationManager);

  ClientLockManager createLockManager(final DSOClientMessageChannel dsoChannel, final ClientIDLogger clientIDLogger,
                                      final SessionManager sessionManager, final ClientLockStatManager lockStatManager,
                                      final LockRequestMessageFactory lockRequestMessageFactory,
                                      final ThreadIDManager threadManager,
                                      final ClientGlobalTransactionManager clientGlobalTransactionManager,
                                      final ClientLockManagerConfig clientLockManagerConfig,
                                      final AbortableOperationManager abortableOperationManager,
                                      final TaskRunner taskRunner);

  @Deprecated
  ClientLockStatManager createLockStatsManager();

  RemoteTransactionManager createRemoteTransactionManager(final ClientIDProvider cidProvider,
                                                          final DNAEncodingInternal encoding,
                                                          final FoldingConfig foldingConfig,
                                                          final TransactionIDGenerator transactionIDGenerator,
                                                          final SessionManager sessionManager,
                                                          final DSOClientMessageChannel dsoChannel,
                                                          final SampledRateCounter transactionSizeCounter,
                                                          final SampledRateCounter transactionPerBatchCounter,
                                                          final AbortableOperationManager abortableOperationManager,
                                                          final TaskRunner taskRunner);

  ObjectIDClientHandshakeRequester getObjectIDClientHandshakeRequester(final BatchSequenceReceiver sequence);

  BatchSequence[] createSequences(RemoteObjectIDBatchSequenceProvider remoteIDProvider, int requestSize);

  ObjectIDProvider createObjectIdProvider(BatchSequence[] sequences, ClientIDProvider clientIDProvider);

  BatchSequenceReceiver getBatchReceiver(BatchSequence[] sequences);

  ClientHandshakeManager createClientHandshakeManager(final TCLogger logger, final DSOClientMessageChannel channel,
                                                      final ClientHandshakeMessageFactory chmf, final Sink pauseSink,
                                                      final SessionManager sessionManager,
                                                      final DsoClusterInternalEventsGun dsoClusterEventsGun,
                                                      final String clientVersion,
                                                      final Collection<ClientHandshakeCallback> callbacks,
                                                      Collection<ClearableCallback> clearCallbacks);

  L1Management createL1Management(TunnelingEventHandler teh, String rawConfigText,
                                  DistributedObjectClient distributedObjectClient);

  void registerForOperatorEvents(L1Management management);

  TCClassFactory createTCClassFactory(final DSOClientConfigHelper config, final ClassProvider classProvider,
                                      final DNAEncoding dnaEncoding, final Manager manager,
                                      final L1ServerMapLocalCacheManager globalLocalCacheManager,
                                      final RemoteServerMapManager remoteServerMapManager);

  LongGCLogger createLongGCLogger(long gcTimeOut);

  RemoteResourceManager createRemoteResourceManager(DSOClientMessageChannel dsoChannel,
                                                    AbortableOperationManager abortableOperationManager);

  ServerEventListenerManager createServerEventListenerManager(DSOClientMessageChannel dsoChannel);
}
