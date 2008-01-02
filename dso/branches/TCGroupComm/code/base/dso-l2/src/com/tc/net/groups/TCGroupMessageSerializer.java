/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.groups;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;

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
    String classname = serialInput.readString();
    try {
      message = (GroupMessage) Class.forName(classname).newInstance();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    } catch (InstantiationException e) {
      throw new RuntimeException(e);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
    message.deserializeFrom(serialInput);
    return message;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    serialOutput.writeString(message.getClass().getName());
    this.message.serializeTo(serialOutput);
  }

}
