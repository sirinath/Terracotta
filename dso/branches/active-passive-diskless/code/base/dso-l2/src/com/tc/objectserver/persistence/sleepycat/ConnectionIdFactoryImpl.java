/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.transport.ConnectionID;
import com.tc.net.protocol.transport.ConnectionIdFactory;
import com.tc.objectserver.persistence.api.ClientStatePersistor;
import com.tc.objectserver.persistence.api.PersistentSequence;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class ConnectionIdFactoryImpl implements ConnectionIdFactory {

  private final ClientStatePersistor clientStateStore;
  private final PersistentSequence   connectionIDSequence;
  private String                     uid;

  public ConnectionIdFactoryImpl(ClientStatePersistor clientStateStore) {
    this.clientStateStore = clientStateStore;
    this.connectionIDSequence = clientStateStore.getConnectionIDSequence();
    this.uid = connectionIDSequence.getUID();
  }

  public ConnectionID nextConnectionId() {
    long clientID = connectionIDSequence.next();
    // Make sure we save the fact that we are giving out this id to someone in the database before giving it out.
    clientStateStore.saveClientState(new ChannelID(clientID));
    ConnectionID rv = new ConnectionID(clientID, uid);
    return rv;
  }
  
  public void setUID(String clusterID) {
    this.uid = clusterID;
  }

  public Set loadConnectionIDs() {
    Set connections = new HashSet();
    for (Iterator i = clientStateStore.loadClientIDs(); i.hasNext();) {
      connections.add(new ConnectionID(((ChannelID) i.next()).toLong(), uid));
    }
    return connections;
  }

}
