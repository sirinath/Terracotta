/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

import com.tc.cluster.DsoCluster;
import com.tc.operatorevent.NodeNameProvider;

public class ClientNameProvider implements NodeNameProvider {

  private final DsoCluster dsoCluster;

  public ClientNameProvider(DsoCluster dsoCluster) {
    this.dsoCluster = dsoCluster;
  }

  @Override
  public String getNodeName() {
    this.dsoCluster.waitUntilNodeJoinsCluster();
    return this.dsoCluster.getCurrentNode().getId();
  }

}
