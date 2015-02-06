/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.config;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.ActiveServerGroupsConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.license.LicenseManager;
import com.tc.net.GroupID;
import com.tc.net.OrderedGroupIDs;
import com.tc.net.TCSocketAddress;
import com.tc.net.groups.Node;
import com.tc.net.groups.ServerGroup;
import com.tc.object.config.schema.L2DSOConfig;
import com.tc.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HaConfigImpl implements HaConfig {

  private final L2ConfigurationSetupManager configSetupManager;
  private final GroupID[]                   groupIDs;
  private final GroupID                     thisGroupID;
  private final GroupID                     activeCoordinatorGroupID;

  private final NodesStoreImpl              nodeStore;
  private final ServerGroup[]               groups;
  private final Node                        thisNode;
  private final ServerGroup                 thisGroup;

  public HaConfigImpl(L2ConfigurationSetupManager configSetupManager) {
    this.configSetupManager = configSetupManager;
    ActiveServerGroupsConfig groupsConfig = this.configSetupManager.activeServerGroupsConfig();
    int groupCount = groupsConfig.getActiveServerGroupCount();
    if (isActiveActive()) {
      LicenseManager.verifyServerStripingCapability();
    }
    this.groups = new ServerGroup[groupCount];
    this.groupIDs = new GroupID[groupCount];
    for (int i = 0; i < groupCount; i++) {
      this.groups[i] = new ServerGroup(groupsConfig.getActiveServerGroups().get(i));
      this.groupIDs[i] = groups[i].getGroupId();
    }

    this.activeCoordinatorGroupID = new OrderedGroupIDs(groupIDs).getActiveCoordinatorGroup();

    Set<Node> nodes = makeAllNodes();
    this.thisNode = makeThisNode();
    this.thisGroup = getThisGroupFrom(this.groups, this.configSetupManager.getActiveServerGroupForThisL2());
    this.thisGroupID = thisGroup.getGroupId();

    this.nodeStore = new NodesStoreImpl(nodes, getNodeNamesForThisGroup(), buildServerGroupIDMap(),
                                        configSetupManager);
  }

  private HashMap<String, GroupID> buildServerGroupIDMap() {
    HashMap<String, GroupID> tempMap = new HashMap<String, GroupID>();
    for (ServerGroup group : groups) {
      for (Node node : group.getNodes()) {
        tempMap.put(node.getServerNodeName(), group.getGroupId());
      }
    }
    return tempMap;
  }

  private Set<String> getNodeNamesForThisGroup() {
    Set<String> tmpSet = new HashSet<String>();
    for (Node n : thisGroup.getNodes()) {
      tmpSet.add(n.getServerNodeName());
    }
    return tmpSet;
  }

  /**
   * @throws ConfigurationSetupException
   */
  @Override
  public ReloadConfigChangeContext reloadConfiguration() throws ConfigurationSetupException {
    ActiveServerGroupsConfig asgsc = this.configSetupManager.activeServerGroupsConfig();
    int grpCount = asgsc.getActiveServerGroupCount();

    ReloadConfigChangeContext context = new ReloadConfigChangeContext();

    List<ActiveServerGroupConfig> asgcArray = asgsc.getActiveServerGroups();
    for (int i = 0; i < grpCount; i++) {
      GroupID gid = asgcArray.get(i).getGroupId();
      for (int j = 0; j < grpCount; j++) {
        if (groups[i].getGroupId().equals(gid)) {
          ReloadConfigChangeContext tempContext = groups[i].reloadGroup(this.configSetupManager, asgcArray.get(i));
          context.update(tempContext);

          nodeStore.updateServerNames(tempContext, gid);

          if (groups[i].getGroupId().equals(thisGroupID)) {
            updateNamesForThisGroup(tempContext);
          }
        }
      }
    }

    nodeStore.topologyChanged(context);
    return context;
  }

  private void updateNamesForThisGroup(ReloadConfigChangeContext context) {
    nodeStore.updateServerNames(context);
  }

  private ServerGroup getThisGroupFrom(ServerGroup[] sg, ActiveServerGroupConfig activeServerGroupForThisL2) {
    for (ServerGroup element : sg) {
      if (element.getGroupId() == activeServerGroupForThisL2.getGroupId()) { return element; }
    }
    throw new RuntimeException("Unable to find this group information for " + this.thisNode + " "
                               + activeServerGroupForThisL2);
  }

  @Override
  public boolean isActiveActive() {
    return this.configSetupManager.activeServerGroupsConfig().getActiveServerGroupCount() > 1;
  }

  @Override
  public GroupID getActiveCoordinatorGroupID() {
    return this.activeCoordinatorGroupID;
  }

  @Override
  public GroupID getThisGroupID() {
    return this.thisGroupID;
  }

  @Override
  public GroupID[] getGroupIDs() {
    return this.groupIDs;
  }

  private Set<Node> makeAllNodes() {
    Set allClusterNodes = new HashSet();
    List<ActiveServerGroupConfig> asgcs = this.configSetupManager.activeServerGroupsConfig().getActiveServerGroups();
    for (ActiveServerGroupConfig asgc : asgcs) {
      Assert.assertNotNull(asgc);
      String[] l2Names = asgc.getMembers();
      for (String l2Name : l2Names) {
        try {
          L2DSOConfig l2 = this.configSetupManager.dsoL2ConfigFor(l2Name);
          Node node = makeNode(l2);
          allClusterNodes.add(node);
          addNodeToGroup(node, l2Name);
        } catch (ConfigurationSetupException e) {
          throw new RuntimeException("Error getting l2 config for: " + l2Name, e);
        }
      }
    }
    return allClusterNodes;
  }

  // servers and groups were checked in configSetupManger
  private void addNodeToGroup(Node node, String serverName) {
    boolean added = false;
    for (ServerGroup group : this.groups) {
      if (group.hasMember(serverName)) {
        group.addNode(node, serverName);
        added = true;
      }
    }
    if (!added) { throw new AssertionError("Node=[" + node + "] with serverName=[" + serverName
                                           + "] was not added to any group!"); }
  }

  @Override
  public Node getThisNode() {
    return this.thisNode;
  }

  private Node makeThisNode() {
    L2DSOConfig l2 = this.configSetupManager.dsoL2Config();
    return makeNode(l2);
  }

  public static Node makeNode(L2DSOConfig l2) {
    String host = l2.tsaGroupPort().getBind();
    if (TCSocketAddress.WILDCARD_IP.equals(host)) {
      host = l2.host();
    }
    return new Node(host, l2.tsaPort().getIntValue(), l2.tsaGroupPort().getIntValue());
  }

  @Override
  public boolean isActiveCoordinatorGroup() {
    return this.thisGroupID.equals(this.activeCoordinatorGroupID);
  }

  @Override
  public ClusterInfo getClusterInfo() {
    return nodeStore;
  }

  @Override
  public NodesStore getNodesStore() {
    return nodeStore;
  }

  @Override
  public String getNodeName(String member) {
    for (ServerGroup group : this.groups) {
      if (group.hasMember(member)) { return group.getNode(member).getServerNodeName(); }
    }
    return null;
  }

}
