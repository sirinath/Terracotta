/*
 * All content copyright (c) 2003-2009 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema.messaging.http;

import org.apache.commons.io.IOUtils;
import org.terracotta.groupConfigForL1.ServerGroup;
import org.terracotta.groupConfigForL1.ServerGroupsDocument;
import org.terracotta.groupConfigForL1.ServerGroupsDocument.ServerGroups;
import org.terracotta.groupConfigForL1.ServerInfo;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.L2ConfigurationSetupManager;
import com.tc.object.config.schema.L2DSOConfig;
import com.terracottatech.config.BindPort;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class GroupInfoServlet extends HttpServlet {
  public static final String                   GROUP_INFO_ATTRIBUTE = GroupInfoServlet.class.getName() + ".groupinfo";

  private volatile L2ConfigurationSetupManager configSetupManager;
  private ServerGroupsDocument                 serverGroupsDocument = null;
  private Map<String, Integer>                 serverNameToDsoPort;
  private Map<String, String>                  serverNameToHostName;

  @Override
  public void init() {
    // NO OP
  }

  private void createDocumentToSend() {
    configSetupManager = getConfigurationManager();
    serverGroupsDocument = ServerGroupsDocument.Factory.newInstance();
    createServerNameToDsoPortAndHostname();
    ServerGroups serverGroups = serverGroupsDocument.addNewServerGroups();
    ActiveServerGroupConfig[] activeServerGroupConfigs = configSetupManager.activeServerGroupsConfig()
        .getActiveServerGroupArray();
    for (ActiveServerGroupConfig activeServerGroupConfig : activeServerGroupConfigs) {
      addServerGroup(serverGroups, activeServerGroupConfig);
    }
  }

  protected L2ConfigurationSetupManager getConfigurationManager() {
    return (L2ConfigurationSetupManager) getServletContext().getAttribute(GROUP_INFO_ATTRIBUTE);
  }

  private void createServerNameToDsoPortAndHostname() {
    serverNameToDsoPort = new HashMap<String, Integer>();
    serverNameToHostName = new HashMap<String, String>();
    String[] allServerNames = configSetupManager.allCurrentlyKnownServers();
    for (String allServerName : allServerNames) {
      int port = 0;
      String host = null;
      try {
        L2DSOConfig dsoL2Config = configSetupManager.dsoL2ConfigFor(allServerName);
        BindPort tsaPort = dsoL2Config.dsoPort();
        port = tsaPort.getIntValue();
        if (tsaPort.isSetBind() && !tsaPort.getBind().equals("0.0.0.0")) {
          host = tsaPort.getBind();
        } else {
          host = dsoL2Config.host();
        }
      } catch (ConfigurationSetupException e) {
        throw new RuntimeException(e);
      }
      serverNameToDsoPort.put(allServerName, port);
      serverNameToHostName.put(allServerName, host);
    }
  }

  protected void addServerGroup(ServerGroups serverGroups, ActiveServerGroupConfig activeServerGroupConfig) {
    ServerGroup group = serverGroups.addNewServerGroup();
    group.setGroupName(activeServerGroupConfig.getGroupName());
    String[] members = activeServerGroupConfig.getMembers().getMemberArray();
    for (String member : members) {
      ServerInfo serverInfo = group.addNewServerInfo();
      serverInfo.setDsoPort(new BigInteger(serverNameToDsoPort.get(member).intValue() + ""));
      serverInfo.setName(serverNameToHostName.get(member));
    }
  }

  @Override
  protected synchronized void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    createDocumentToSend();
    OutputStream out = getOutPutStream(response);
    IOUtils.copy(this.serverGroupsDocument.newInputStream(), out);
    response.flushBuffer();
  }

  protected OutputStream getOutPutStream(HttpServletResponse response) throws IOException {
    return response.getOutputStream();
  }
}
