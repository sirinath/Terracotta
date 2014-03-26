/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tctest;

import org.hyperic.sigar.NetConnection;
import org.hyperic.sigar.NetFlags;
import org.hyperic.sigar.NetInfo;
import org.hyperic.sigar.NetInterfaceConfig;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;

import com.tc.lcp.LinkedJavaProcess;
import com.tc.object.BaseDSOTestCase;
import com.tc.objectserver.control.ServerMBeanRetriever;
import com.tc.process.Exec;
import com.tc.process.Exec.Result;
import com.tc.statistics.retrieval.SigarUtil;
import com.tc.util.CallableWaiter;
import com.tc.util.TcConfigBuilder;
import com.tctest.process.ExternalDsoServer;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import junit.framework.Assert;

public class HTTPConnectionLeakTest extends BaseDSOTestCase {
  private TcConfigBuilder      configBuilder;
  private ExternalDsoServer    server;
  private int                  jmxPort;
  private int                  dsoPort;
  private ServerMBeanRetriever serverMBeanRetriever;

  @Override
  protected void setUp() throws Exception {
    configBuilder = new TcConfigBuilder("/com/tctest/tc-one-server-config.xml");
    configBuilder.randomizePorts();
    server = new ExternalDsoServer(getWorkDir("server1"), configBuilder.newInputStream(), "server1");
    jmxPort = configBuilder.getJmxPort(0);
    dsoPort = configBuilder.getDsoPort(0);
    serverMBeanRetriever = new ServerMBeanRetriever("localhost", jmxPort);
    server.start();
    System.out.println("server1 started");
    waitTillBecomeActive();
    System.out.println("server1 became active");
  }

  public void testLeak() throws Exception {
    int initialConnectionCount = getNetInfoEstablishedConnectionsCount(dsoPort);
    System.out.println("http://localhost:" + dsoPort + "/config");
    for (int i = 0; i < 20; i++) {
      fetchConfig();
    }
    int finalConnectionCount = getNetInfoEstablishedConnectionsCount(dsoPort);
    System.out.println("initialConnectioncount : " + initialConnectionCount + " finalConnectionCount : "
                       + finalConnectionCount);
    Assert.assertEquals(initialConnectionCount, finalConnectionCount);
  }

  private void fetchConfig() throws Exception {
    List<String> URLsToFetch = new ArrayList<String>();
    URLsToFetch.add("http://localhost:" + dsoPort + "/config");
    LinkedJavaProcess fetchURLProcess = new LinkedJavaProcess(URLFetcher.class.getName(), URLsToFetch, null);
    fetchURLProcess.start();
    Result result = Exec.execute(fetchURLProcess, fetchURLProcess.getCommand(), null, null, null);
    if(result.getExitCode() != 0){
 throw new AssertionError("URLFetcher Exit code is " + result.getExitCode());
    }
  }

  private int getNetInfoEstablishedConnectionsCount(int bindPort) throws SigarException {
    int establishedConnections = 0;
    SigarUtil.sigarInit();
    Sigar s = new Sigar();
    NetInfo info = s.getNetInfo();
    NetInterfaceConfig config = s.getNetInterfaceConfig(null);
    System.out.println(info.toString());
    System.out.println(config.toString());

    int flags = NetFlags.CONN_TCP | NetFlags.TCP_ESTABLISHED | NetFlags.TCP_CLOSE_WAIT;

    NetConnection[] connections = s.getNetConnectionList(flags);

    System.out.println("XXX Established connections if any");
    for (NetConnection connection : connections) {
      long port = connection.getLocalPort();
      long remotePort = connection.getRemotePort();
      // not checking bind address
      if ((bindPort == port || bindPort == remotePort)
          && (connection.getState() == NetFlags.TCP_ESTABLISHED || connection.getState() == NetFlags.TCP_CLOSE_WAIT)) {
        establishedConnections++;
        System.out.println("XXX " + connection);
      }
    }
    return establishedConnections;
  }

  private void waitTillBecomeActive() throws Exception {
    CallableWaiter.waitOnCallable(new Callable<Boolean>() {
      @Override
      public Boolean call() throws Exception {
        return serverMBeanRetriever.getTCServerInfoMBean().isActive();
      }
    });
  }

  @Override
  protected void tearDown() throws Exception {
    System.err.println("in tearDown");
    if (server != null) server.stop();
  }

  private File getWorkDir(final String subDir) throws IOException {
    File workDir = new File(getTempDirectory(), subDir);
    workDir.mkdirs();
    return workDir;
  }

  public static class URLFetcher {
    public static void main(String[] args) throws Exception {
      for (String arg : args) {
        URL configURL = new URL(arg);
        BufferedReader in = new BufferedReader(new InputStreamReader(configURL.openStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
          System.out.println(inputLine);
        in.close();
      }
    }
  }
}
