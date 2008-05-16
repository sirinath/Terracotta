/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

public class TCNetworkMessageEventImpl implements TCNetworkMessageEvent {
  private final TCNetworkMessageEventType type;
  private final TCNetworkMessage          message;

  public TCNetworkMessageEventImpl(TCNetworkMessageEventType type, TCNetworkMessage message) {
    this.type = type;
    this.message = message;
  }

  public TCNetworkMessageEventType getType() {
    return type;
  }

  public TCNetworkMessage getMessage() {
    return message;
  }

}
