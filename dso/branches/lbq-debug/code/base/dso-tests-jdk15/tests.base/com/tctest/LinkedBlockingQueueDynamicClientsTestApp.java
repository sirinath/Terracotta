/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest;

import org.apache.commons.io.FileUtils;

import com.tc.object.config.ConfigVisitor;
import com.tc.object.config.DSOClientConfigHelper;
import com.tc.object.config.TransparencyClassSpec;
import com.tc.objectserver.control.ExtraL1ProcessControl;
import com.tc.simulator.app.ApplicationConfig;
import com.tc.simulator.listener.ListenerProvider;
import com.tc.util.DebugUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class LinkedBlockingQueueDynamicClientsTestApp extends ServerCrashingAppBase {
  private final Random              r     = new Random();
  private final LinkedBlockingQueue queue = new LinkedBlockingQueue(100);
  private CyclicBarrier       barrier;

  public LinkedBlockingQueueDynamicClientsTestApp() {
    super();
  }

  public LinkedBlockingQueueDynamicClientsTestApp(String appId, ApplicationConfig cfg, ListenerProvider listenerProvider) {
    super(appId, cfg, listenerProvider);
    barrier = new CyclicBarrier(getParticipantCount());
  }
  
  public static void visitL1DSOConfig(ConfigVisitor visitor, DSOClientConfigHelper config) {
    String testClassName = LinkedBlockingQueueDynamicClientsTestApp.class.getName();
    TransparencyClassSpec spec = config.getOrCreateSpec(testClassName);

    String methodExpression = "* " + testClassName + "*.*(..)";
    config.addWriteAutolock(methodExpression);
    spec.addRoot("queue", "queue");
    spec.addRoot("barrier", "barrier");
  }

  public void runTest() throws Throwable {
    int index = barrier.await();

    if (index == 0) {
      doPut();
      barrier.await();
    } else {
      doSpawnAndDrain();
      barrier.await();
    }
  }

  private void doPut() throws Throwable {
    while (true) {
      queue.put(r.nextInt());
    }
  }

  private void doSpawnAndDrain() throws Throwable {
    Thread dataVerifier = new Thread(new Runnable() {
      public void run() {
        while (true) {
          try {
            queue.drainTo(new ArrayList(), 10);
            Thread.sleep(1000);
          } catch (Exception e) {
            throw new AssertionError(e);
          }
        }
      }
    });

    Thread spawner = new Thread(new Runnable() {
      public void run() {
        while (true) {
          try {
            spawnNewClient("0", LinkedBlockingQueueDynamicClientsTestApp.class, new String[] {});
            System.err.println("SpawnNewClient returned");
            Thread.sleep(1000);
          } catch (Exception e) {
            e.printStackTrace(System.err);
            throw new AssertionError(e);
          }
        }
      }
    });

    dataVerifier.start();
    spawner.start();

    dataVerifier.join();
    spawner.join();
  }

  protected ExtraL1ProcessControl spawnNewClient(String clientId, Class clientClass, String[] mainArgs)
      throws Exception {
    final String hostName = getHostName();
    final int port = getPort();
    final File configFile = new File(getConfigFilePath());
    File workingDir = new File(configFile.getParentFile(), "client-" + clientId);
    FileUtils.forceMkdir(workingDir);

    List jvmArgs = new ArrayList();
    addTestTcPropertiesFile(jvmArgs);
    ExtraL1ProcessControl client = new ExtraL1ProcessControl(hostName, port, clientClass, configFile.getAbsolutePath(),
                                                             mainArgs, workingDir, jvmArgs);
    client.start();
    client.mergeSTDERR();
    client.mergeSTDOUT();
    int returnCode = client.waitFor();
    System.err.println("\n### Started New Client");
    if (returnCode != 0) {
      throw new AssertionError("Non-zero return code from spawning new client.");
    }
    return client;
  }

  public static void main(String args[]) throws Exception {
    DebugUtil.DEBUG = true;

    LinkedBlockingQueueDynamicClientsTestApp l1 = new LinkedBlockingQueueDynamicClientsTestApp();
    l1.doConsume();

    DebugUtil.DEBUG = false;
  }

  public void doConsume() throws Exception {
    long start = System.currentTimeMillis();
    boolean timeout = false;
    while (!timeout) {
      queue.take();
      timeout = (System.currentTimeMillis() - start) > 30000;
    }
    System.err.println("Spawned client finished");
  }

}
