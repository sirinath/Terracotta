/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.object.tx.TransactionID;

public interface TransactionBatchManager {

  public void defineBatch(ChannelID channelID, int numTxns) throws BatchDefinedException;

  public boolean batchComponentComplete(ChannelID channelID, TransactionID txnID)
      throws NoSuchBatchException;

  public void shutdownClient(ChannelID channelID);
}
