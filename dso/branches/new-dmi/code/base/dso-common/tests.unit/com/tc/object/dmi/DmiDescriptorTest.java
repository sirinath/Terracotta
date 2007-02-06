/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.object.bytecode.MockClassProvider;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.object.loaders.ClassProvider;
import com.tc.test.TCTestCase;

import java.io.IOException;
import java.util.Arrays;

public class DmiDescriptorTest extends TCTestCase {

  public void testSerialization() throws IOException, ClassNotFoundException {
    final long receiverID = 567;
    final String receiverClassName = "someString1";
    final String receiverClassLoaderDesc = "someString2";
    final String methodDesc = "someString3";
    final Object[] parameters = new Object[] { new Short((short) 1), new Integer(2), new Long(3), new Float(4.4), new Double(5.5), new String("6") };
    final boolean[] isRef = new boolean[] { false, false, false, false, false, false };
    final DmiDescriptor dd1 = new DmiDescriptor(receiverID , receiverClassName , receiverClassLoaderDesc , methodDesc ,
                                                parameters , isRef);
    final ClassProvider classProvider = new MockClassProvider();
    final DNAEncoding encoding = new DNAEncoding(classProvider);
    final TCByteBufferOutput out = new TCByteBufferOutputStream();
    dd1.writeTo(out, encoding);
    out.close();
    final TCByteBufferInputStream in = new TCByteBufferInputStream(out.toArray());
    final DmiDescriptor dd2 = DmiDescriptor.readFrom(in , encoding);
    
    assertEquals(receiverID, dd2.getReceiverID());
    assertEquals(receiverClassLoaderDesc, dd2.getReceiverClassLoaderDesc());
    assertEquals(receiverClassName, dd2.getReceiverClassName());
    assertEquals(methodDesc, dd2.getMethodDesc());
    assertTrue(Arrays.equals( parameters, dd2.getParameters()));
    assertTrue(Arrays.equals(isRef, dd2.getIsRef()));
  }

}
