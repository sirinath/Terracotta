/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.mortbay.jetty.Request;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.security.Constraint;
import org.mortbay.jetty.security.ConstraintMapping;
import org.mortbay.jetty.security.HashUserRealm;
import org.mortbay.jetty.security.SecurityHandler;
import org.mortbay.jetty.security.UserRealm;
import org.mortbay.jetty.servlet.Context;
import org.mortbay.jetty.servlet.DefaultServlet;
import org.mortbay.jetty.servlet.ServletHandler;
import org.mortbay.jetty.servlet.ServletHolder;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.SEDA;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.async.api.StageManager;
import com.tc.config.Directories;
import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.CommonL2Config;
import com.tc.config.schema.ConfigurationModel;
import com.tc.config.schema.HaConfigSchema;
import com.tc.config.schema.L2Info;
import com.tc.config.schema.SecurityConfig;
import com.tc.config.schema.ServerGroupInfo;
import com.tc.config.schema.messaging.http.ConfigServlet;
import com.tc.config.schema.messaging.http.GroupIDMapServlet;
import com.tc.config.schema.messaging.http.GroupInfoServlet;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.l2.state.StateManager;
import com.tc.lang.StartupHelper;
import com.tc.lang.StartupHelper.StartupAction;
import com.tc.lang.TCThreadGroup;
import com.tc.lang.ThrowableHandler;
import com.tc.license.LicenseManager;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.beans.L2MBeanNames;
import com.tc.management.beans.L2State;
import com.tc.management.beans.TCServerInfo;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.net.TCSocketAddress;
import com.tc.net.core.security.TCSecurityManager;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.ConnectionPolicyImpl;
import com.tc.objectserver.core.api.ServerConfigurationContext;
import com.tc.objectserver.core.impl.ServerManagementContext;
import com.tc.objectserver.dgc.impl.GCStatsEventPublisher;
import com.tc.objectserver.impl.DistributedObjectServer;
import com.tc.objectserver.mgmt.ObjectStatsRecorder;
import com.tc.operatorevent.TerracottaOperatorEventHistoryProvider;
import com.tc.properties.TCProperties;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.servlets.L1ReconnectPropertiesServlet;
import com.tc.statistics.StatisticsGathererSubSystem;
import com.tc.statistics.beans.StatisticsMBeanNames;
import com.tc.statistics.beans.impl.StatisticsLocalGathererMBeanImpl;
import com.tc.stats.DSO;
import com.tc.stats.api.DSOMBean;
import com.tc.util.Assert;
import com.tc.util.ProductInfo;
import com.tc.util.concurrent.QueueFactory;
import com.terracottatech.config.Offheap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import javax.management.InstanceAlreadyExistsException;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServer;
import javax.management.NotCompliantMBeanException;

public class TCServerImpl extends SEDA implements TCServer {

  public static final String                VERSION_SERVLET_PATH                         = "/version";
  public static final String                CONFIG_SERVLET_PATH                          = "/config";
  public static final String                GROUP_INFO_SERVLET_PATH                      = "/groupinfo";
  public static final String                GROUPID_MAP_SERVLET_PATH                     = "/groupidmap";
  public static final String                STATISTICS_GATHERER_SERVLET_PREFIX           = "/statistics-gatherer";
  public static final String                STATISTICS_GATHERER_SERVLET_PATH             = STATISTICS_GATHERER_SERVLET_PREFIX
                                                                                           + "/*";
  public static final String                L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH = "/l1reconnectproperties";

  public static final String                HTTP_AUTHENTICATION_ROLE_STATISTICS          = "statistics";

  public static final String                HTTP_SECURITY_ROLE                           = "terracotta";

  private static final TCLogger             logger                                       = TCLogging
                                                                                             .getLogger(TCServer.class);
  private static final TCLogger             consoleLogger                                = CustomerLogging
                                                                                             .getConsoleLogger();

  private volatile long                     startTime                                    = -1;
  private volatile long                     activateTime                                 = -1;

  protected DistributedObjectServer         dsoServer;
  private Server                            httpServer;
  private TerracottaConnector               terracottaConnector;
  private StatisticsGathererSubSystem       statisticsGathererSubSystem;

  private final Object                      stateLock                                    = new Object();
  private final L2State                     state                                        = new L2State();

  private final L2ConfigurationSetupManager configurationSetupManager;
  protected final ConnectionPolicy          connectionPolicy;
  protected final TCSecurityManager         securityManager;
  private boolean                           shutdown                                     = false;

  /**
   * This should only be used for tests.
   */
  public TCServerImpl(final L2ConfigurationSetupManager configurationSetupManager) {
    this(configurationSetupManager, new TCThreadGroup(new ThrowableHandler(TCLogging.getLogger(TCServer.class))));
  }

  public TCServerImpl(final L2ConfigurationSetupManager configurationSetupManager, final TCThreadGroup threadGroup) {
    this(configurationSetupManager, threadGroup, new ConnectionPolicyImpl(Integer.MAX_VALUE));
  }

  public TCServerImpl(final L2ConfigurationSetupManager manager, final TCThreadGroup group,
                      final ConnectionPolicy connectionPolicy) {
    super(group, QueueFactory.LINKED_BLOCKING_QUEUE);

    this.connectionPolicy = connectionPolicy;
    Assert.assertNotNull(manager);
    validateEnterpriseFeatures(manager);
    this.configurationSetupManager = manager;

    if (configurationSetupManager.isSecure()) {
      this.securityManager = createSecurityManager(configurationSetupManager.getSecurity());
    } else {
      this.securityManager = null;
    }

    this.statisticsGathererSubSystem = new StatisticsGathererSubSystem();
    if (!this.statisticsGathererSubSystem.setup(manager.commonl2Config())) {
      notifyShutdown();
      throw new RuntimeException("Unable to setup StatisticsGathererSubSystem");
    }
  }

  protected TCSecurityManager createSecurityManager(final SecurityConfig securityConfig) {
    throw new UnsupportedOperationException("Only Terracotta EE supports the security feature, "
                                            + "you're currently running an OS version");
  }

  private void validateEnterpriseFeatures(final L2ConfigurationSetupManager manager) {
    if (!LicenseManager.enterpriseEdition()) return;
    if (manager.dsoL2Config().getPersistence().isSetOffheap()) {
      Offheap offHeapConfig = manager.dsoL2Config().getPersistence().getOffheap();
      LicenseManager.verifyServerArrayOffheapCapability(offHeapConfig.getMaxDataSize());
    }
    if (manager.commonl2Config().authentication()) {
      LicenseManager.verifyAuthenticationCapability();
    }
  }

  private static OrderedGroupIDs createOrderedGroupIds(ActiveServerGroupConfig[] groupArray) {
    GroupID[] gids = new GroupID[groupArray.length];
    for (int i = 0; i < groupArray.length; i++) {
      gids[i] = groupArray[i].getGroupId();
    }
    return new OrderedGroupIDs(gids);
  }

  @Override
  public ServerGroupInfo[] serverGroups() {
    L2Info[] l2Infos = infoForAllL2s();
    ActiveServerGroupConfig[] groupArray = this.configurationSetupManager.activeServerGroupsConfig()
        .getActiveServerGroupArray();
    OrderedGroupIDs orderedGroupsIds = createOrderedGroupIds(groupArray);
    GroupID coordinatorId = orderedGroupsIds.getActiveCoordinatorGroup();
    ServerGroupInfo[] result = new ServerGroupInfo[groupArray.length];
    for (int i = 0; i < groupArray.length; i++) {
      ActiveServerGroupConfig groupInfo = groupArray[i];
      GroupID groupId = groupInfo.getGroupId();
      List<L2Info> memberList = new ArrayList<L2Info>();
      for (L2Info l2Info : l2Infos) {
        if (groupInfo.isMember(l2Info.name())) {
          memberList.add(l2Info);
        }
      }
      result[i] = new ServerGroupInfo(memberList.toArray(new L2Info[0]), groupInfo.getGroupName(), groupId.toInt(),
                                      coordinatorId.equals(groupId));
    }
    return result;
  }

  @Override
  public L2Info[] infoForAllL2s() {
    String[] allKnownL2s = this.configurationSetupManager.allCurrentlyKnownServers();
    L2Info[] out = new L2Info[allKnownL2s.length];

    for (int i = 0; i < out.length; ++i) {
      try {
        CommonL2Config config = this.configurationSetupManager.commonL2ConfigFor(allKnownL2s[i]);

        String name = allKnownL2s[i];
        if (name == null) {
          name = L2Info.IMPLICIT_L2_NAME;
        }

        String host = config.jmxPort().getBind();
        if (TCSocketAddress.WILDCARD_IP.equals(host)) {
          host = config.host();
        }
        if (StringUtils.isBlank(host)) {
          host = name;
        }

        out[i] = new L2Info(name, host, config.jmxPort().getIntValue());
      } catch (ConfigurationSetupException cse) {
        throw Assert.failure("This should be impossible here", cse);
      }
    }

    return out;
  }

  @Override
  public String getL2Identifier() {
    return configurationSetupManager.getL2Identifier();
  }

  @Override
  public String getDescriptionOfCapabilities() {
    if (ProductInfo.getInstance().isEnterprise()) {
      return LicenseManager.licensedCapabilities();
    } else {
      return "Open source capabilities";
    }
  }

  /**
   * I realize this is wrong, since the server can still be starting but we'll have to deal with the whole stopping
   * issue later, and there's the TCStop feature which should be removed.
   */
  @Override
  public void stop() {
    synchronized (this.stateLock) {
      if (!this.state.isStartState()) {
        stopServer();
        logger.info("Server stopped.");
      } else {
        logger.warn("Server in incorrect state (" + this.state.getState() + ") to be stopped.");
      }
    }

  }

  @Override
  public void start() {
    synchronized (this.stateLock) {
      if (this.state.isStartState()) {
        try {
          startServer();
        } catch (Throwable t) {
          if (t instanceof RuntimeException) { throw (RuntimeException) t; }
          throw new RuntimeException(t);
        }
      } else {
        logger.warn("Server in incorrect state (" + this.state.getState() + ") to be started.");
      }
    }
  }

  @Override
  public boolean canShutdown() {
    return (!this.state.isStartState() || (this.dsoServer != null && this.dsoServer.isBlocking()))
           && !this.state.isStopState();
  }

  @Override
  public synchronized void shutdown() {
    if (canShutdown()) {
      this.state.setState(StateManager.STOP_STATE);
      consoleLogger.info("Server exiting...");
      notifyShutdown();
      Runtime.getRuntime().exit(0);
    } else {
      logger.warn("Server in incorrect state (" + this.state.getState() + ") to be shutdown.");
    }
  }

  @Override
  public long getStartTime() {
    return this.startTime;
  }

  @Override
  public void updateActivateTime() {
    if (this.activateTime == -1) {
      this.activateTime = System.currentTimeMillis();
    }
  }

  @Override
  public long getActivateTime() {
    return this.activateTime;
  }

  @Override
  public boolean isGarbageCollectionEnabled() {
    return this.configurationSetupManager.dsoL2Config().garbageCollection().getEnabled();
  }

  @Override
  public int getGarbageCollectionInterval() {
    return this.configurationSetupManager.dsoL2Config().garbageCollection().getInterval();
  }

  @Override
  public String getConfig() {
    try {
      InputStream is = this.configurationSetupManager.rawConfigFile();
      return IOUtils.toString(is);
    } catch (IOException ioe) {
      return ioe.getLocalizedMessage();
    }
  }

  @Override
  public String getPersistenceMode() {
    return this.configurationSetupManager.dsoL2Config().getPersistence().getMode().toString();
  }

  @Override
  public String getFailoverMode() {
    HaConfigSchema haConfig = this.configurationSetupManager.haConfig();
    return haConfig.getHa().getMode().toString();
  }

  @Override
  public int getDSOListenPort() {
    if (this.dsoServer != null) { return this.dsoServer.getListenPort(); }
    throw new IllegalStateException("DSO Server not running");
  }

  @Override
  public int getDSOGroupPort() {
    if (this.dsoServer != null) { return this.dsoServer.getGroupPort(); }
    throw new IllegalStateException("DSO Server not running");
  }

  public DistributedObjectServer getDSOServer() {
    return this.dsoServer;
  }

  @Override
  public boolean isStarted() {
    return !this.state.isStartState();
  }

  @Override
  public boolean isActive() {
    return this.state.isActiveCoordinator();
  }

  @Override
  public boolean isStopped() {
    // XXX:: introduce a new state when stop is officially supported.
    return this.state.isStartState();
  }

  @Override
  public String toString() {
    StringBuffer buf = new StringBuffer();
    buf.append("Server: ").append(super.toString()).append("\n");
    if (isActive()) {
      buf.append("Active since ").append(new Date(getStartTime())).append("\n");
    } else if (isStarted()) {
      buf.append("Started at ").append(new Date(getStartTime())).append("\n");
    } else {
      buf.append("Server is stopped").append("\n");
    }

    return buf.toString();
  }

  private void stopServer() {
    // XXX: I have no idea if order of operations is correct here?

    if (logger.isDebugEnabled()) {
      consoleLogger.debug("Stopping TC server...");
    }

    if (this.statisticsGathererSubSystem != null) {
      try {
        this.statisticsGathererSubSystem.cleanup();
      } catch (Exception e) {
        logger.error("Error shutting down statistics gatherer", e);
      } finally {
        this.statisticsGathererSubSystem = null;
      }
    }

    if (this.terracottaConnector != null) {
      try {
        this.terracottaConnector.shutdown();
      } catch (Exception e) {
        logger.error("Error shutting down terracotta connector", e);
      } finally {
        this.terracottaConnector = null;
      }
    }

    try {
      getStageManager().stopAll();
    } catch (Exception e) {
      logger.error("Error shutting down stage manager", e);
    }

    if (this.httpServer != null) {
      if (logger.isDebugEnabled()) {
        logger.debug("Shutting down HTTP server...");
      }

      try {
        this.httpServer.stop();
      } catch (Exception e) {
        logger.error("Error shutting down HTTP server", e);
      } finally {
        this.httpServer = null;
      }
    }

    // this stops the jmx server then dso server
    if (this.dsoServer != null) {
      try {
        this.dsoServer.quickStop();
      } catch (Exception e) {
        logger.error("Error shutting down DSO server", e);
      } finally {
        this.dsoServer = null;
      }
    }

  }

  private class StartAction implements StartupAction {
    @Override
    public void execute() throws Throwable {
      if (logger.isDebugEnabled()) {
        logger.debug("Starting Terracotta server instance...");
      }

      TCServerImpl.this.startTime = System.currentTimeMillis();

      CommonL2Config commonL2Config = TCServerImpl.this.configurationSetupManager.commonl2Config();

      if (Runtime.getRuntime().maxMemory() != Long.MAX_VALUE) {
        consoleLogger.info("Available Max Runtime Memory: " + (Runtime.getRuntime().maxMemory() / 1024 / 1024) + "MB");
      }

      TCServerImpl.this.terracottaConnector = new TerracottaConnector(
                                                                      TCServerImpl.this.configurationSetupManager
                                                                          .getSecurity() != null);
      startHTTPServer(commonL2Config, TCServerImpl.this.terracottaConnector);

      Stage stage = getStageManager().createStage("dso-http-bridge",
                                                  new HttpConnectionHandler(TCServerImpl.this.terracottaConnector), 1,
                                                  100);
      stage.start(new NullContext(getStageManager()));

      // the following code starts the jmx server as well
      startDSOServer(stage.getSink());

      if (isActive()) {
        updateActivateTime();
        if (TCServerImpl.this.activationListener != null) {
          TCServerImpl.this.activationListener.serverActivated();
        }
      }

      if (updateCheckEnabled()) {
        UpdateCheckAction.start(TCServerImpl.this, updateCheckPeriodDays());
      }

      String l2Identifier = TCServerImpl.this.configurationSetupManager.getL2Identifier();
      if (l2Identifier != null) {
        logger.info("Server started as " + l2Identifier);
      }
    }
  }

  private boolean updateCheckEnabled() {
    String s = System.getenv("TC_UPDATE_CHECK_ENABLED");
    boolean checkEnabled = (s == null) || Boolean.parseBoolean(s);
    return checkEnabled && this.configurationSetupManager.updateCheckConfig().getUpdateCheck().getEnabled();
  }

  private int updateCheckPeriodDays() {
    return this.configurationSetupManager.updateCheckConfig().getUpdateCheck().getPeriodDays();
  }

  protected void startServer() throws Exception {
    new StartupHelper(getThreadGroup(), new StartAction()).startUp();
  }

  private void startDSOServer(final Sink httpSink) throws Exception {
    Assert.assertTrue(this.state.isStartState());
    TCProperties tcProps = TCPropertiesImpl.getProperties();
    ObjectStatsRecorder objectStatsRecorder = new ObjectStatsRecorder(
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_FAULT_LOGGING_ENABLED),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_REQUEST_LOGGING_ENABLED),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_FLUSH_LOGGING_ENABLED),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_TRANSACTIONMANAGER_LOGGING_PRINT_BROADCAST_STATS),
                                                                      tcProps
                                                                          .getBoolean(TCPropertiesConsts.L2_OBJECTMANAGER_PERSISTOR_LOGGING_ENABLED));

    this.dsoServer = createDistributedObjectServer(this.configurationSetupManager, this.connectionPolicy, httpSink,
                                                   new TCServerInfo(this, this.state, objectStatsRecorder),
                                                   objectStatsRecorder, this.state, this);
    this.dsoServer.start();
    registerDSOServer();
  }

  protected DistributedObjectServer createDistributedObjectServer(L2ConfigurationSetupManager configSetupManager,
                                                                  ConnectionPolicy policy, Sink httpSink,
                                                                  TCServerInfo serverInfo,
                                                                  ObjectStatsRecorder objectStatsRecorder,
                                                                  L2State l2State, TCServerImpl serverImpl) {
    return new DistributedObjectServer(configSetupManager, getThreadGroup(), policy, httpSink, serverInfo,
                                       objectStatsRecorder, l2State, this, this, securityManager);
  }

  private void startHTTPServer(final CommonL2Config commonL2Config, final TerracottaConnector tcConnector)
      throws Exception {
    this.httpServer = new Server();
    this.httpServer.setSendServerVersion(false);
    this.httpServer.addConnector(tcConnector);

    Context context = new Context(null, "/", Context.NO_SESSIONS | Context.SECURITY);

    if (commonL2Config.isSecure()) {
      final String pathSpec = "/*";
      final TCUserRealm userRealm = new TCUserRealm(securityManager);
      setupBasicAuth(context, pathSpec, userRealm, HTTP_SECURITY_ROLE);
      logger.info("HTTPS Authentication enabled for path '" + pathSpec + "'");
    } else if (commonL2Config.httpAuthentication()) {
      final HashUserRealm userRealm = new HashUserRealm("Terracotta Statistics Gatherer",
                                                        commonL2Config.httpAuthenticationUserRealmFile());
      setupBasicAuth(context, STATISTICS_GATHERER_SERVLET_PATH, userRealm, HTTP_AUTHENTICATION_ROLE_STATISTICS);
      logger.info("HTTP Authentication enabled for path '" + STATISTICS_GATHERER_SERVLET_PATH
                  + "', using user realm file '" + commonL2Config.httpAuthenticationUserRealmFile() + "'");
    }

    context.setAttribute(ConfigServlet.CONFIG_ATTRIBUTE, this.configurationSetupManager);
    context.setAttribute(GroupInfoServlet.GROUP_INFO_ATTRIBUTE, this.configurationSetupManager);
    context.setAttribute(GroupIDMapServlet.GROUPID_MAP_ATTRIBUTE, this.configurationSetupManager);

    final boolean cvtRestEnabled = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.CVT_REST_INTERFACE_ENABLED, true);
    if (cvtRestEnabled) {
      context.setAttribute(StatisticsGathererServlet.GATHERER_ATTRIBUTE, this.statisticsGathererSubSystem);
    }

    ServletHandler servletHandler = new ServletHandler();

    /**
     * We usually don't serve up any files, just hook in a few servlets. The ResourceBase can't be null though.
     */
    File tcInstallDir;
    try {
      tcInstallDir = Directories.getInstallationRoot();
    } catch (FileNotFoundException e) {
      // if an error occurs during the retrieval of the installation root, just set it to null
      // so that the fallback mechanism can be used
      tcInstallDir = null;
    }
    File userDir = new File(System.getProperty("user.dir"));
    boolean tcInstallDirValid = false;
    File resourceBaseDir = userDir;
    if (tcInstallDir != null && tcInstallDir.exists() && tcInstallDir.isDirectory() && tcInstallDir.canRead()) {
      tcInstallDirValid = true;
      resourceBaseDir = tcInstallDir;
    }
    context.setResourceBase(resourceBaseDir.getAbsolutePath());

    createAndAddServlet(servletHandler, VersionServlet.class.getName(), VERSION_SERVLET_PATH);
    createAndAddServlet(servletHandler, ConfigServlet.class.getName(), CONFIG_SERVLET_PATH);
    createAndAddServlet(servletHandler, GroupInfoServlet.class.getName(), GROUP_INFO_SERVLET_PATH);
    createAndAddServlet(servletHandler, GroupIDMapServlet.class.getName(), GROUPID_MAP_SERVLET_PATH);

    if (cvtRestEnabled) {
      createAndAddServlet(servletHandler, StatisticsGathererServlet.class.getName(), STATISTICS_GATHERER_SERVLET_PATH);
    }
    createAndAddServlet(servletHandler, L1ReconnectPropertiesServlet.class.getName(),
                        L1_RECONNECT_PROPERTIES_FROML2_SERVELET_PATH);

    if (TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ENABLED, false)) {
      if (!tcInstallDirValid) {
        String msg = "Default HTTP servlet with file serving NOT enabled because the '"
                     + Directories.TC_INSTALL_ROOT_PROPERTY_NAME + "' system property is invalid.";
        consoleLogger.warn(msg);
        logger.warn(msg);
      } else {
        boolean aliases = TCPropertiesImpl.getProperties()
            .getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ATTRIBUTE_ALIASES, false);
        boolean dirallowed = TCPropertiesImpl.getProperties()
            .getBoolean(TCPropertiesConsts.HTTP_DEFAULT_SERVLET_ATTRIBUTE_DIR_ALLOWED, false);
        context.setAttribute("aliases", aliases);
        context.setAttribute("dirAllowed", dirallowed);
        createAndAddServlet(servletHandler, DefaultServlet.class.getName(), "/");
        String msg = "Default HTTP servlet with file serving enabled for '" + resourceBaseDir.getCanonicalPath()
                     + "' (aliases = '" + aliases + "', dirallowed = '" + dirallowed + "')";
        consoleLogger.info(msg);
        logger.info(msg);
      }
    }

    context.setServletHandler(servletHandler);
    this.httpServer.addHandler(context);

    try {
      this.httpServer.start();
    } catch (Exception e) {
      consoleLogger.warn("Couldn't start HTTP server", e);
      throw e;
    }
  }

  private void setupBasicAuth(final Context context, final String pathSpec, final UserRealm userRealm, String... roles) {
    Constraint constraint = new Constraint();
    constraint.setName(Constraint.__BASIC_AUTH);
    constraint.setRoles(roles);
    constraint.setAuthenticate(true);

    ConstraintMapping cm = new ConstraintMapping();
    cm.setConstraint(constraint);
    cm.setPathSpec(pathSpec);

    SecurityHandler sh = new SecurityHandler();
    sh.setUserRealm(userRealm);
    sh.setConstraintMappings(new ConstraintMapping[] { cm });

    context.addHandler(sh);
  }

  private static void createAndAddServlet(final ServletHandler servletHandler, final String servletClassName,
                                          final String path) {
    ServletHolder holder = servletHandler.addServletWithMapping(servletClassName, path);
    holder.setInitParameter("scratchdir", "jsp"); // avoid jetty from creating a "jsp" directory
    servletHandler.addServlet(holder);
  }

  @Override
  public void dump() {
    if (this.dsoServer != null) {
      this.dsoServer.dump();
    }
  }

  private void registerDSOServer() throws InstanceAlreadyExistsException, MBeanRegistrationException,
      NotCompliantMBeanException, NullPointerException {

    ServerManagementContext mgmtContext = this.dsoServer.getManagementContext();
    ServerConfigurationContext configContext = this.dsoServer.getContext();
    MBeanServer mBeanServer = this.dsoServer.getMBeanServer();
    registerDSOMBeans(mgmtContext, configContext, mBeanServer);
    mBeanServer.registerMBean(mgmtContext.getDSOAppEventsMBean(), L2MBeanNames.DSO_APP_EVENTS);
    StatisticsLocalGathererMBeanImpl local_gatherer = new StatisticsLocalGathererMBeanImpl(
                                                                                           this.statisticsGathererSubSystem,
                                                                                           this.configurationSetupManager
                                                                                               .commonl2Config(),
                                                                                           this.configurationSetupManager
                                                                                               .dsoL2Config());
    mBeanServer.registerMBean(local_gatherer, StatisticsMBeanNames.STATISTICS_GATHERER);
  }

  protected void registerDSOMBeans(ServerManagementContext mgmtContext, ServerConfigurationContext configContext,
                                   MBeanServer mBeanServer) throws NotCompliantMBeanException,
      InstanceAlreadyExistsException, MBeanRegistrationException {
    GCStatsEventPublisher gcStatsPublisher = this.dsoServer.getGcStatsEventPublisher();
    TerracottaOperatorEventHistoryProvider operatorEventHistoryProvider = this.dsoServer
        .getOperatorEventsHistoryProvider();
    DSOMBean dso = new DSO(mgmtContext, configContext, mBeanServer, gcStatsPublisher, operatorEventHistoryProvider,
                           this.dsoServer.getOffheapStats());
    mBeanServer.registerMBean(dso, L2MBeanNames.DSO);
  }

  // TODO: check that this is not needed then remove
  private TCServerActivationListener activationListener;

  public void setActivationListener(final TCServerActivationListener listener) {
    this.activationListener = listener;
  }

  private static class NullContext implements ConfigurationContext {

    private final StageManager manager;

    public NullContext(final StageManager manager) {
      this.manager = manager;
    }

    @Override
    public TCLogger getLogger(final Class clazz) {
      return TCLogging.getLogger(clazz);
    }

    @Override
    public Stage getStage(final String name) {
      return this.manager.getStage(name);
    }

  }

  @Override
  public void startBeanShell(final int port) {
    if (this.dsoServer != null) {
      this.dsoServer.startBeanShell(port);
    }
  }

  private synchronized void notifyShutdown() {
    shutdown = true;
    notifyAll();
  }

  @Override
  public synchronized void waitUntilShutdown() {
    while (!shutdown) {
      try {
        wait();
      } catch (InterruptedException e) {
        throw new AssertionError(e);
      }
    }
  }

  @Override
  public void reloadConfiguration() throws ConfigurationSetupException {
    dsoServer.reloadConfiguration();
  }

  @Override
  public String[] processArguments() {
    return configurationSetupManager.processArguments();
  }

  @Override
  public void dumpClusterState() {
    if (this.dsoServer != null) {
      this.dsoServer.dumpClusterState();
    }
  }

  @Override
  public boolean isProduction() {
    ConfigurationModel configurationModel = configurationSetupManager.systemConfig().configurationModel();
    return configurationModel.equals(ConfigurationModel.PRODUCTION);
  }
}

class TCUserRealm implements UserRealm {

  private final ConcurrentHashMap<String, Principal> users = new ConcurrentHashMap<String, Principal>();
  private final TCSecurityManager                    securityManager;

  TCUserRealm(final TCSecurityManager securityManager) {
    this.securityManager = securityManager;
  }

  @Override
  public String getName() {
    return this.getClass().getSimpleName();
  }

  @Override
  public Principal getPrincipal(final String username) {
    return users.get(username);
  }

  @Override
  public Principal authenticate(final String username, final Object credentials, final Request request) {
    final Principal authenticatedUser = securityManager.authenticate(username, ((String) credentials).toCharArray());
    if (authenticatedUser != null) {
      users.put(authenticatedUser.getName(), authenticatedUser);
    }
    return authenticatedUser;
  }

  @Override
  public boolean reauthenticate(final Principal user) {
    return true;
  }

  @Override
  public boolean isUserInRole(final Principal user, final String role) {
    return user != null && TCServerImpl.HTTP_SECURITY_ROLE.equals(role);
  }

  @Override
  public void disassociate(final Principal user) {
    //
  }

  @Override
  public Principal pushRole(final Principal user, final String role) {
    return user;
  }

  @Override
  public Principal popRole(final Principal user) {
    return user;
  }

  @Override
  public void logout(final Principal user) {
    users.remove(user.getName());
  }
}
