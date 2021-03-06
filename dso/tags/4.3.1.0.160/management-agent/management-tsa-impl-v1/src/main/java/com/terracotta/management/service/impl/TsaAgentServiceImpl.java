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

import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.services.AgentService;

import com.terracotta.management.service.RemoteAgentBridgeService;
import com.terracotta.management.web.utils.TSAConfig;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Ludovic Orban
 */
public class TsaAgentServiceImpl implements AgentService {

  private static final String AGENCY = "TSA";

  private final ServerManagementService serverManagementService;
  private final RemoteAgentBridgeService remoteAgentBridgeService;
  private final AgentService l1Agent;

  public TsaAgentServiceImpl(ServerManagementService serverManagementService, RemoteAgentBridgeService remoteAgentBridgeService, AgentService l1Agent) {
    this.serverManagementService = serverManagementService;
    this.remoteAgentBridgeService = remoteAgentBridgeService;
    this.l1Agent = l1Agent;
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> ids) throws ServiceExecutionException {
    Collection<AgentEntity> agentEntities = new ArrayList<AgentEntity>();
    if (ids.isEmpty()) {
      agentEntities.add(buildAgentEntity());
      agentEntities.addAll(l1Agent.getAgents(ids));
    } else {
      Set<String> l1Nodes = null;
      Set<String> remoteIds = new HashSet<String>();
      for (String id : ids) {
        if (id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
          agentEntities.add(buildAgentEntity());
        } else {
          if (l1Nodes == null) {
            l1Nodes = remoteAgentBridgeService.getRemoteAgentNodeNames();
          }
          if (l1Nodes.contains(id)) {
            remoteIds.add(id);
          } else {
            throw new ServiceExecutionException("Unknown agent ID : " + id);
          }
        }
      }
      if (!remoteIds.isEmpty()) {
        agentEntities.addAll(l1Agent.getAgents(remoteIds));
      }
    }
    return agentEntities;
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    Collection<AgentMetadataEntity> agentMetadataEntities = new ArrayList<AgentMetadataEntity>();
    if (ids.isEmpty()) {
      AgentMetadataEntity agentMetadataEntity = buildAgentMetadata();
      agentMetadataEntities.addAll(l1Agent.getAgentsMetadata(ids));
      agentMetadataEntities.add(agentMetadataEntity);
    } else {
      Set<String> l1Nodes = null;
      Set<String> remoteIds = new HashSet<String>();
      for (String id : ids) {
        if (id.equals(AgentEntity.EMBEDDED_AGENT_ID)) {
          agentMetadataEntities.add(buildAgentMetadata());
        } else {
          if (l1Nodes == null) {
            l1Nodes = remoteAgentBridgeService.getRemoteAgentNodeNames();
          }
          if (l1Nodes.contains(id)) {
            remoteIds.add(id);
          } else {
            throw new ServiceExecutionException("Unknown agent ID : " + id);
          }
        }
      }
      if (!remoteIds.isEmpty()) {
        agentMetadataEntities.addAll(l1Agent.getAgentsMetadata(remoteIds));
      }
    }
    return agentMetadataEntities;
  }

  private AgentMetadataEntity buildAgentMetadata() throws ServiceExecutionException {
    AgentMetadataEntity ame = new AgentMetadataEntity();

    ame.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    ame.setAgencyOf(AGENCY);
    ame.setVersion(this.getClass().getPackage().getImplementationVersion());
    ame.setAvailable(true);

    ame.setSecured(TSAConfig.isSslEnabled());
    ame.setSslEnabled(TSAConfig.isSslEnabled());
    ame.setLicensed(serverManagementService.isEnterpriseEdition());
    ame.setNeedClientAuth(false);
    ame.setEnabled(true);
    ame.setRestAPIVersion("v1");

    return ame;
  }

  private AgentEntity buildAgentEntity() throws ServiceExecutionException {
    AgentEntity e = new AgentEntity();
    e.setAgentId(AgentEntity.EMBEDDED_AGENT_ID);
    e.setAgencyOf(AGENCY);
    e.setVersion(this.getClass().getPackage().getImplementationVersion());
    e.getRootRepresentables().put("urls", createL2Urls());
    return e;
  }

  private String createL2Urls() throws ServiceExecutionException {
    StringBuilder sb = new StringBuilder();

    Collection<String> l2Urls = serverManagementService.getL2Urls();
    for (String l2Url : l2Urls) {
      sb.append(l2Url).append(",");
    }
    if (sb.indexOf(",") > - 1) {
      sb.deleteCharAt(sb.length() - 1);
    }

    return sb.toString();
  }

}
