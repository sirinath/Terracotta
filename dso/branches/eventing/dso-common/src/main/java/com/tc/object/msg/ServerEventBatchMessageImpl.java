package com.tc.object.msg;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.dna.api.DNAEncoding;
import com.tc.object.dna.impl.SerializerDNAEncodingImpl;
import com.tc.object.dna.impl.UTF8ByteDataHolder;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.session.SessionID;
import com.tc.server.BasicServerEvent;
import com.tc.server.CustomLifespanVersionedServerEvent;
import com.tc.server.ServerEvent;
import com.tc.server.ServerEventType;
import com.tc.server.VersionedServerEvent;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Eugene Shelestovich
 */
public class ServerEventBatchMessageImpl extends DSOMessageBase implements ServerEventBatchMessage {

  private final static DNAEncoding encoder = new SerializerDNAEncodingImpl();
  private final static DNAEncoding decoder = new SerializerDNAEncodingImpl();

  private static final byte TRANSACTIONS_COUNT = 0;
  private Map<GlobalTransactionID, List<ServerEvent>> serverEventMap     = Maps.newHashMap();

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final TCByteBufferOutputStream out, final MessageChannel channel,
                                     final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public ServerEventBatchMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                     final MessageChannel channel, final TCMessageHeader header,
                                     final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTIONS_COUNT, serverEventMap.size());

    int txnCount = 0;
    final TCByteBufferOutputStream outStream = getOutputStream();

    for (Entry<GlobalTransactionID, List<ServerEvent>> entry : serverEventMap.entrySet()) {
      int eventCount = 0;
      encoder.encode(entry.getKey().toLong(), outStream);
      encoder.encode(entry.getValue().size(), outStream);
      for (ServerEvent event : entry.getValue()) {
        encoder.encode(event.getType().ordinal(), outStream);
        encoder.encode(event.getCacheName(), outStream);
        encoder.encode(event.getKey(), outStream);
        encoder.encode(event.getValue(), outStream);
        // Note: This is an ugly hack, but it will work for now. Should fix it soon.
        // Currently every event is a VersionedServerEvent, there is no implementation for ServerEvent
        encoder.encode(((VersionedServerEvent) event).getVersion(), outStream);

        boolean customLifespanEvent = (event instanceof CustomLifespanVersionedServerEvent);
        encoder.encode(customLifespanEvent, outStream);
        if (customLifespanEvent) {
          CustomLifespanVersionedServerEvent customLifespanVersionedServerEvent = (CustomLifespanVersionedServerEvent) event;
          encoder.encode(customLifespanVersionedServerEvent.getCreationTimeInSeconds(), outStream);
          encoder.encode(customLifespanVersionedServerEvent.getTimeToIdle(), outStream);
          encoder.encode(customLifespanVersionedServerEvent.getTimeToLive(), outStream);
        }
        eventCount++;
      }
      Assert.assertEquals(entry.getValue().size(), eventCount);
      txnCount++;
    }

    Assert.assertEquals(serverEventMap.size(), txnCount);
  }

  @Override
  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TRANSACTIONS_COUNT:
        try {
          int txnCount = getIntValue();
          serverEventMap = Maps.newHashMap();
          final TCByteBufferInputStream inputStream = getInputStream();
          while (txnCount-- > 0) {
            final GlobalTransactionID gtxId = new GlobalTransactionID((Long) decoder.decode(inputStream));
            int eventCount = (Integer) decoder.decode(inputStream);
            List<ServerEvent> events = Lists.newArrayList();
            serverEventMap.put(gtxId, events);
            while (eventCount-- > 0) {
              int index = (Integer) decoder.decode(inputStream);
              final ServerEventType type = ServerEventType.values()[index];
              final String destination = (String) decoder.decode(inputStream);
              final Object key = decoder.decode(inputStream);
              final byte[] value = (byte[]) decoder.decode(inputStream);
              final long version = (Long) decoder.decode(inputStream);
              VersionedServerEvent serverEvent = new BasicServerEvent(type, extractStringIfNecessary(key), value,
                                                                      version, destination);

              boolean customLifespanEvent = (Boolean) decoder.decode(inputStream);
              if (customLifespanEvent) {
                final int creationTime = (Integer) decoder.decode(inputStream);
                final int timeToIdle = (Integer) decoder.decode(inputStream);
                final int timeToLive = (Integer) decoder.decode(inputStream);
                serverEvent = new CustomLifespanVersionedServerEvent((BasicServerEvent) serverEvent, creationTime,
                                                                     timeToIdle, timeToLive);
              }
              events.add(serverEvent);
            }
          }
        } catch (ClassNotFoundException e) {
          throw new AssertionError(e);
        }
        return true;
      default:
        return false;
    }
  }

  /**
   * Transform a key from internal representation to string if necessary.
   */
  private static Object extractStringIfNecessary(final Object key) {
    final Object normalizedKey;
    if (key instanceof UTF8ByteDataHolder) {
      normalizedKey = ((UTF8ByteDataHolder)key).asString();
    } else {
      normalizedKey = key;
    }
    return normalizedKey;
  }

  @Override
  public void setEvents(final Map<GlobalTransactionID, List<ServerEvent>> events) {
    this.serverEventMap = events;
  }

  @Override
  public Map<GlobalTransactionID, List<ServerEvent>> getEvents() {
    return serverEventMap;
  }
}
