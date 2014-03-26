/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandler;
import com.tc.invalidation.Invalidations;
import com.tc.invalidation.InvalidationsProcessor;
import com.tc.object.msg.InvalidateObjectsMessage;

public class ReceiveInvalidationHandler extends AbstractEventHandler implements EventHandler {

  private final InvalidationsProcessor invalidationsProcessor;

  public ReceiveInvalidationHandler(InvalidationsProcessor invalidationsProcessor) {
    this.invalidationsProcessor = invalidationsProcessor;
  }

  @Override
  public void handleEvent(EventContext context) {
    final Invalidations invalidations;

    if (context instanceof InvalidateObjectsMessage) {
      InvalidateObjectsMessage invalidationContext = (InvalidateObjectsMessage) context;
      invalidations = invalidationContext.getObjectIDsToInvalidate();
    } else {
      InvalidatationContext invalidationContext = (InvalidatationContext) context;
      invalidations = invalidationContext.getObjectIDsToInvalidate();
    }
    invalidationsProcessor.processInvalidations(invalidations);
  }

  public static class InvalidatationContext implements EventContext {
    private final Invalidations invalidations;

    public InvalidatationContext(Invalidations invalidations) {
      this.invalidations = invalidations;
    }

    protected Invalidations getObjectIDsToInvalidate() {
      return invalidations;
    }
  }
}
