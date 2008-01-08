/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * This is a helper class to bridge the GroupMessage to TCMessage for TC's serialization.
 */
public class TCGroupMessageSerializer implements TCSerializable {

  private GroupMessage message;

  public TCGroupMessageSerializer() {
    // NOP
  }

  public TCGroupMessageSerializer(GroupMessage message) {
    this.message = message;
  }

  public GroupMessage getGroupMessage() {
    return message;
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    int length = serialInput.readInt();
    byte[] data = new byte[length];
    serialInput.read(data);
    
    ByteArrayInputStream in = new ByteArrayInputStream(data);
    ObjectInputStream stream = new ObjectInputStream(in);
    try {
      this.message = (GroupMessage) stream.readObject();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException();
    }

    return message;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    //serialOutput.writeString(message.getClass().getName());
    //this.message.serializeTo(serialOutput);
    
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    try {
      ObjectOutputStream stream = new ObjectOutputStream(out);
      stream.writeObject(this.message);
    } catch (IOException e) {
      throw new RuntimeException();
    }
    byte[] data = out.toByteArray();
    serialOutput.writeInt(data.length);
    serialOutput.write(data);

  }

}
