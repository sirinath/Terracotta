/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tctest.config;

import org.apache.commons.io.IOUtils;

import com.tc.config.schema.ActiveServerGroupConfig;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.FatalIllegalConfigurationChangeHandler;
import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.config.schema.setup.TestTVSConfigurationSetupManagerFactory;
import com.tc.config.schema.test.GroupConfigBuilder;
import com.tc.config.schema.test.GroupsConfigBuilder;
import com.tc.config.schema.test.HaConfigBuilder;
import com.tc.config.schema.test.L2ConfigBuilder;
import com.tc.config.schema.test.MembersConfigBuilder;
import com.tc.config.schema.test.TerracottaConfigBuilder;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;

public class ActiveActiveTcConfigTest extends TCTestCase {
  private File tcConfig = null;

  public void testParseGroupInOrder() {
    try {
      tcConfig = getTempFile("tc-config.xml");
      writeConfigFile();
      TestTVSConfigurationSetupManagerFactory factory = new TestTVSConfigurationSetupManagerFactory(
                                                                                                    TestTVSConfigurationSetupManagerFactory.MODE_CENTRALIZED_CONFIG,
                                                                                                    null,
                                                                                                    new FatalIllegalConfigurationChangeHandler());
      L2TVSConfigurationSetupManager l2TVSConfigurationSetupManager = factory
          .createL2TVSConfigurationSetupManager(tcConfig, null);
      ActiveServerGroupConfig[] activeServerGroup = l2TVSConfigurationSetupManager.activeServerGroupsConfig()
          .getActiveServerGroupArray();

      int numberOfGroups = activeServerGroup.length;
      int serverNumber = 1;
      for (int i = 0; i < numberOfGroups; i++) {
        String[] groupMembers = activeServerGroup[i].getMembers().getMemberArray();
        for (int j = 0; j < groupMembers.length; j++) {
           Assert.eval(groupMembers[j].equals("server" + serverNumber++));
        }
      }
    } catch (ConfigurationSetupException e) {
      System.out.println(e.getMessage());
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }

  private synchronized void writeConfigFile() {
    try {
      TerracottaConfigBuilder builder = createConfig();
      FileOutputStream out = new FileOutputStream(tcConfig);
      IOUtils.write(builder.toString(), out);
      out.close();
    } catch (Exception e) {
      throw Assert.failure("Can't create config file", e);
    }
  }

  public static TerracottaConfigBuilder createConfig() {
    MembersConfigBuilder[] membersConfigBuilder = new MembersConfigBuilder[3];
    membersConfigBuilder[0] = new MembersConfigBuilder();
    membersConfigBuilder[0].addMember("server1");
    membersConfigBuilder[0].addMember("server2");

    membersConfigBuilder[1] = new MembersConfigBuilder();
    membersConfigBuilder[1].addMember("server3");
    membersConfigBuilder[1].addMember("server4");
    membersConfigBuilder[1].addMember("server5");

    membersConfigBuilder[2] = new MembersConfigBuilder();
    membersConfigBuilder[2].addMember("server6");
    membersConfigBuilder[2].addMember("server7");

    HaConfigBuilder haConfigBuilder = new HaConfigBuilder();
    haConfigBuilder.setElectionTime("1000");
    haConfigBuilder.setMode("networked-active-passive");

    int len = membersConfigBuilder.length;
    GroupConfigBuilder groupConfigBuilder[] = new GroupConfigBuilder[len];
    for (int i = 0; i < len; i++) {
      groupConfigBuilder[i] = new GroupConfigBuilder();
      groupConfigBuilder[i].setMembers(membersConfigBuilder[i]);
      groupConfigBuilder[i].setId(i);
      groupConfigBuilder[i].setHa(haConfigBuilder);
    }

    GroupsConfigBuilder groupsConfigBuilder = new GroupsConfigBuilder();
    groupsConfigBuilder.addGroupConfigBuilder(groupConfigBuilder);

    TerracottaConfigBuilder out = new TerracottaConfigBuilder();
    out.getServers().setGroups(groupsConfigBuilder);
    out.getServers().getL2s()[0].setName("server1");
    out.getServers().getL2s()[0].setPersistenceMode(L2ConfigBuilder.PERSISTENCE_MODE_PERMANENT_STORE);
    return out;
  }
}
