/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCSerializable;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.ObjectID;
import com.tc.object.dna.api.DNA;
import com.tc.object.dna.api.LogicalChangeID;
import com.tc.object.dna.api.LogicalChangeResult;
import com.tc.object.dna.impl.DNAImpl;
import com.tc.object.dna.impl.ObjectStringSerializer;
import com.tc.object.dna.impl.ObjectStringSerializerImpl;
import com.tc.object.dna.impl.VersionizedDNAWrapper;
import com.tc.object.gtx.GlobalTransactionID;
import com.tc.object.locks.ClientServerExchangeLockContext;
import com.tc.object.locks.LockID;
import com.tc.object.session.SessionID;
import com.tc.object.tx.TransactionID;
import com.tc.object.tx.TxnType;
import com.tc.server.ServerEvent;
import com.tc.util.Assert;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author steve
 */
public class BroadcastTransactionMessageImpl extends DSOMessageBase implements BroadcastTransactionMessage {

  private final static byte DNA_ID = 1;
  private final static byte LOCK_ID = 2;
  private final static byte CHANGE_ID = 3;
  private final static byte TRANSACTION_ID = 4;
  private final static byte COMMITTER_ID = 5;
  private final static byte TRANSACTION_TYPE_ID = 6;
  private final static byte GLOBAL_TRANSACTION_ID = 7;
  private final static byte LOW_WATERMARK = 8;
  private final static byte SERIALIZER_ID = 9;
  private final static byte NOTIFIED = 10;
  private final static byte ROOT_NAME_ID_PAIR = 11;
  private final static byte LOGICAL_CHANGE_RESULT = 13;
  private final static byte SERVER_EVENT = 14;

  private long changeID;
  private TransactionID transactionID;
  private NodeID committerID;
  private TxnType transactionType;
  private GlobalTransactionID globalTransactionID;
  private GlobalTransactionID lowWatermark;
  private ObjectStringSerializer serializer;

  private final List                                      changes               = new ArrayList();
  private final Collection                                notifies              = new ArrayList();
  private final Map                                       newRoots              = new HashMap();
  private final List<LockID>                              lockIDs               = new ArrayList();
  private final Map<LogicalChangeID, LogicalChangeResult> logicalChangeResults  = new HashMap();
  private final List<ServerEvent>                         events                = new ArrayList();

  public BroadcastTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final TCByteBufferOutputStream out, final MessageChannel channel,
                                         final TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public BroadcastTransactionMessageImpl(final SessionID sessionID, final MessageMonitor monitor,
                                         final MessageChannel channel, final TCMessageHeader header,
                                         final TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  @Override
  protected void dehydrateValues() {
    putNVPair(TRANSACTION_TYPE_ID, this.transactionType.getType());
    for (final Object element : this.lockIDs) {
      LockID lockID = (LockID) element;
      putNVPair(LOCK_ID, lockID);
    }
    for (final Object notify : this.notifies) {
      ClientServerExchangeLockContext notified = (ClientServerExchangeLockContext) notify;
      putNVPair(NOTIFIED, notified);
    }

    putNVPair(SERIALIZER_ID, this.serializer);
    putNVPair(CHANGE_ID, this.changeID);
    putNVPair(TRANSACTION_ID, this.transactionID.toLong());
    putNVPair(COMMITTER_ID, this.committerID);
    putNVPair(GLOBAL_TRANSACTION_ID, this.globalTransactionID.toLong());
    putNVPair(LOW_WATERMARK, this.lowWatermark.toLong());

    for (final Object change : this.changes) {
      DNAImpl dna = (DNAImpl) change;
      putNVPair(DNA_ID, dna);
    }
    for (final Object o : this.newRoots.keySet()) {
      String key = (String) o;
      ObjectID value = (ObjectID) this.newRoots.get(key);
      putNVPair(ROOT_NAME_ID_PAIR, new RootIDPair(key, value));
    }
    for (final Entry<LogicalChangeID, LogicalChangeResult> entry : logicalChangeResults.entrySet()) {
      putNVPair(LOGICAL_CHANGE_RESULT, new LogicalChangeResultPair(entry.getKey(), entry.getValue()));
    }
    for (final ServerEvent event : events) {
      putNVPair(SERVER_EVENT, new ServerEventSerializableContext(event));
    }
  }

  @Override
  protected boolean hydrateValue(final byte name) throws IOException {
    switch (name) {
      case TRANSACTION_TYPE_ID:
        this.transactionType = TxnType.typeFor(getByteValue());
        return true;
      case DNA_ID:
        this.changes.add(getObject(new DNAImpl(this.serializer, false)));
        return true;
      case SERIALIZER_ID:
        this.serializer = (ObjectStringSerializer) getObject(new ObjectStringSerializerImpl());
        return true;
      case LOCK_ID:
        this.lockIDs.add(getLockIDValue());
        return true;
      case NOTIFIED:
        this.notifies.add(this.getObject(new ClientServerExchangeLockContext()));
        return true;
      case CHANGE_ID:
        this.changeID = getLongValue();
        return true;
      case TRANSACTION_ID:
        this.transactionID = new TransactionID(getLongValue());
        return true;
      case COMMITTER_ID:
        this.committerID = getNodeIDValue();
        return true;
      case GLOBAL_TRANSACTION_ID:
        this.globalTransactionID = new GlobalTransactionID(getLongValue());
        return true;
      case LOW_WATERMARK:
        this.lowWatermark = new GlobalTransactionID(getLongValue());
        return true;
      case ROOT_NAME_ID_PAIR:
        RootIDPair rootIDPair = (RootIDPair) getObject(new RootIDPair());
        this.newRoots.put(rootIDPair.getRootName(), rootIDPair.getRootID());
        return true;
      case LOGICAL_CHANGE_RESULT:
        LogicalChangeResultPair resultPair = (LogicalChangeResultPair) getObject(new LogicalChangeResultPair());
        this.logicalChangeResults.put(resultPair.getId(), resultPair.getResult());
        return true;
      case SERVER_EVENT:
        final ServerEventSerializableContext ctx = (ServerEventSerializableContext) getObject(new ServerEventSerializableContext());
        events.add(ctx.getEvent());
        return true;
      default:
        return false;
    }
  }

  @Override
  public void initialize(final List chges, final ObjectStringSerializer aSerializer, final LockID[] lids,
                         final long cid, final TransactionID txID, final NodeID client, final GlobalTransactionID gtx,
                         final TxnType txnType, final GlobalTransactionID lowGlobalTransactionIDWatermark,
                         final Collection theNotifies, final Map roots,
                         final Map<LogicalChangeID, LogicalChangeResult> logicalInvokeResults,
                         final Collection<ServerEvent> events) {
    Assert.assertNotNull(txnType);

    this.changes.addAll(chges);
    Collections.addAll(this.lockIDs, lids);
    this.changeID = cid;
    this.transactionID = txID;
    this.committerID = client;
    this.transactionType = txnType;
    this.globalTransactionID = gtx;
    this.lowWatermark = lowGlobalTransactionIDWatermark;
    this.serializer = aSerializer;
    this.notifies.addAll(theNotifies);
    this.newRoots.putAll(roots);
    this.logicalChangeResults.putAll(logicalInvokeResults);
    this.events.addAll(events);
  }

  @Override
  public List getLockIDs() {
    return this.lockIDs;
  }

  @Override
  public TxnType getTransactionType() {
    return this.transactionType;
  }

  @Override
  public Collection getObjectChanges() {
    final Collection versionizedChanges = new ArrayList(this.changes.size());
    for (final Object change : this.changes) {
      versionizedChanges.add(new VersionizedDNAWrapper((DNA) change, this.globalTransactionID.toLong()));
    }
    return versionizedChanges;
  }

  @Override
  public long getChangeID() {
    return this.changeID;
  }

  @Override
  public TransactionID getTransactionID() {
    return this.transactionID;
  }

  @Override
  public NodeID getCommitterID() {
    return this.committerID;
  }

  @Override
  public GlobalTransactionID getGlobalTransactionID() {
    return this.globalTransactionID;
  }

  @Override
  public GlobalTransactionID getLowGlobalTransactionIDWatermark() {
    Assert.assertNotNull(this.lowWatermark);
    return this.lowWatermark;
  }

  @Override
  public Collection getNotifies() {
    return new ArrayList(this.notifies);
  }

  @Override
  public void doRecycleOnRead() {
    // dont recycle yet
  }

  @Override
  protected boolean isOutputStreamRecycled() {
    return true;
  }

  @Override
  public void doRecycleOnWrite() {
    // recycle only those buffers created for this message
    recycleOutputStream();
  }

  @Override
  public Map getNewRoots() {
    return this.newRoots;
  }

  private static class RootIDPair implements TCSerializable {
    private String rootName;
    private ObjectID rootID;

    public RootIDPair() {
    }

    public RootIDPair(final String rootName, final ObjectID rootID) {
      this.rootName = rootName;
      this.rootID = rootID;
    }

    @Override
    public void serializeTo(final TCByteBufferOutput serialOutput) {
      serialOutput.writeString(this.rootName);
      serialOutput.writeLong(this.rootID.toLong());

    }

    @Override
    public Object deserializeFrom(final TCByteBufferInput serialInput) throws IOException {
      this.rootName = serialInput.readString();
      this.rootID = new ObjectID(serialInput.readLong());
      return this;
    }

    public ObjectID getRootID() {
      return this.rootID;
    }

    public String getRootName() {
      return this.rootName;
    }
  }

  private static class LogicalChangeResultPair implements TCSerializable {
    private LogicalChangeID id;
    private LogicalChangeResult result;


    public LogicalChangeResultPair(LogicalChangeID id, LogicalChangeResult result) {
      this.id = id;
      this.result = result;
    }

    public LogicalChangeResultPair() {
    }

    @Override
    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeLong(id.toLong());
      result.serializeTo(serialOutput);
    }

    @Override
    public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
      this.id = new LogicalChangeID(serialInput.readLong());
      this.result = (LogicalChangeResult) (new LogicalChangeResult()).deserializeFrom(serialInput);
      return this;
    }

    public LogicalChangeID getId() {
      return id;
    }

    public LogicalChangeResult getResult() {
      return result;
    }

  }

  public Map<LogicalChangeID, LogicalChangeResult> getLogicalChangeResults() {
    return logicalChangeResults;
  }

  @Override
  public List<ServerEvent> getEvents() {
    return events;
  }
}
