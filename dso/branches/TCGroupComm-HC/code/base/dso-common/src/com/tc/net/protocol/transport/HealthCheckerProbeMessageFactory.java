/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;

public interface HealthCheckerProbeMessageFactory {
  public HealthCheckerProbeMessage createPing(ConnectionID connectionId, TCConnection source);

  public HealthCheckerProbeMessage createDummyPing(ConnectionID connectionId, TCConnection source);

  public HealthCheckerProbeMessage createPingReply(ConnectionID connectionId, TCConnection source);

}
