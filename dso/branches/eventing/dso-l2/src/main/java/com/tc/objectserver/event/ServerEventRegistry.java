/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.event;

import com.tc.net.ClientID;
import com.tc.server.ServerEventType;

import java.util.Set;

public interface ServerEventRegistry {

  void register(ClientID clientId, String destination, Set<ServerEventType> eventTypes);

  void unregister(ClientID clientId, String destination, Set<ServerEventType> eventTypes);

}
