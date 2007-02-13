/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.groups;

import com.tc.test.TCTestCase;

public class TribesGroupManagerTest extends TCTestCase {
  
  public void testIfTribesGroupManagerLoads() throws Exception {
    GroupManager gm = GroupManagerFactory.createGroupManager();
    assertNotNull(gm);
    assertEquals(TribesGroupManager.class.getName(), gm.getClass().getName());
  }

}
