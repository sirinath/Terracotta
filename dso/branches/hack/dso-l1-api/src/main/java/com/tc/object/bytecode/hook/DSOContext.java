/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.bytecode.hook;

import com.tc.cluster.DsoCluster;

/**
 * The idea behind DSOContext is to encapsulate a DSO "world" in a client VM. But this idea has not been fully realized.
 */
public interface DSOContext {

  public void shutdown();

  public void addTunneledMBeanDomain(String mbeanDomain);

  public void sendCurrentTunneledDomains();

  public DsoCluster getDsoCluster();

}
