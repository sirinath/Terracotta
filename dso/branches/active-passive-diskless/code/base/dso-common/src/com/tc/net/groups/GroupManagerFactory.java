/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.util.runtime.Vm;

import java.lang.reflect.Constructor;

public class GroupManagerFactory {

  public static GroupManager createGroupManager() throws GroupException {
    // Using reflection to avoid weird 1.4 / 1.5 project dependency issues !!
    //TODO:: Move to isTCK15compliant one trunk is merged
    if(Vm.isJDK15()) {
      return createTribesGroupManager();
    } else {
      return new SingleNodeGroupManager();
    }
  }
  
  private static GroupManager createTribesGroupManager() throws GroupException {
    try {
      Class clazz = Class.forName("com.tc.net.groups.TribesGroupManager");
      Constructor constructor = clazz.getConstructor(new Class[0]);
      return (GroupManager) constructor.newInstance(new Object[0]);
    } catch (Exception e) {
      throw new GroupException(e);
    }
  }
}
