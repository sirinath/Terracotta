/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;

public class CallbackZapServerNodeAdapter implements CallbackOnExitHandler {
  private TCLogger                  consoleLogger;
  private CallbackOnExitActionState callbackExitActionState = new CallbackOnExitActionState();

  public CallbackZapServerNodeAdapter(TCLogger consoleLogger) {
    this.consoleLogger = consoleLogger;
  }

  public void callbackOnExit() {
    throw new ImplementMe();
  }

  public void callbackOnExit(Throwable t) {
    consoleLogger.warn(t.getMessage());
    // if any cleanup actions before restart, do it here based on the Zap reason type
    callbackExitActionState.actionSuccess();
  }

  public CallbackOnExitActionState getCallbackOnExitActionState() {
    return callbackExitActionState;
  }

  public boolean isRestartNeeded() {
    return true;
  }

}
