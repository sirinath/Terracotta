/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.util;

import java.io.Serializable;

public class State implements Serializable {
  private final String name;

  public State(String name) {
    Assert.assertNotNull(name);
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public boolean equals(Object o) {
    if (!(o instanceof State)) { return false; }
    return name.equals(((State) o).name);
  }

  @Override
  public int hashCode() {
    return name.hashCode();
  }

  @Override
  public String toString() {
    return "State[ " + this.name + " ]";
  }
}
