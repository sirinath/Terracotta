/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

public interface NVPair {

  public abstract String getName();

  public abstract void setName(String aName);

  public abstract String valueAsString();

  public abstract ValueType getType();

}