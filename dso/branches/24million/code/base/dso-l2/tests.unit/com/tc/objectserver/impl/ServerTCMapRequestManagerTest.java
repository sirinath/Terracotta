/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tc.async.api.Sink;
import com.tc.net.ClientID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.ServerMapRequestID;
import com.tc.object.msg.ServerTCMapResponseMessage;
import com.tc.object.net.DSOChannelManager;
import com.tc.object.net.NoSuchChannelException;
import com.tc.objectserver.api.ObjectManager;
import com.tc.objectserver.context.RequestEntryForKeyContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;

import junit.framework.TestCase;

public class ServerTCMapRequestManagerTest extends TestCase {
  
  
  public void tests() {
    ObjectManager objManager = mock(ObjectManager.class);
    ClientID clientID = new ClientID(0);
    ServerMapRequestID requestID = new ServerMapRequestID(0);
    ObjectID mapID = new ObjectID(1);
    Object portableKey = "key1";
    Object portableValue ="value1";
    Sink respondToServerTCMapSink = mock(Sink.class);
    Sink managedObjectRequestSink = mock(Sink.class);
    DSOChannelManager channelManager = mock(DSOChannelManager.class);
    ServerTCMapRequestManagerImpl serverTCMapRequestManager = new ServerTCMapRequestManagerImpl(objManager, channelManager, respondToServerTCMapSink, managedObjectRequestSink);
    serverTCMapRequestManager.requestValues(requestID, clientID, mapID, portableKey);
    
    RequestEntryForKeyContext requestContext = new RequestEntryForKeyContext(clientID, mapID, portableKey,
                                                                                   respondToServerTCMapSink);
    
    verify(objManager, atLeastOnce()).lookupObjectsFor(clientID, requestContext);
   
    ManagedObject mo = mock(ManagedObject.class);
    ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey)).thenReturn(portableValue);
    when(mo.getManagedObjectState()).thenReturn(mos);
    MessageChannel messageChannel = mock(MessageChannel.class);
    try {
      when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    } catch (NoSuchChannelException e) {
      throw new AssertionError(e);
    }
    ServerTCMapResponseMessage message = mock(ServerTCMapResponseMessage.class);
    when(messageChannel.createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE)).thenReturn(message);
    
    serverTCMapRequestManager.sendValues(clientID, mapID, mo, portableKey);
   
    verify(mo, atLeastOnce()).getManagedObjectState();
    
    verify(objManager, atLeastOnce()).releaseReadOnly(mo);
    
    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (NoSuchChannelException e) {
      throw new AssertionError(e);
    }
      
    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);
    
    verify(message, atLeastOnce()).initialize(mapID, portableKey, portableValue);
    
    verify(message, atLeastOnce()).send();
    
  }
  
  


}
