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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.terracotta.management.resource.ResponseEntityV2;

import com.terracotta.management.resource.ClientEntityV2;
import com.terracotta.management.service.RemoteAgentBridgeService;

import java.util.HashMap;
import java.util.HashSet;

import junit.framework.TestCase;

public class L1AgentIdRetrievalServiceImplV2Test extends TestCase {

  public void testGetAgentIdFromRemoteAddress() throws Exception {
    final String expectedAgentId = "localhost_4343";

    RemoteAgentBridgeService remoteAgentBridgeService =  mock(RemoteAgentBridgeService.class);
    HashSet<String> remoteAgentNodeNames = new HashSet<String>() {{
      add(expectedAgentId);
    }};
    when(remoteAgentBridgeService.getRemoteAgentNodeNames()).thenReturn(remoteAgentNodeNames);
    HashMap<String, String> nodeDetails = new HashMap<String, String>();
    nodeDetails.put("ClientUUIDs", "aa,bb");
    when(remoteAgentBridgeService.getRemoteAgentNodeDetails(expectedAgentId)).thenReturn(nodeDetails);

    ClientManagementServiceV2 clientManagementService = mock(ClientManagementServiceV2.class);
    ResponseEntityV2<ClientEntityV2> clientEntityV2ResponseEntityV2 = new ResponseEntityV2<ClientEntityV2>();
    ClientEntityV2 clientEntityV2AA = new ClientEntityV2();
    clientEntityV2AA.getAttributes().put("RemoteAddress", "localhost:888");
    clientEntityV2AA.getAttributes().put("ClientUUID", "aa");
    ClientEntityV2 clientEntityV2BB = new ClientEntityV2();
    clientEntityV2BB.getAttributes().put("RemoteAddress", "localhost:777");
    clientEntityV2BB.getAttributes().put("ClientUUID", "bb");
    clientEntityV2ResponseEntityV2.getEntities().add(clientEntityV2BB);
    when(clientManagementService.getClients(null, null)).thenReturn(clientEntityV2ResponseEntityV2);

    L1AgentIdRetrievalServiceImplV2 l1AgentIdRetrievalServiceImplV2 =  new L1AgentIdRetrievalServiceImplV2(remoteAgentBridgeService, clientManagementService);
    String agentIdFromRemoteAddress = l1AgentIdRetrievalServiceImplV2.getAgentIdFromRemoteAddress("localhost_777", null);

    assertEquals(expectedAgentId,agentIdFromRemoteAddress);
  }

  public void testGetAgentIdFromRemoteAddress__noClient() throws Exception {
    ClientManagementServiceV2 clientManagementService = mock(ClientManagementServiceV2.class);
    ResponseEntityV2<ClientEntityV2> clientEntityV2ResponseEntityV2 = new ResponseEntityV2<ClientEntityV2>();
    ClientEntityV2 clientEntityV2AA = new ClientEntityV2();
    clientEntityV2AA.getAttributes().put("RemoteAddress", "localhost:888");
    clientEntityV2AA.getAttributes().put("ClientUUID", "aa");
    ClientEntityV2 clientEntityV2BB = new ClientEntityV2();
    clientEntityV2BB.getAttributes().put("RemoteAddress", "localhost:777");
    clientEntityV2BB.getAttributes().put("ClientUUID", "bb");
    clientEntityV2ResponseEntityV2.getEntities().add(clientEntityV2BB);
    when(clientManagementService.getClients(null, null)).thenReturn(clientEntityV2ResponseEntityV2);

    L1AgentIdRetrievalServiceImplV2 l1AgentIdRetrievalServiceImplV2 =  new L1AgentIdRetrievalServiceImplV2(null, clientManagementService);
    String agentIdFromRemoteAddress = l1AgentIdRetrievalServiceImplV2.getAgentIdFromRemoteAddress("localhost_999", null);

    assertEquals("localhost_999",agentIdFromRemoteAddress);
  }

}