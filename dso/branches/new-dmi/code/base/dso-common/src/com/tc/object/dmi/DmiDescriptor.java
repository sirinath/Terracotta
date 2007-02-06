/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.DNAEncoding;

import java.io.IOException;

/**
 * Representation of a distributed method call
 */
public class DmiDescriptor {

  private final String    receiverClassName;
  private final long      receiverID;
  private final String    methodDesc;
  private final String    receiverClassLoaderDesc;
  private final boolean[] isRef;
  private final Object[]  parameters;

  public DmiDescriptor(long receiverID, String receiverClassName, String receiverClassLoaderDesc, String methodDesc,
                       Object[] parameters, boolean[] isRef) {
    assertPre(isRef.length == parameters.length, "Mismatched arrays");
    this.receiverClassName = receiverClassName;
    this.receiverClassLoaderDesc = receiverClassLoaderDesc;
    this.receiverID = receiverID;
    this.parameters = parameters;
    this.isRef = isRef;
    this.methodDesc = methodDesc;
  }

  public String getMethodName() {
    return methodDesc.substring(0, methodDesc.indexOf('('));
  }

  public String getReceiverClassName() {
    return receiverClassName;
  }

  public String getReceiverClassLoaderDesc() {
    return receiverClassLoaderDesc;
  }

  public String getParameterDesc() {
    return methodDesc.substring(methodDesc.indexOf('('));
  }

  public long getReceiverID() {
    return receiverID;
  }

  public final boolean isRef(int index) {
    return isRef[index];
  }

  public final Object[] getParameters() {
    return parameters;
  }

  public String toString() {
    return super.toString() + "[" + receiverClassName + ";  " + receiverID + ";  " + methodDesc + ";  " + ";  "
           + receiverClassLoaderDesc + "]";
  }

  public static DmiDescriptor readFrom(TCByteBufferInputStream in, DNAEncoding encoding) throws IOException {
    final String receiverClassName = in.readString();
    final long receiverID = in.readLong();
    final String methodDesc = in.readString();
    final String receiverClassLoaderDesc = in.readString();
    final int size = in.readInt();
    final boolean[] isRef = new boolean[size];
    final Object[] parameters = new Object[size];
    for (int i = 0; i < isRef.length; i++) {
      isRef[i] = in.readBoolean();
      try {
        parameters[i] = encoding.decode(in);
      } catch (ClassNotFoundException e) {
        throw new IOException("Error reading method param: " + e.toString());
      }
    }
    final DmiDescriptor rv = new DmiDescriptor(receiverID, receiverClassName, receiverClassLoaderDesc, methodDesc,
                                               parameters, isRef);
    return rv;
  }

  public void writeTo(TCByteBufferOutput out, DNAEncoding encoding) {
    out.writeString(receiverClassName);
    out.writeLong(receiverID);
    out.writeString(methodDesc);
    out.writeString(receiverClassLoaderDesc);
    out.writeInt(isRef.length);
    for (int i = 0; i < isRef.length; i++) {
      out.writeBoolean(isRef[i]);
      encoding.encode(parameters[i], out);
    }
  }

  private static void assertPre(boolean v, String msg) {
    if (!v) throw new AssertionError(msg);
  }
}
