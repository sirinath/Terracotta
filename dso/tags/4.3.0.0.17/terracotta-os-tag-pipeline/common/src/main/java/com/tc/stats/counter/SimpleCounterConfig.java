/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.stats.counter;

public class SimpleCounterConfig implements CounterConfig {

  private final long initialValue;

  public SimpleCounterConfig(long initialValue) {
    this.initialValue = initialValue;
  }

  @Override
  public long getInitialValue() {
    return initialValue;
  }

  @Override
  public Counter createCounter() {
    return new CounterImpl(initialValue);
  }
}
