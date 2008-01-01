/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.tcm;

import com.tc.net.core.TCConnectionManager;
import com.tc.net.groups.NodeID;
import com.tc.net.protocol.NetworkStackHarnessFactory;
import com.tc.net.protocol.transport.ConnectionPolicy;
import com.tc.net.protocol.transport.TransportHandshakeMessageFactoryImpl;

/**
 * TC Group Communications manager for setting up listners and creating client connections with a NodeID
 * 
 */
public class TCGroupCommunicationsManagerImpl extends CommunicationsManagerImpl {

  // for TC-Group_Comm to exchange NodeID at handshaking
  public TCGroupCommunicationsManagerImpl(MessageMonitor monitor, NetworkStackHarnessFactory stackHarnessFactory,
                                          TCConnectionManager connMgr, ConnectionPolicy connectionPolicy,
                                          int workerCommCount, NodeID nodeID) {

    super(monitor, stackHarnessFactory, connMgr, connectionPolicy, workerCommCount,
          new TransportHandshakeMessageFactoryImpl(nodeID));
  }
}
