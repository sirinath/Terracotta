/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;

public class CallbackDumpAdapter implements CallbackOnExitHandler {

  private DumpHandler         dumpHandler;
  private CallbackOnExitActionState actionState = new CallbackOnExitActionState();

  public CallbackDumpAdapter(DumpHandler dumpHandler) {
    this.dumpHandler = dumpHandler;
  }

  public void callbackOnExit() {
    dumpHandler.dumpToLogger();
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
