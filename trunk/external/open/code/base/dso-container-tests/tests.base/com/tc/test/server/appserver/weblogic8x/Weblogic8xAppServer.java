/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.test.server.appserver.weblogic8x;

import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.weblogic.WebLogic8xInstalledLocalContainer;
import org.codehaus.cargo.container.weblogic.WebLogicPropertySet;

import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.cargo.CargoAppServer;

import java.io.File;

/**
 * Weblogic8x AppServer implementation
 */
public final class Weblogic8xAppServer extends CargoAppServer {

  public Weblogic8xAppServer(Weblogic8xAppServerInstallation installation) {
    super(installation);
  }
  
  protected String cargoServerKey() {
    return "weblogic8x";
  }

  protected InstalledLocalContainer container(LocalConfiguration config) {
    return new WebLogic8xInstalledLocalContainer(config);
  }
  
  protected void setExtraClasspath(AppServerParameters params) {
    container().setExtraClasspath(params.classpath().split(String.valueOf(File.pathSeparatorChar)));
  }
  
  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    config.setProperty(WebLogicPropertySet.DOMAIN, "domain");
  }
}