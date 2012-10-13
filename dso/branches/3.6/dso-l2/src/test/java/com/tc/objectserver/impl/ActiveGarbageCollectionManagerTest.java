/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.object.ObjectID;
import com.tc.objectserver.l1.impl.ClientObjectReferenceSet;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.ObjectIDSet;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Collections;

public class ActiveGarbageCollectionManagerTest extends TCTestCase {

  private Sink                     gcSink;
  private ClientObjectReferenceSet refSet;

  @Override
  public void setUp() {
    gcSink = mock(Sink.class);
    refSet = mock(ClientObjectReferenceSet.class);
    when(refSet.contains(any(ObjectID.class))).thenReturn(false);
  }

  public void testDelayedObjectRelease() {
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_INTERVAL_SECONDS, "1");
    TCPropertiesImpl.getProperties().setProperty(TCPropertiesConsts.L2_OBJECTMANAGER_DGC_INLINE_DELETE_DELAY_SECONDS, "2");
    ActiveGarbageCollectionManager gcManager = new ActiveGarbageCollectionManager(gcSink, refSet);
    ThreadUtil.reallySleep(1000);
    gcManager.deleteObjects(new ObjectIDSet(Collections.singleton(new ObjectID(1))));
    verify(gcSink).addLossy(any(EventContext.class));
    assertTrue(gcManager.nextObjectsToDelete().isEmpty());

    ThreadUtil.reallySleep(2100);

    assertTrue(gcManager.nextObjectsToDelete().isEmpty());

    ThreadUtil.reallySleep(2100);

    assertTrue(gcManager.nextObjectsToDelete().isEmpty());

    ThreadUtil.reallySleep(2100);

    assertTrue(gcManager.nextObjectsToDelete().contains(new ObjectID(1)));
  }
}
