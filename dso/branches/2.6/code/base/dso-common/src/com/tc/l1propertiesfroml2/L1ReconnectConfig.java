/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.l1propertiesfroml2;

public interface L1ReconnectConfig {

  public boolean getReconnectEnabled();

  public int getL1ReconnectTimeout();
}
