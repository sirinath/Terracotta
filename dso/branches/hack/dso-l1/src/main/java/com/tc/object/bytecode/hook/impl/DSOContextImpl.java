/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook.impl;

import org.apache.log4j.Hierarchy;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.WriterAppender;
import org.apache.log4j.spi.RootLogger;

import com.tc.abortable.AbortableOperationManager;
import com.tc.abortable.AbortableOperationManagerImpl;
import com.tc.client.AbstractClientFactory;
import com.tc.cluster.DsoClusterImpl;
import com.tc.config.schema.L2ConfigForL1.L2Data;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L1ConfigurationSetupManager;
import com.tc.config.schema.setup.StandardConfigurationSetupManagerFactory;
import com.tc.lang.L1ThrowableHandler;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.license.ProductID;
import com.tc.logging.TCLogging;
import com.tc.net.core.SecurityInfo;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.object.DistributedObjectClient;
import com.tc.object.bytecode.hook.DSOContext;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.StandardDSOClientConfigHelperImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.object.loaders.SingleLoaderClassProvider;
import com.tc.object.tx.ClusterEventListener;
import com.tc.platform.PlatformService;
import com.tc.platform.rejoin.RejoinManagerImpl;
import com.tc.platform.rejoin.RejoinManagerInternal;
import com.tc.util.Assert;
import com.tc.util.UUID;
import com.tcclient.cluster.DsoClusterInternal;
import com.terracotta.management.security.SecretProvider;

import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.Callable;

public class DSOContextImpl implements DSOContext {
  private final String                       configSpec;
  private final TCSecurityManager            securityManager;
  private final SecurityInfo                 securityInfo;
  private final ClassLoader                  loader;
  private final boolean                      rejoin;
  private final ProductID                    productId;

  private volatile DSOClientConfigHelper     configHelper;
  private volatile DistributedObjectClient   client;

  public static DSOContext createStandaloneContext(String configSpec, ClassLoader loader, boolean rejoin,
                                                   TCSecurityManager securityManager, SecurityInfo securityInfo,
                                                   ProductID productId) {
    return createContext(configSpec, loader, rejoin, securityManager, securityInfo, productId);
  }

  public void init() throws ConfigurationSetupException {
    StandardConfigurationSetupManagerFactory factory = new StandardConfigurationSetupManagerFactory(
                                                                                                    (String[]) null,
                                                                                                    StandardConfigurationSetupManagerFactory.ConfigMode.EXPRESS_L1,
                                                                                                    new FatalIllegalConfigurationChangeHandler(),
                                                                                                    configSpec,
                                                                                                    securityManager);

    L1ConfigurationSetupManager config = factory.getL1TVSConfigurationSetupManager(securityInfo);
    config.setupLogging();

    final PreparedComponentsFromL2Connection connectionComponents;
    try {
      connectionComponents = validateMakeL2Connection(config, securityManager);
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    this.configHelper = new StandardDSOClientConfigHelperImpl(config);

    try {
      startToolkitConfigurator();
    } catch (Exception e) {
      throw new ConfigurationSetupException(e.getLocalizedMessage(), e);
    }

    L1ThrowableHandler throwableHandler = new L1ThrowableHandler(TCLogging.getLogger(DistributedObjectClient.class),
                                                                 new Callable<Void>() {

                                                                   @Override
                                                                   public Void call() throws Exception {
                                                                     shutdown();
                                                                     return null;
                                                                   }
                                                                 });
    final TCThreadGroup group = new TCThreadGroup(throwableHandler);

    final ClassProvider classProvider = new SingleLoaderClassProvider(loader == null ? getClass().getClassLoader()
        : loader);

    final RejoinManagerInternal rejoinManager = new RejoinManagerImpl(rejoin);
    final DsoClusterInternal dsoCluster = new DsoClusterImpl(rejoinManager);
    final UUID uuid = UUID.getUUID();
    final AbortableOperationManager abortableOperationManager = new AbortableOperationManagerImpl();

    final StartupAction action = new StartupHelper.StartupAction() {
      @Override
      public void execute() throws Throwable {
        final AbstractClientFactory clientFactory = AbstractClientFactory.getFactory();
        DSOContextImpl.this.client = clientFactory.createClient(configHelper, group, classProvider,
                                                                connectionComponents, dsoCluster, securityManager,
                                                                abortableOperationManager, rejoinManager, uuid,
                                                                productId);

        DSOContextImpl.this.client.start();

        dsoCluster.init(client.getClusterMetaDataManager(), client.getObjectManager(), client.getClusterEventsStage());
        dsoCluster.addClusterListener(new ClusterEventListener(client.getRemoteTransactionManager()));
      }

    };

    final StartupHelper startupHelper = new StartupHelper(group, action);
    startupHelper.startUp();
  }

  @Override
  public PlatformService getPlatformService() {
    return client.getPlatformService();
  }

  public static TCSecurityManager createSecurityManager(Map<String, Object> env) {
    return AbstractClientFactory.getFactory().createClientSecurityManager(env);
  }

  public byte[] getSecret() {
    return SecretProvider.getSecret();
  }

  private void startToolkitConfigurator() throws Exception {
    Class toolkitConfiguratorClass = null;
    try {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.EnterpriseToolkitConfigurator");
    } catch (ClassNotFoundException e) {
      toolkitConfiguratorClass = Class.forName("com.terracotta.toolkit.ToolkitConfigurator");
    }

    Object toolkitConfigurator = toolkitConfiguratorClass.newInstance();
    Method start = toolkitConfiguratorClass.getMethod("start", DSOClientConfigHelper.class);
    start.invoke(toolkitConfigurator, configHelper);
  }

  private static DSOContextImpl createContext(String configSpec, ClassLoader loader, boolean rejoin,
                                              TCSecurityManager securityManager, SecurityInfo securityInfo,
                                              ProductID productId) {
    return new DSOContextImpl(configSpec, loader, rejoin, securityManager, securityInfo, productId);
  }

  private DSOContextImpl(String configSpec, ClassLoader loader, boolean rejoin, TCSecurityManager securityManager,
                         SecurityInfo securityInfo, ProductID productId) {
    resolveClasses();

    this.configSpec = configSpec;
    this.loader = loader;
    this.rejoin = rejoin;
    this.securityManager = securityManager;
    this.securityInfo = securityInfo;
    this.productId = productId;
  }

  private void resolveClasses() {
    // This is to help a deadlock in log4j (see MNK-3461, MNK-3512)
    Logger l = new RootLogger(Level.ALL);
    Hierarchy h = new Hierarchy(l);
    l.addAppender(new WriterAppender(new PatternLayout(TCLogging.FILE_AND_JMX_PATTERN), new OutputStream() {
      @Override
      public void write(int b) {
        //
      }
    }));
    l.debug(h.toString(), new Throwable());
  }

  @Override
  public void addTunneledMBeanDomain(String mbeanDomain) {
    this.configHelper.addTunneledMBeanDomain(mbeanDomain);
  }

  private static PreparedComponentsFromL2Connection validateMakeL2Connection(L1ConfigurationSetupManager config,
                                                                             final TCSecurityManager securityManager) {
    L2Data[] l2Data = config.l2Config().l2Data();
    Assert.assertNotNull(l2Data);

    return new PreparedComponentsFromL2Connection(config, securityManager);
  }

  @Override
  public void shutdown() {
    client.shutdown();
  }

  @Override
  public void sendCurrentTunneledDomains() {
    client.getTunneledDomainManager().sendCurrentTunneledDomains();
  }
}
