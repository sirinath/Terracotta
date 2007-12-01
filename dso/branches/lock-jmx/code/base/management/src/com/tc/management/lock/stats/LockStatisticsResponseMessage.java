/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.lock.stats;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.msg.DSOMessageBase;
import com.tc.object.session.SessionID;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;

public class LockStatisticsResponseMessage extends DSOMessageBase {

  private final static byte TYPE                                  = 1;
  private final static byte LOCK_ID                               = 2;
  private final static byte NUMBER_OF_LOCK_STAT_ELEMENTS          = 3;
  private final static byte LOCK_STAT_ELEMENT                     = 4;

  // message types
  private final static byte LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE = 1;

  private int               type;
  private LockID            lockID;
  private Collection        lockStatElements;

  public LockStatisticsResponseMessage(SessionID sessionID, MessageMonitor monitor, TCByteBufferOutput out,
                                       MessageChannel channel, TCMessageType type) {
    super(sessionID, monitor, out, channel, type);
  }

  public LockStatisticsResponseMessage(SessionID sessionID, MessageMonitor monitor, MessageChannel channel,
                                       TCMessageHeader header, TCByteBuffer[] data) {
    super(sessionID, monitor, channel, header, data);
  }

  protected void dehydrateValues() {
    putNVPair(TYPE, this.type);
    putNVPair(LOCK_ID, lockID.asString());
    put(lockStatElements);
  }

  private void put(Collection lockStatElements) {
    super.putNVPair(NUMBER_OF_LOCK_STAT_ELEMENTS, lockStatElements.size());
    for (Iterator i = lockStatElements.iterator(); i.hasNext();) {
      LockStatElement lse = (LockStatElement)i.next();
      putNVPair(LOCK_STAT_ELEMENT, lse);
    }
  }

  private boolean isLockStatisticsResponseMessage() {
    return type == LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE;
  }

  protected String describePayload() {
    StringBuffer rv = new StringBuffer();
    rv.append("Type : ");

    if (isLockStatisticsResponseMessage()) {
      rv.append("LOCK STATISTICS RESPONSE \n");
    } else {
      rv.append("UNKNOWN \n");
    }

    rv.append(lockID).append(' ').append("Lock Type: ").append('\n');

    return rv.toString();
  }

  protected boolean hydrateValue(byte name) throws IOException {
    switch (name) {
      case TYPE:
        this.type = getIntValue();
        return true;
      case LOCK_ID:
        this.lockID = new LockID(getStringValue());
        return true;
      case NUMBER_OF_LOCK_STAT_ELEMENTS:
        int numOfStackTraces = getIntValue();
        this.lockStatElements = new LinkedList();
        return true;
      case LOCK_STAT_ELEMENT:
        LockStatElement lse = new LockStatElement();
        getObject(lse);
        this.lockStatElements.add(lse);
        return true;
      default:
        return false;
    }
  }

  public LockID getLockID() {
    return this.lockID;
  }

  public Collection getLockStatElements() {
    return this.lockStatElements;
  }

  public void initialize(LockID lid, Collection lockStatElements) {
    this.lockID = lid;
    this.lockStatElements = lockStatElements;
    this.type = LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE;
  }

}
