/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.config.schema;

import org.apache.xmlbeans.XmlObject;

import com.tc.config.schema.context.ConfigContext;
import com.tc.config.schema.defaults.DefaultValueProvider;
import com.tc.config.schema.repository.ChildBeanFetcher;
import com.tc.config.schema.repository.ChildBeanRepository;
import com.tc.config.schema.repository.MutableBeanRepository;
import com.tc.config.schema.setup.ConfigurationSetupException;
import com.tc.config.schema.setup.StandardL2TVSConfigurationSetupManager;
import com.terracottatech.config.ActiveServerGroup;
import com.terracottatech.config.ActiveServerGroups;
import com.terracottatech.config.Ha;

public class NewActiveServerGroupsConfigObject extends BaseNewConfigObject implements NewActiveServerGroupsConfig {
  private final NewActiveServerGroupConfig[] groupConfigArray;
  private final int                          smallestGroupId;
  private final int                          activeServerGroupCount;
  private final int[]                        activeServerGroupIds;

  public NewActiveServerGroupsConfigObject(ConfigContext context, StandardL2TVSConfigurationSetupManager setupManager)
      throws ConfigurationSetupException {
    super(context);
    context.ensureRepositoryProvides(ActiveServerGroups.class);
    final ActiveServerGroups groups = (ActiveServerGroups) context.bean();

    if (groups == null) { throw new AssertionError(
        "ActiveServerGroups is null!  This should never happen since we make sure default is used."); }

    final ActiveServerGroup[] groupArray = groups.getActiveServerGroupArray();

    if (groupArray == null || groupArray.length == 0) { throw new AssertionError(
        "ActiveServerGroup array is null!  This should never happen since we make sure default is used."); }

    this.activeServerGroupCount = groupArray.length;
    this.activeServerGroupIds = new int[groupArray.length];

    this.groupConfigArray = new NewActiveServerGroupConfig[groupArray.length];
    int smallest = Integer.MAX_VALUE;

    for (int i = 0; i < groupArray.length; i++) {
      if (!checkGroupIdUnique(groupConfigArray, groupArray[i])) { throw new ConfigurationSetupException(
          "Each active-server-group must have a unique id:  there are two or more groups defined with id{"
              + groupArray[i].getId() + "}"); }
      // if no Ha element defined for this group then set it to common ha
      if (!groupArray[i].isSetHa()) {
        groupArray[i].setHa(setupManager.getCommonHa());
      }
      this.groupConfigArray[i] = new NewActiveServerGroupConfigObject(createContext(setupManager, groupArray[i]),
          setupManager);

      this.activeServerGroupIds[i] = groupArray[i].getId();
      if (groupArray[i].getId() < smallest) {
        smallest = groupArray[i].getId();
      }
    }

    this.smallestGroupId = smallest;
  }

  public int getSmallestGroupId() {
    return this.smallestGroupId;
  }

  public int getActiveServerGroupCount() {
    return this.activeServerGroupCount;
  }

  public int[] getActiveServerGroupIds() {
    return this.activeServerGroupIds;
  }

  private boolean checkGroupIdUnique(NewActiveServerGroupConfig[] groupList, ActiveServerGroup newGroup) {
    for (int i = 0; i < groupList.length; i++) {
      if (groupList[i] != null && (groupList[i].getId() == newGroup.getId())) { return false; }
    }
    return true;
  }

  public NewActiveServerGroupConfig[] getActiveServerGroupArray() {
    return groupConfigArray;
  }

  private final ConfigContext createContext(StandardL2TVSConfigurationSetupManager setupManager,
                                            final ActiveServerGroup group) {
    ChildBeanRepository beanRepository = new ChildBeanRepository(setupManager.serversBeanRepository(),
        ActiveServerGroup.class, new ChildBeanFetcher() {
          public XmlObject getChild(XmlObject parent) {
            return group;
          }
        });
    return setupManager.createContext(beanRepository, setupManager.getConfigFilePath());
  }

  public static ActiveServerGroups getDefaultActiveServerGroups(DefaultValueProvider defaultValueProvider,
                                                                MutableBeanRepository serversBeanRepository, Ha commonHa) {
    ActiveServerGroups asgs = ActiveServerGroups.Factory.newInstance();
    ActiveServerGroup[] groupArray = new ActiveServerGroup[1];
    groupArray[0] = NewActiveServerGroupConfigObject.getDefaultActiveServerGroup(defaultValueProvider,
        serversBeanRepository, commonHa);
    asgs.setActiveServerGroupArray(groupArray);
    return asgs;
  }
}
