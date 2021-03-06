/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.cargo;

import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.ContainerType;
import org.codehaus.cargo.container.InstalledLocalContainer;
import org.codehaus.cargo.container.State;
import org.codehaus.cargo.container.configuration.ConfigurationType;
import org.codehaus.cargo.container.configuration.LocalConfiguration;
import org.codehaus.cargo.container.deployable.WAR;
import org.codehaus.cargo.container.property.GeneralPropertySet;
import org.codehaus.cargo.container.property.ServletPropertySet;
import org.codehaus.cargo.generic.configuration.ConfigurationFactory;
import org.codehaus.cargo.generic.configuration.DefaultConfigurationFactory;

import com.tc.lcp.HeartBeatService;
import com.tc.test.server.ServerParameters;
import com.tc.test.server.ServerResult;
import com.tc.test.server.appserver.AbstractAppServer;
import com.tc.test.server.appserver.AppServer;
import com.tc.test.server.appserver.AppServerInstallation;
import com.tc.test.server.appserver.AppServerParameters;
import com.tc.test.server.appserver.AppServerResult;
import com.tc.test.server.appserver.StandardAppServerParameters;
import com.tc.test.server.appserver.cargo.CargoJava.Link;
import com.tc.test.server.util.AppServerUtil;

import java.io.File;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Generic {@link AppServer} implementation which delegates to the Codehaus CARGO package
 * http://docs.codehaus.org/display/CARGO/Home
 */
public abstract class CargoAppServer extends AbstractAppServer {

  private InstalledLocalContainer container;
  private int                     port;
  private int                     linkedPort;

  // System property used by cargo. This String is referenced in the CARGO patch, therefore it must not be changed
  public static final String      CARGO_JAVA       = "cargo_java";
  public static final String      CARGO_JAVA_CLASS = CargoJava.class.getName();

  static {
    System.setProperty(CARGO_JAVA, CARGO_JAVA_CLASS);
  }

  public CargoAppServer(AppServerInstallation installation) {
    super(installation);
  }

  public final ServerResult start(ServerParameters rawParams) throws Exception {
    StandardAppServerParameters params = (StandardAppServerParameters) rawParams;

    adjustParams(params);

    port = AppServerUtil.getPort();
    File instance = createInstance(params);
    setProperties(params, port, instance);

    ConfigurationFactory factory = new DefaultConfigurationFactory();
    LocalConfiguration config = (LocalConfiguration) factory.createConfiguration(cargoServerKey(),
                                                                                 ContainerType.INSTALLED,
                                                                                 ConfigurationType.STANDALONE, instance
                                                                                     .getAbsolutePath());
    setConfigProperties(config);
    config.setProperty(ServletPropertySet.PORT, Integer.toString(port));
    config.setProperty(GeneralPropertySet.JVMARGS, params.jvmArgs());
    config.setProperty(GeneralPropertySet.LOGGING, "low");
    addWars(config, params.wars(), params.instanceName());

    container = container(config, params);
    container.setTimeout(8 * 60 * 1000);
    container.setHome(serverInstallDirectory().getAbsolutePath());
    container.setLogger(new ConsoleLogger(params.instanceName()));
    setExtraClasspath(params);

    linkJavaProcess(instance);

    container.start();

    return new AppServerResult(port, this);
  }

  protected void adjustParams(StandardAppServerParameters params) throws Exception {
    // override if desired
  }

  public final void stop() {
    if (container != null) {
      if (container.getState().equals(State.STARTED) || container.getState().equals(State.STARTING)
          || container.getState().equals(State.UNKNOWN)) {
        try {
          // XXX: clear out the jvmargs so that the VMs spawned for stop() don't try to use DSO
          // XXX: If you know a better way to do this, go for it
          String jvmArgs = container.getConfiguration().getPropertyValue(GeneralPropertySet.JVMARGS);
          try {
            container.getConfiguration().setProperty(GeneralPropertySet.JVMARGS, null);
            container.stop(); // NOTE: stop is not guaranteed to work
          } finally {
            container.getConfiguration().setProperty(GeneralPropertySet.JVMARGS, jvmArgs);
          }
        } catch (ContainerException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private void addWars(LocalConfiguration config, Map wars, String instanceName) {
    WAR warapp = null;
    for (Iterator it = wars.entrySet().iterator(); it.hasNext();) {
      Map.Entry entry = (Entry) it.next();
      String context = (String) entry.getKey();
      File warFile = (File) entry.getValue();
      System.out.println("WAR: " + warFile);
      warapp = new WAR(warFile.getPath());
      warapp.setContext(context);
      warapp.setLogger(new ConsoleLogger(instanceName));
      config.addDeployable(warapp);
    }
  }

  /**
   * Create a linked java process {@link LinkedJavaProcessPollingAgent}
   * 
   * @throws InterruptedException
   */
  private void linkJavaProcess(File instance) throws InterruptedException {
    linkedPort = HeartBeatService.listenPort();
    Link.put(new CargoJava.Args(linkedPort, instance));
  }

  protected final InstalledLocalContainer container() {
    return container;
  }

  protected abstract String cargoServerKey();

  protected abstract InstalledLocalContainer container(LocalConfiguration config, AppServerParameters params);

  protected void setConfigProperties(LocalConfiguration config) throws Exception {
    // do nothing
  }

  protected void setExtraClasspath(AppServerParameters params) {
    // do nothing
  }

}
