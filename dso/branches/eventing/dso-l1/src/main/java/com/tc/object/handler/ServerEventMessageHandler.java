package com.tc.object.handler;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.async.api.Sink;
import com.tc.net.NodeID;
import com.tc.object.context.ServerEventDeliveryContext;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.msg.AcknowledgeServerEventMessage;
import com.tc.object.msg.AcknowledgeServerEventMessageFactory;
import com.tc.object.msg.ServerEventBatchMessage;
import com.tc.server.ServerEvent;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Handles batches of events coming from server and multiplexes them.
 *
 * @author Eugene Shelestovich
 */
public class ServerEventMessageHandler extends AbstractEventHandler {

  private final Sink deliverySink;
  private final AcknowledgeServerEventMessageFactory ackMessageFactory;

  public ServerEventMessageHandler(final Sink deliverySink, final AcknowledgeServerEventMessageFactory ackMessageFactory) {
    this.deliverySink = deliverySink;
    this.ackMessageFactory = ackMessageFactory;
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (!(context instanceof ServerEventBatchMessage)) {
      throw new AssertionError("Unknown event type: " + context.getClass().getName());
    }
    
    final ServerEventBatchMessage message = (ServerEventBatchMessage) context;
    final NodeID remoteNode = message.getChannel().getRemoteNodeID();
    // unfold the batch and multiplex messages to different queues based on the event key
    Map<GlobalTransactionID, List<ServerEvent>> eventMap = message.getEvents();
    for (Entry<GlobalTransactionID, List<ServerEvent>> entry : eventMap.entrySet()) {
      for (final ServerEvent event : entry.getValue()) {
        deliverySink.add(new ServerEventDeliveryContext(event, remoteNode));
      }
    }

    // TODO: Acknowledge events received
    AcknowledgeServerEventMessage ack = ackMessageFactory.newAcknowledgeServerEventMessage(message.getSourceNodeID());
    ack.initialize(eventMap.keySet());
    ack.send();

  }
}
