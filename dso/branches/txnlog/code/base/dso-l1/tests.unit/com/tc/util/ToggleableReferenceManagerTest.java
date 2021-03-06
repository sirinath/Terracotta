/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.util;

import com.tc.object.ObjectID;
import com.tc.object.util.ToggleableStrongReference;
import com.tc.util.concurrent.ThreadUtil;

import junit.framework.TestCase;

public class ToggleableReferenceManagerTest extends TestCase {

  public void test() throws Exception {
    ToggleableReferenceManager mgr = new ToggleableReferenceManager();

    Object peer = new Object();
    mgr.start();

    ToggleableStrongReference toggleRef = mgr.getOrCreateFor(new ObjectID(1), peer);
    toggleRef.strongRef(peer);
    peer = null;

    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(1, mgr.size());
    Assert.assertEquals(0, mgr.clearCount());

    toggleRef.clearStrongRef();
    System.gc();
    ThreadUtil.reallySleep(5000);

    Assert.assertEquals(0, mgr.size());
    Assert.assertEquals(1, mgr.clearCount());
  }

}
