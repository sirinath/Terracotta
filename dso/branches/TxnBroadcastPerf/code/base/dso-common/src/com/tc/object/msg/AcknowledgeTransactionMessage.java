/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.groups.ClientID;
import com.tc.net.groups.NodeID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;

public interface AcknowledgeTransactionMessage {

  public void initialize(NodeID channelID);
  
  public void addAckMessage(TransactionID txID);

  public NodeID getRequesterID();

  public TransactionID getRequestID();
  
  public TransactionID getRequestID(int index);

  public void send();

  public ClientID getClientID();

  public SessionID getLocalSessionID();

  public int acksBatchSize();
}