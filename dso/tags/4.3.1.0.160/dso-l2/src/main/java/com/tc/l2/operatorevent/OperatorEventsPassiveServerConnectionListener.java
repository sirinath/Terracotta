/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.l2.operatorevent;

import com.tc.config.NodesStore;
import com.tc.management.TSAManagementEventPayload;
import com.tc.management.TerracottaRemoteManagement;
import com.tc.net.ServerID;
import com.tc.net.groups.PassiveServerListener;
import com.tc.operatorevent.TerracottaOperatorEventFactory;
import com.tc.operatorevent.TerracottaOperatorEventLogger;
import com.tc.operatorevent.TerracottaOperatorEventLogging;

public class OperatorEventsPassiveServerConnectionListener implements PassiveServerListener {

  private final TerracottaOperatorEventLogger operatorEventLogger = TerracottaOperatorEventLogging.getEventLogger();
  private final NodesStore                    nodesStore;

  public OperatorEventsPassiveServerConnectionListener(NodesStore nodesStore) {
    this.nodesStore = nodesStore;
  }


  @Override
  public void passiveServerJoined(ServerID nodeID) {
    // no-op
  }


  @Override
  public void passiveServerLeft(ServerID nodeID) {
    String serverName = nodesStore.getServerNameFromNodeName(nodeID.getName());
    if (serverName == null) {
      serverName = nodeID.getName();
    }
    TSAManagementEventPayload tsaManagementEventPayload = new TSAManagementEventPayload("TSA.TOPOLOGY.MIRROR_LEFT");
    tsaManagementEventPayload.getAttributes().put("Server.Name", serverName);
    TerracottaRemoteManagement.getRemoteManagementInstance().sendEvent(tsaManagementEventPayload.toManagementEvent());

    operatorEventLogger.fireOperatorEvent(TerracottaOperatorEventFactory.createPassiveL2DisconnectedEvent(serverName));
  }

}
