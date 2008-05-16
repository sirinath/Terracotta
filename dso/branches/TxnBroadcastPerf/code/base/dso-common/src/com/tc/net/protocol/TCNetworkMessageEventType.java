/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol;

public class TCNetworkMessageEventType {

  public static final TCNetworkMessageEventType SENT_EVENT       = new TCNetworkMessageEventType("MESSAGE_SENT_EVENT");
  public static final TCNetworkMessageEventType SEND_ERROR_EVENT = new TCNetworkMessageEventType(
                                                                                                 "MESSAGE_SEND_ERROR_EVENT");

  private final String                          name;

  private TCNetworkMessageEventType(String name) {
    this.name = name;
  }

  public String toString() {
    return name;
  }
}
