/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol.delivery;

import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;

import java.util.LinkedList;

/**
 * 
 */
public class StateMachineRunner implements EventContext {
  private final LinkedList           events    = new LinkedList();
  private boolean                    scheduled = false;
  private final Sink                 sink;
  private final AbstractStateMachine stateMachine;

  public StateMachineRunner(AbstractStateMachine stateMachine, Sink sink) {
    this.sink = sink;
    this.stateMachine = stateMachine;
  }

  public synchronized void start() {
    stateMachine.start();
  }

  public synchronized void pause() {
    if (! stateMachine.isPaused()) {
      stateMachine.pause();
    }
  }

  public synchronized void resume() {
    stateMachine.resume();
    scheduleIfNeeded();
  }

  public void run() {
    OOOProtocolEvent pe = null;
    synchronized (this) {
      pe = (OOOProtocolEvent) events.removeFirst();
    }
    pe.execute(stateMachine);
    synchronized (this) {
      scheduled = false;
      scheduleIfNeeded();
    }
  }

  public synchronized void addEvent(OOOProtocolEvent event) {
    events.addLast(event);
    scheduleIfNeeded();
  }

  private synchronized void scheduleIfNeeded() {
    if (!scheduled && !events.isEmpty() && ! stateMachine.isPaused()) {
      scheduled = true;
      sink.add(this);
    }
  }
}