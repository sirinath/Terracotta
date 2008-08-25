/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test;

import com.tc.test.proxyconnect.ProxyConnectManager;

import java.util.List;

public interface MultipleServerManager {

  public ProxyConnectManager[] getL2ProxyManagers();

  public List getErrors();

  public void stopAllServers() throws Exception;

  public void dumpAllServers(int currentPid, int dumpCount, long dumpInterval) throws Exception;

  public boolean crashActiveServersAfterMutate();

  public void crashActiveServers() throws Exception;

}