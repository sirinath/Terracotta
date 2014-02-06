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

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((event == null) ? 0 : event.hashCode());
    result = prime * result + ((gtxId == null) ? 0 : gtxId.hashCode());
    result = prime * result + ((type == null) ? 0 : type.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    ServerEventWrapper other = (ServerEventWrapper) obj;
    if (event == null) {
      if (other.event != null) return false;
    } else if (!event.equals(other.event)) return false;
    if (gtxId == null) {
      if (other.gtxId != null) return false;
    } else if (!gtxId.equals(other.gtxId)) return false;
    if (type != other.type) return false;
    return true;
  }

}
