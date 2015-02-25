/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.servermap.localcache.impl;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;

public class L1ServerMapTransactionCompletionHandler extends AbstractEventHandler {
  @Override
  public void handleEvent(EventContext context) {
    if (context instanceof RunnableEventContext) {
      ((RunnableEventContext) context).getRunnable().run();
    } else {
    L1ServerMapLocalStoreTransactionCompletionListener txnListener = (L1ServerMapLocalStoreTransactionCompletionListener) context;
      txnListener.postTransactionCallback();
    }
  }

  public static class RunnableEventContext implements EventContext {
    private final Runnable runnable;

    public RunnableEventContext(Runnable runnable) {
      this.runnable = runnable;
    }

    public Runnable getRunnable() {
      return runnable;
    }
  }
}
