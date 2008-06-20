/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.exception.ImplementMe;

public class CallbackStartupExceptionLoggingAdapter implements CallbackOnExitHandler {

  private String              extraMessage;
  private CallbackOnExitActionState actionState = new CallbackOnExitActionState();

  public CallbackStartupExceptionLoggingAdapter() {
    this("");
  }

  public CallbackStartupExceptionLoggingAdapter(String extraMessage) {
    this.extraMessage = extraMessage;
  }

  public void callbackOnExit() {
    throw new ImplementMe();
  }

  public void callbackOnExit(Throwable t) {
    System.err.println("");
    System.err.println("");
    System.err.println("Fatal Terracotta startup exception:");
    System.err.println("");
    System.err.println(" " + t.getMessage() + extraMessage);
    System.err.println("");
    System.err.println("Server startup failed.");
    actionState.actionSuccess();
  }

  public CallbackOnExitActionState getCallbackOnExitActionState() {
    return this.actionState;
  }

  public boolean isRestartNeeded() {
    return false;
  }
}