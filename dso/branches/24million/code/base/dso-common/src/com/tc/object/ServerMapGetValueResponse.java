/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object;

public class ServerMapGetValueResponse {

  private final ServerMapRequestID requestID;
  private final Object             value;

  public ServerMapGetValueResponse(final ServerMapRequestID requestID, final Object value) {
    this.requestID = requestID;
    this.value = value;
  }

  public ServerMapRequestID getRequestID() {
    return this.requestID;
  }

  public Object getValue() {
    return this.value;
  }

}
