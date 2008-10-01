/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.glassfishv1;

import com.tc.test.server.appserver.glassfish.AbstractGlassfishAppServer;
import com.tc.test.server.appserver.glassfish.GlassfishAppServerInstallation;

/**
 * Glassfish AppServer implementation
 */
public final class GlassfishV1AppServer extends AbstractGlassfishAppServer {

  public GlassfishV1AppServer(GlassfishAppServerInstallation installation) {
    super(installation);
  }

  protected String[] getDisplayCommand(String script) {
    return new String[] { script, "display" };
  }

}
