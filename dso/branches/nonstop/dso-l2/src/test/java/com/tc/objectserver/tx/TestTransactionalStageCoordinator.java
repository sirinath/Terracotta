/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;


import com.tc.async.impl.MockSink;
import com.tc.objectserver.context.ApplyTransactionContext;
import com.tc.objectserver.context.LookupEventContext;
import com.tc.objectserver.context.RecallObjectsContext;

import java.util.Collections;

public class TestTransactionalStageCoordinator implements TransactionalStageCoordinator {

  public MockSink lookupSink        = new MockSink();
  public MockSink recallSink        = new MockSink();
  public MockSink applySink         = new MockSink();

  public void addToApplyStage(ApplyTransactionContext context) {
    applySink.add(context);
  }

  public void initiateLookup() {
    lookupSink.addLossy(new LookupEventContext());
  }

  public void initiateRecallAll() {
    recallSink.add(new RecallObjectsContext(Collections.EMPTY_LIST, true));
  }
}