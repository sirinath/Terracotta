/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import com.tc.test.TestConfigObject;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.NewAppServerFactory;
import com.tc.test.server.appserver.war.DescriptorXml;
import com.tc.test.server.appserver.war.DtdWar;
import com.tc.test.server.appserver.war.War;
import com.tc.test.server.tcconfig.StandardTerracottaAppServerConfig;

import java.io.File;
import java.net.URL;
import java.util.Properties;

/**
 * This class creates specific implementations of return values for the given methods. To obtain an instance you must
 * call {@link NewAppServerFactory.createFactoryFromProperties()}.
 */
public final class Weblogic8xAppServerFactory extends NewAppServerFactory {

  // This class may only be instantiated by it's parent which contains the ProtectedKey
  public Weblogic8xAppServerFactory(ProtectedKey protectedKey, TestConfigObject config) {
    super(protectedKey, config);
  }

  public AppServerParameters createParameters(String instanceName, Properties props) {
    return new Weblogic8xAppServerParameters(instanceName, props, config.sessionClasspath());
  }

  public AppServer createAppServer(AppServerInstallation installation) {
    return new Weblogic8xAppServer((Weblogic8xAppServerInstallation) installation);
  }

  public AppServerInstallation createInstallation(URL host, File serverDir, File workingDir) throws Exception {
    return new Weblogic8xAppServerInstallation(host, serverDir, workingDir, config.appserverMajorVersion(), config
        .appserverMinorVersion());
  }

  public AppServerInstallation createInstallation(File home, File workingDir) throws Exception {
    return new Weblogic8xAppServerInstallation(home, workingDir, config.appserverMajorVersion(), config
        .appserverMinorVersion());
  }

  public War createWar(String appName) {
    War war = new DtdWar(appName);
    DescriptorXml weblogicWar = new Weblogic8xDescriptorXml();
    war.addContainerSpecificXml(weblogicWar.getFileName(), weblogicWar.getBytes());
    return war;
  }

  public StandardTerracottaAppServerConfig createTcConfig(File baseDir) {
    return new Weblogic8xAppServerConfig(baseDir);
  }
}
