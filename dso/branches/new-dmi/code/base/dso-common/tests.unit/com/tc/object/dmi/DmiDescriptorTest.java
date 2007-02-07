/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.test.TCTestCase;

import java.io.IOException;

public class DmiDescriptorTest extends TCTestCase {

  final ObjectID receiverId              = new ObjectID(567);
  final ObjectID dmiCallId               = new ObjectID(789);

  public void testSerialization() throws IOException {

    final DmiDescriptor dd1 = new DmiDescriptor(receiverId,dmiCallId);
    final DmiDescriptor dd2 = writeAndRead(dd1);
    check(dd1, dd2);
  }

  private void check(DmiDescriptor dd1, DmiDescriptor dd2) {
    assertEquals(receiverId, dd2.getReceiverId());
    assertEquals(dmiCallId, dd2.getDmiCallId());
  }

  private DmiDescriptor writeAndRead(DmiDescriptor dd1) throws IOException {
    final TCByteBufferInputStream in = new TCByteBufferInputStream(write(dd1));
    final DmiDescriptor rv = DmiDescriptor.readFrom(in);
    assertTrue(in.available() == 0);
    return rv;
  }

  private TCByteBuffer[] write(DmiDescriptor dd) {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    dd.writeTo(out);
    out.close();
    return out.toArray();
  }
}
