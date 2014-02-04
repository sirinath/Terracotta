/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.event;

import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.ServerEvent;

public class ServerEventWrapper {
  enum TYPE {
    BEGIN, END, SERVER_EVENT
  }

  private final TYPE                type;
  private final GlobalTransactionID gtxId;
  private final ServerEvent         event;

  private ServerEventWrapper(final TYPE type, final GlobalTransactionID gtxId, final ServerEvent event) {
    this.type = type;
    this.gtxId = gtxId;
    this.event = event;
  }

  public static ServerEventWrapper createBeginEvent(final GlobalTransactionID gtxId) {
    return new ServerEventWrapper(TYPE.BEGIN, gtxId, null);
  }

  public static ServerEventWrapper createEndEvent(final GlobalTransactionID gtxId) {
    return new ServerEventWrapper(TYPE.END, gtxId, null);
  }

  public static ServerEventWrapper createServerEventWrapper(final GlobalTransactionID gtxId, final ServerEvent event) {
    return new ServerEventWrapper(TYPE.SERVER_EVENT, gtxId, event);
  }

  public TYPE getType() {
    return type;
  }

  public GlobalTransactionID getGtxId() {
    return gtxId;
  }

  public ServerEvent getEvent() {
    return event;
  }

}
