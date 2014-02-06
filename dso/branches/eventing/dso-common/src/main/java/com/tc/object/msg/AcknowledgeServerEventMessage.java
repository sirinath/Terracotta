/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.gtx.GlobalTransactionID;

import java.util.Set;

public interface AcknowledgeServerEventMessage extends TCMessage {

  public void initialize(Set<GlobalTransactionID> acknowledgedGtxIds);

  public Set<GlobalTransactionID> getAcknowledgedGtxIds();

}
