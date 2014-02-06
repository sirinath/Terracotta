package com.tc.object.msg;

import com.tc.net.protocol.tcm.TCMessage;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.server.ServerEvent;

import java.util.List;
import java.util.Map;

/**
 * A message with batched server events.
 *
 * @author Eugene Shelestovich
 */
public interface ServerEventBatchMessage extends TCMessage {

  void setEvents(Map<GlobalTransactionID, List<ServerEvent>> events);

  Map<GlobalTransactionID, List<ServerEvent>> getEvents();
}
