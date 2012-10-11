/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.activepassive;

import com.tctest.TestState;

public class ActivePassiveServerCrasher implements Runnable {
  private static boolean                   DEBUG      = true;
  private final ActivePassiveServerManager serverManger;
  private final long                       serverCrashWaitTimeInSec;
  private final long                       serverCrashInitialDelaySeconds;
  private final int                        maxCrashCount;

  private int                              crashCount = 0;
  private final TestState                  testState;
  private volatile boolean                 done;

  public ActivePassiveServerCrasher(ActivePassiveServerManager serverManager, long serverCrashWaitTimeInSec,
                                    long serverCrashInitialDelaySeconds, int maxCrashCount, TestState testState) {
    this.serverManger = serverManager;
    this.serverCrashWaitTimeInSec = serverCrashWaitTimeInSec;
    this.serverCrashInitialDelaySeconds = serverCrashInitialDelaySeconds;
    this.maxCrashCount = maxCrashCount;
    this.testState = testState;
  }

  public void run() {

    sleep(serverCrashInitialDelaySeconds * 1000);

    while (!done) {
      sleep(serverCrashWaitTimeInSec * 1000);

      synchronized (testState) {
        if (testState.isRunning() && (maxCrashCount - crashCount) > 0 && serverManger.getErrors().isEmpty() && !done) {
          try {
            debugPrintln("***** ActivePassiveServerCrasher:  about to crash server  threadID=["
                         + Thread.currentThread().getName() + "]");
            serverManger.crashServer();

            debugPrintln("***** ActivePassiveServerCrasher:  about to restart crashed server threadID=["
                         + Thread.currentThread().getName() + "]");
            serverManger.restartLastCrashedServer();

            crashCount++;
          } catch (Exception e) {
            debugPrintln("***** ActivePassiveServerCrasher:  error occured while crashing/restarting server  threadID=["
                         + Thread.currentThread().getName() + "]");

            e.printStackTrace();

            serverManger.storeErrors(e);
          }
        } else {
          debugPrintln("***** ActivePassiveServerCrasher is done: testStateRunning[" + testState.isRunning()
                       + "] errors[" + serverManger.getErrors().size() + "] crashCount[" + crashCount + "]");
          break;
        }
      }
    }
  }

  private void sleep(long millis) {
    debugPrintln("***** ActivePassiveServerCrasher:  Sleeping for " + millis + " millis,  threadID=["
                 + Thread.currentThread().getName() + "]");
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e1) {
      serverManger.storeErrors(e1);
    }
  }

  public void debugPrintln(String s) {
    if (DEBUG) {
      System.err.println(s);
    }
  }

  public synchronized void stop() {
    this.done = true;
  }
}
