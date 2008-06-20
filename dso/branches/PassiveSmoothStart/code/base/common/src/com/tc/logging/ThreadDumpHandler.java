/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;
import com.tc.util.runtime.ThreadDumpUtil;

public class ThreadDumpHandler implements CallbackOnExitHandler {

  private static final TCLogger logger      = TCLogging.getLogger(ThreadDumpHandler.class);
  private CallbackOnExitActionState   actionState = new CallbackOnExitActionState();

  public void callbackOnExit() {
    logger.error(ThreadDumpUtil.getThreadDump());
    actionState.actionSuccess();
  }

  public void callbackOnExit(Throwable t) {
    throw new ImplementMe();
  }

  public CallbackOnExitActionState getCallbackOnExitActionState() {
    return this.actionState;
  }

  public boolean isRestartNeeded() {
    return false;
  }
}
