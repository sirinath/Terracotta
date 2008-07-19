/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.beans;

import com.tc.management.AbstractTerracottaMBean;
import com.tc.object.DistributedObjectClient;

import javax.management.NotCompliantMBeanException;

public class L1Dumper extends AbstractTerracottaMBean implements L1DumperMBean {

  private final DistributedObjectClient client;

  public L1Dumper(DistributedObjectClient client) throws NotCompliantMBeanException {
    super(L1DumperMBean.class, false);
    this.client = client;
  }

  public void reset() {
    //
  }

  public void dump() {
    client.dump();
  }

  public void startBeanShell(int port) {
    client.startBeanShell(port);
  }

}
