/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.bytes.TCByteBuffer;
import com.tc.exception.TCRuntimeException;
import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessageHeader;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.lockmanager.api.LockID;
import com.tc.object.session.SessionID;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

public class LockStatisticsResponseMessage extends DSOMessageBase {

  private final static byte TYPE                                  = 1;
  private final static byte LOCK_ID                               = 2;
  private final static byte NUMBER_OF_STACK_TRACE                 = 3;
  private final static byte STACK_TRACE                           = 4;

  // message types
  private final static byte LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE = 1;

  private int               type;
  private LockID            lockID;
  private List              stackTraces;

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
    put(stackTraces);
  }

  private void put(Collection stackTraces) {
    super.putNVPair(NUMBER_OF_STACK_TRACE, stackTraces.size());
    for (Iterator i = stackTraces.iterator(); i.hasNext();) {
      putNVPair(STACK_TRACE, new TCStackTraceElement((StackTraceElement[]) i.next()));
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
      case NUMBER_OF_STACK_TRACE:
        int numOfStackTraces = getIntValue();
        this.stackTraces = new LinkedList();
        return true;
      case STACK_TRACE:
        TCStackTraceElement ste = new TCStackTraceElement();
        getObject(ste);
        this.stackTraces.add(ste);
        return true;
      default:
        return false;
    }
  }
  
  public LockID getLockID() {
    return this.lockID;
  }
  
  public List getStackTraces() {
    return this.stackTraces;
  }

  public void initialize(LockID lid, List stackTraces) {
    this.lockID = lid;
    this.stackTraces = stackTraces;
    this.type = LOCK_STATISTICS_RESPONSE_MESSAGE_TYPE;
  }

  private static class TCStackTraceElement implements TCSerializable, Serializable {
    private StackTraceElement[] stackTraceElements;

    public TCStackTraceElement() {
      return;
    }

    public TCStackTraceElement(StackTraceElement[] stackTraceElements) {
      this.stackTraceElements = stackTraceElements;
    }

    public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
      int length = serialInput.readInt();
      stackTraceElements = new StackTraceElement[length];
      for (int i = 0; i < length; i++) {
        int numBytes = serialInput.readInt();
        byte[] bytes = new byte[numBytes];
        serialInput.read(bytes);
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInputStream is = new ObjectInputStream(bis);
        try {
          stackTraceElements[i] = (StackTraceElement) is.readObject();
        } catch (ClassNotFoundException e) {
          throw new TCRuntimeException(e);
        }
      }

      return this;
    }

    public void serializeTo(TCByteBufferOutput serialOutput) {
      serialOutput.writeInt(stackTraceElements.length);
      for (int i = 0; i < stackTraceElements.length; i++) {
        StackTraceElement se = stackTraceElements[i];
        try {
          ByteArrayOutputStream bos = new ByteArrayOutputStream();
          ObjectOutputStream os = new ObjectOutputStream(bos);
          os.writeObject(se);
          byte[] bytes = bos.toByteArray();
          serialOutput.writeInt(bytes.length);
          serialOutput.write(bytes);
        } catch (IOException e) {
          throw new TCRuntimeException(e);
        }
      }
    }
    
    public String toString() {
      StringBuffer sb = new StringBuffer();
      for (int i=0; i<stackTraceElements.length; i++) {
        for (int j=0; j<i; j++) {
          sb.append(" ");
        }
        sb.append(stackTraceElements[i].toString());
        sb.append("\n");
      }
      return sb.toString();
    }

  }

}
