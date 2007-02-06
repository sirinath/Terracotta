/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.bytes.TCByteBuffer;
import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.ObjectID;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.loaders.ClassProvider;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.util.Arrays;

public class DmiDescriptorTest extends TCTestCase {

  final ClassProvider classProvider           = new MockClassProvider();
  final DNAEncoding   encoding                = new DNAEncoding(classProvider);
  final ObjectID      receiverID              = new ObjectID(567);
  final String        receiverClassName       = "someString1";
  final String        receiverClassLoaderDesc = "someString2";
  final String        methodName              = "someString3";
  final String        paramDesc               = "someString4";
  final Object[]      parameters              = new Object[] { new Short((short) 1), new Integer(2), new Long(3),
      new Float(4.4), new Double(5.5), new String("6"), new ObjectID(7) };

  public void testSerialization() throws IOException, ClassNotFoundException {

    final DmiDescriptor dd1 = new DmiDescriptor(receiverID, receiverClassName, receiverClassLoaderDesc, methodName,
                                                paramDesc, parameters);
    final DmiDescriptor dd2 = writeAndRead(dd1);
    check(dd1, dd2);
  }

  public void testBufferedSerialization() throws IOException, ClassNotFoundException {

    final DmiDescriptor dd1 = new DmiDescriptor(receiverID, receiverClassName, receiverClassLoaderDesc, methodName,
                                                paramDesc, parameters);
    final TCByteBuffer[] buffs = write(dd1);
    TCByteBufferInputStream in = new TCByteBufferInputStream(buffs);
    final BufferedDmiDescriptor bdd = BufferedDmiDescriptor.readFrom(in);
    assertTrue(in.available() == 0);
    assertEquals(dd1.getReceiverID(), bdd.getReceiverId());
    
    final TCByteBuffer[] buffs2 = write(bdd);
    TCByteBufferInputStream in2 = new TCByteBufferInputStream(buffs2);
    final DmiDescriptor dd2 = DmiDescriptor.readFrom(in2, encoding);
    check(dd1, dd2);
  }

  private void check(DmiDescriptor dd1, DmiDescriptor dd2) {
    assertEquals(receiverID, dd2.getReceiverID());
    assertEquals(receiverClassLoaderDesc, dd2.getReceiverClassLoaderDesc());
    assertEquals(receiverClassName, dd2.getReceiverClassName());
    assertEquals(methodName, dd2.getMethodName());
    assertEquals(paramDesc, dd2.getParameterDesc());
    assertTrue(Arrays.equals(parameters, dd2.getParameters()));
  }

  private DmiDescriptor writeAndRead(DmiDescriptor dd1) throws IOException, ClassNotFoundException {
    final TCByteBufferInputStream in = new TCByteBufferInputStream(write(dd1));
    final DmiDescriptor rv = DmiDescriptor.readFrom(in, encoding);
    assertTrue(in.available() == 0);
    return rv;
  }

  private TCByteBuffer[] write(DmiDescriptor dd) {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    dd.writeTo(out, encoding);
    out.close();
    return out.toArray();
  }
  private TCByteBuffer[] write(BufferedDmiDescriptor dd) {
    final TCByteBufferOutputStream out = new TCByteBufferOutputStream();
    dd.writeTo(out);
    out.close();
    return out.toArray();
  }

}
