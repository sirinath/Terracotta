/*
 * Created on Jun 17, 2004
 */
package com.tc.async.impl;

import com.tc.async.api.ConfigurationContext;
import com.tc.async.api.Sink;
import com.tc.async.api.Stage;
import com.tc.exception.ImplementMe;

/**
 * @author orion
 */
public class MockStage implements Stage {

  private final String  name;
  public final MockSink sink;

  public MockStage(String name) {
    this.name = name;
    this.sink = new MockSink();
  }

  public void destroy() {
    //
  }

  public synchronized Sink getSink() {
    return sink;
  }

  public String getName() {
    return name;
  }

  public void start(ConfigurationContext context) {
    //
  }

  public void turnTracingOn() {
    //
  }

  public void turnTracingOff() {
    //
  }

  public void pause() {
    throw new ImplementMe();
    
  }

  public void unpause() {
    throw new ImplementMe();
    
  }
}