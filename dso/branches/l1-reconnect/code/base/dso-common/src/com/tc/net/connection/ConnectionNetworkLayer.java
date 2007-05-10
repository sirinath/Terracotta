/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.connection;

import com.tc.net.core.event.TCConnectionEventListener;
import com.tc.net.protocol.NetworkLayer;

public interface ConnectionNetworkLayer extends NetworkLayer {
  public void addListener(TCConnectionEventListener listener);

  public void removeListener(TCConnectionEventListener listener);
}
