/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.impl;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.atMost;
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
import com.tc.objectserver.context.ServerMapRequestContext;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ConcurrentDistributedServerMapManagedObjectState;

import junit.framework.TestCase;

public class ServerTCMapRequestManagerTest extends TestCase {

  public void tests() {
    final ObjectManager objManager = mock(ObjectManager.class);
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID = new ServerMapRequestID(0);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey = "key1";
    final Object portableValue = "value1";
    final Sink respondToServerTCMapSink = mock(Sink.class);
    final Sink managedObjectRequestSink = mock(Sink.class);
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    final ServerTCMapRequestManagerImpl serverTCMapRequestManager = new ServerTCMapRequestManagerImpl(
                                                                                                      objManager,
                                                                                                      channelManager,
                                                                                                      respondToServerTCMapSink,
                                                                                                      managedObjectRequestSink);
    serverTCMapRequestManager.requestValues(requestID, clientID, mapID, portableKey);

    final ServerMapRequestContext requestContext = new ServerMapRequestContext(requestID, clientID, mapID,
                                                                                   portableKey,
                                                                                   respondToServerTCMapSink);

    verify(objManager, atLeastOnce()).lookupObjectsFor(clientID, requestContext);

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey)).thenReturn(portableValue);
    when(mo.getManagedObjectState()).thenReturn(mos);
    final MessageChannel messageChannel = mock(MessageChannel.class);
    try {
      when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }
    final ServerTCMapResponseMessage message = mock(ServerTCMapResponseMessage.class);
    when(messageChannel.createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE)).thenReturn(message);

    serverTCMapRequestManager.sendResponseFor(mapID, mo);

    verify(mo, atLeastOnce()).getManagedObjectState();

    verify(objManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);

    verify(message, atLeastOnce()).initializeGetValueResponse(mapID, requestID, portableValue);

    verify(message, atLeastOnce()).send();

  }

  public void testMultipleKeysRequests() {
    final ObjectManager objManager = mock(ObjectManager.class);
    final ClientID clientID = new ClientID(0);
    final ServerMapRequestID requestID1 = new ServerMapRequestID(0);
    final ServerMapRequestID requestID2 = new ServerMapRequestID(1);
    final ObjectID mapID = new ObjectID(1);
    final Object portableKey1 = "key1";
    final Object portableValue1 = "value1";
    final Object portableKey2 = "key2";
    final Object portableValue2 = "value2";
    final Sink respondToServerTCMapSink = mock(Sink.class);
    final Sink managedObjectRequestSink = mock(Sink.class);
    final DSOChannelManager channelManager = mock(DSOChannelManager.class);
    final ServerTCMapRequestManagerImpl serverTCMapRequestManager = new ServerTCMapRequestManagerImpl(
                                                                                                      objManager,
                                                                                                      channelManager,
                                                                                                      respondToServerTCMapSink,
                                                                                                      managedObjectRequestSink);
    serverTCMapRequestManager.requestValues(requestID1, clientID, mapID, portableKey1);
    serverTCMapRequestManager.requestValues(requestID2, clientID, mapID, portableKey2);

    final ServerMapRequestContext requestContext = new ServerMapRequestContext(requestID1, clientID, mapID,
                                                                                   portableKey1,
                                                                                   respondToServerTCMapSink);

    verify(objManager, atMost(1)).lookupObjectsFor(clientID, requestContext);

    final ManagedObject mo = mock(ManagedObject.class);
    final ConcurrentDistributedServerMapManagedObjectState mos = mock(ConcurrentDistributedServerMapManagedObjectState.class);
    when(mos.getValueForKey(portableKey1)).thenReturn(portableValue1);
    when(mos.getValueForKey(portableKey2)).thenReturn(portableValue2);

    when(mo.getManagedObjectState()).thenReturn(mos);
    final MessageChannel messageChannel = mock(MessageChannel.class);
    try {
      when(channelManager.getActiveChannel(clientID)).thenReturn(messageChannel);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }
    final ServerTCMapResponseMessage message = mock(ServerTCMapResponseMessage.class);
    when(messageChannel.createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE)).thenReturn(message);

    serverTCMapRequestManager.sendResponseFor(mapID, mo);

    verify(mo, atMost(1)).getManagedObjectState();

    verify(objManager, atLeastOnce()).releaseReadOnly(mo);

    try {
      verify(channelManager, atLeastOnce()).getActiveChannel(clientID);
    } catch (final NoSuchChannelException e) {
      throw new AssertionError(e);
    }

    verify(messageChannel, atLeastOnce()).createMessage(TCMessageType.SERVER_TC_MAP_RESPONSE_MESSAGE);

    verify(message, atLeastOnce()).initializeGetValueResponse(mapID, requestID1, portableValue1);
    verify(message, atLeastOnce()).initializeGetValueResponse(mapID, requestID2, portableValue2);

    verify(message, atLeastOnce()).send();
  }

}
