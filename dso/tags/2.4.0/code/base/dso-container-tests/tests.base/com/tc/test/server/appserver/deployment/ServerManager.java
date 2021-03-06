/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.deployment;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import junit.framework.Assert;

public class ServerManager {

  protected static TCLogger      logger         = TCLogging.getLogger(ServerManager.class);
  private static int             appServerIndex = 0;

  private List                   serversToStop  = new ArrayList();
  private DSOServer              dsoServer;

  private final TestConfigObject config;
  private NewAppServerFactory    factory;
  private AppServerInstallation  installation;
  private File                   sandbox;
  private File                   tempDir;
  private File                   installDir;
  private File                   warDir;

  public ServerManager(final Class testClass) throws Exception {
    PropertiesHackForRunningInEclipse.initializePropertiesWhenRunningInEclipse();
    config = TestConfigObject.getInstance();
    factory = NewAppServerFactory.createFactoryFromProperties(config);
    installDir = config.appserverServerInstallDir();
    tempDir = TempDirectoryUtil.getTempDirectory(testClass);    
    sandbox = AppServerUtil.createSandbox(tempDir);
    warDir = new File(sandbox, "war");
    installation = AppServerUtil.createAppServerInstallation(factory, installDir, sandbox);
  }

  public void addServerToStop(Stoppable stoppable) {
    getServersToStop().add(0, stoppable);
  }

  void stop() {
    logger.info("Stopping all servers");
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!stoppable.isStopped()) {
          logger.debug("About to stop server: " + stoppable.toString());
          stoppable.stop();
        }
      } catch (Exception e) {
        logger.error(stoppable, e);
      }
    }

    AppServerUtil.shutdownAndArchive(sandbox, new File(tempDir, "sandbox"));
  }

  protected boolean cleanTempDir() {
    return true;
  }

  void start(boolean withPersistentStore) throws Exception {
    startDSO(withPersistentStore);
  }

  private void startDSO(boolean withPersistentStore) throws Exception {
    dsoServer = new DSOServer(withPersistentStore, tempDir);
    logger.debug("Starting DSO server with sandbox: " + sandbox.getAbsolutePath());
    dsoServer.start();
    addServerToStop(dsoServer);
  }

  public void restartDSO(boolean withPersistentStore) throws Exception {
    logger.debug("Restarting DSO server : " + dsoServer);
    dsoServer.stop();
    startDSO(withPersistentStore);
  }

  public WebApplicationServer makeWebApplicationServer(String tcConfigPath) throws Exception {
    return makeWebApplicationServer(getTcConfigFile(tcConfigPath));
  }

  // public AbstractDBServer makeDBServer(String dbType, String dbName, int serverPort) {
  // // XXX this should use server factory
  // AbstractDBServer svr = new HSqlDBServer(dbName, serverPort);
  // this.addServerToStop(svr);
  // return svr;
  // }

  public FileSystemPath getTcConfigFile(String tcConfigPath) {
    URL url = getClass().getResource(tcConfigPath);
    Assert.assertNotNull("could not find: " + tcConfigPath, url);
    Assert.assertTrue("should be file:" + url.toString(), url.toString().startsWith("file:"));
    FileSystemPath pathToTcConfigFile = FileSystemPath.makeExistingFile(url.toString().substring("file:".length()));
    return pathToTcConfigFile;
  }

  public WebApplicationServer makeWebApplicationServer(FileSystemPath tcConfigPath) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer appServer = new GenericServer(config, factory, installation, tcConfigPath, i, tempDir);
    addServerToStop(appServer);
    return appServer;
  }

  public WebApplicationServer makeWebApplicationServer(StandardTerracottaAppServerConfig tcConfig) throws Exception {
    int i = ServerManager.appServerIndex++;

    WebApplicationServer tomcatServer = new GenericServer(config, factory, installation, tcConfig, i, tempDir);
    addServerToStop(tomcatServer);
    return tomcatServer;
  }

  void setServersToStop(List serversToStop) {
    this.serversToStop = serversToStop;
  }

  List getServersToStop() {
    return serversToStop;
  }

  public DeploymentBuilder makeDeploymentBuilder(String warFileName) {
    return new WARBuilder(warFileName, warDir, config);
  }

  public DeploymentBuilder makeDeploymentBuilder() throws IOException {
    return new WARBuilder(warDir, config);
  }

  public StandardTerracottaAppServerConfig getConfig() {
    return factory.createTcConfig(installation.dataDirectory());
  }

  public void stopAllWebServers() {
    for (Iterator it = getServersToStop().iterator(); it.hasNext();) {
      Stoppable stoppable = (Stoppable) it.next();
      try {
        if (!(stoppable instanceof DSOServer || stoppable.isStopped())) stoppable.stop();
      } catch (Exception e) {
        logger.error("Unable to stop server: " + stoppable, e);
      }
    }
  }

  public TestConfigObject getTestConfig() {
    return this.config;
  }

  public File getSandbox() {
    return sandbox;
  }

  public File getTempDir() {
    return tempDir;
  }
}
