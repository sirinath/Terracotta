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
package com.terracotta.management.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.service.L1AgentIdRetrievalServiceV2;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * @author Anthony Dahanne
 */
public class L1AgentIdRetrievalServiceImplV2 implements L1AgentIdRetrievalServiceV2 {

  private static final Logger LOG = LoggerFactory.getLogger(L1AgentIdRetrievalServiceImplV2.class);
  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final ClientManagementServiceV2 clientManagementService;


  public L1AgentIdRetrievalServiceImplV2(RemoteAgentBridgeService remoteAgentBridgeService, ClientManagementServiceV2 clientManagementService) {
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.clientManagementService = clientManagementService;
  }

  @Override
  public String getAgentIdFromRemoteAddress(String remoteAddress, String clientID) throws ServiceExecutionException {
    //by default, keep the remoteAddress as the agentId
    String agentId = remoteAddress;
    Set<String> clientIDSet = (clientID != null) ? Collections.singleton(clientID) : null;

    remoteAddress = remoteAddress.replaceAll("_",":");

    ResponseEntityV2<ClientEntityV2> clients = clientManagementService.getClients(clientIDSet, null);
    Collection<ClientEntityV2> entities = clients.getEntities();
    String clientUUID = null;
    for (ClientEntityV2 entity : entities) {
      if (entity.getAttributes().get("RemoteAddress").equals(remoteAddress)) {
        clientUUID = (String) entity.getAttributes().get("ClientUUID");
        break;
      }
    }
    if (clientUUID == null) {
      String clientIDPart = clientID != null ? (", ClientID[" + clientID + "]") : null;
      LOG.warn("Could not determine clientUUID for remoteAddress " + remoteAddress + clientIDPart);
    } else {
      Set<String> remoteAgentNodeNames = remoteAgentBridgeService.getRemoteAgentNodeNames();
      for (String remoteAgentNodeName : remoteAgentNodeNames) {

        Map<String, String> remoteAgentNodeDetails = remoteAgentBridgeService.getRemoteAgentNodeDetails(remoteAgentNodeName);
        String clientUUIDs = remoteAgentNodeDetails.get("ClientUUIDs");
        if (clientUUIDs != null) {
          String[] split = clientUUIDs.split(",");
          for (String s : split) {
            if (s.equals(clientUUID)) {
              agentId = remoteAgentNodeName;
              break;
            }
          }
        }
      }
    }
    return agentId;
  }

}
