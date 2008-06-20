/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import com.tc.util.State;

public class CallbackOnExitActionState {

  private static final State CALLBACK_ACTION_STATE_NOTRUN  = new State("CALLBACK_ACTION_STATE_NOTRUN");
  private static final State CALLBACK_ACTION_STATE_SUCCESS = new State("CALLBACK_ACTION_STATE_SUCCESS");
  private static final State CALLBACK_ACTION_STATE_FAILURE = new State("CALLBACK_ACTION_STATE_FAILURE");

  private State              state;

  public CallbackOnExitActionState() {
    this.state = CALLBACK_ACTION_STATE_NOTRUN;
  }

  public void actionSuccess() {
    this.state = CALLBACK_ACTION_STATE_SUCCESS;
  }

  public void actionFailure() {
    this.state = CALLBACK_ACTION_STATE_FAILURE;
  }

  public boolean isSuccess() {
    return (this.state == CALLBACK_ACTION_STATE_SUCCESS);
  }

  public boolean isActionRun() {
    return (!(this.state == CALLBACK_ACTION_STATE_NOTRUN));
  }
}