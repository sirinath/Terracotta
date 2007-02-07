/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.EventHandlerException;
import com.tc.object.dmi.DmiDescriptor;

public class DmiHandler extends AbstractEventHandler {

  public void handleEvent(EventContext context) throws EventHandlerException {
    DmiDescriptor dd = (DmiDescriptor) context;
    // do it, baby!
    
  }

}
