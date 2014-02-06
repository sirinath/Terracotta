/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.NodeID;

public interface AcknowledgeServerEventMessageFactory {

  public AcknowledgeServerEventMessage newAcknowledgeServerEventMessage(final NodeID remoteNode);

}
