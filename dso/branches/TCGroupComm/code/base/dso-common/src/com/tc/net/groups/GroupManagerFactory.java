/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.config.schema.setup.L2TVSConfigurationSetupManager;
import com.tc.lang.TCThreadGroup;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;
import com.tc.util.runtime.Vm;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.util.logging.LogManager;

public class GroupManagerFactory {
  public static final String NHA_COMM_LAYER_PROPERTY = "l2.nha.groupcomm.type";
  public static final String TC_GROUP_COMM           = "tc-group-comm";
  public static final String TRIBES_COMM             = "tribes";

  public static GroupManager createGroupManager(L2TVSConfigurationSetupManager configSetupManager,
                                                TCThreadGroup threadGroup) throws GroupException {
    // Using reflection to avoid weird 1.4 / 1.5 project dependency issues !!
    if (Vm.isJDK15Compliant()) {
      final String commLayer = TCPropertiesImpl.getProperties().getProperty(NHA_COMM_LAYER_PROPERTY);
      if (commLayer.equals(TC_GROUP_COMM)) {
        return createTCGroupManager(configSetupManager, threadGroup);
      } else if (commLayer.equals(TRIBES_COMM)) {
        return createTribesGroupManager();
      } else {
        throw new GroupException("wrong property " + NHA_COMM_LAYER_PROPERTY + " can be " + TC_GROUP_COMM + " or "
                                 + TRIBES_COMM);
      }
    } else {
      return new SingleNodeGroupManager();
    }
  }

  public static GroupManager createGroupManager() throws GroupException {
    // Using reflection to avoid weird 1.4 / 1.5 project dependency issues !!
    if (Vm.isJDK15Compliant()) {
      return createTribesGroupManager();
    } else {
      return new SingleNodeGroupManager();
    }
  }

  private static GroupManager createTribesGroupManager() throws GroupException {
    initLoggerForJuli();
    try {
      Class clazz = Class.forName("com.tc.net.groups.TribesGroupManager");
      Constructor constructor = clazz.getConstructor(new Class[0]);
      return (GroupManager) constructor.newInstance(new Object[0]);
    } catch (Exception e) {
      throw new GroupException(e);
    }
  }

  private static GroupManager createTCGroupManager(L2TVSConfigurationSetupManager configSetupManager,
                                                   TCThreadGroup threadGroup) throws GroupException {
    initLoggerForJuli();
    try {
      Class clazz = Class.forName("com.tc.net.groups.TCGroupManagerImpl");
      Class classArgs[] = new Class[] { L2TVSConfigurationSetupManager.class, TCThreadGroup.class };
      Constructor constructor = clazz.getConstructor(classArgs);
      Object objArgs[] = new Object[] { configSetupManager, threadGroup };
      return (GroupManager) constructor.newInstance(objArgs);
    } catch (Exception e) {
      throw new GroupException(e);
    }
  }

  private static void initLoggerForJuli() {
    System.setProperty("java.util.logging.config.class", LogConfig.class.getName());
  }

  public static final class LogConfig {
    public LogConfig() throws SecurityException, IOException {
      InputStream in = GroupManagerFactory.class.getResourceAsStream("/com/tc/logging/juli.properties");
      Assert.assertNotNull(in);
      LogManager.getLogManager().readConfiguration(in);
    }
  }

}
