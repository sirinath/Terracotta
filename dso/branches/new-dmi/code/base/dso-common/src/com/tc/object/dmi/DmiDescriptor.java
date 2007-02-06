/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.util.Assert;

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
    Assert.pre(receiverClassName != null);
    Assert.pre(receiverClassLoaderDesc != null);
    Assert.pre(methodDesc != null && methodDesc.indexOf('(') > 0);
    Assert.pre(isRef.length == parameters.length);
    
    this.receiverClassName = receiverClassName;
    this.receiverClassLoaderDesc = receiverClassLoaderDesc;
    this.receiverID = receiverID;
    this.parameters = parameters;
    this.isRef = isRef;
    this.methodDesc = methodDesc;
  }

  public String getReceiverClassName() {
    return receiverClassName;
  }

  public String getReceiverClassLoaderDesc() {
    return receiverClassLoaderDesc;
  }

  public String getMethodDesc() {
    return methodDesc;
  }

  public String getParameterDesc() {
    return methodDesc.substring(methodDesc.indexOf('('));
  }

  public String getMethodName() {
    return methodDesc.substring(0, methodDesc.indexOf('('));
  }

  public long getReceiverID() {
    return receiverID;
  }

  public boolean[] getIsRef() {
    return isRef;
  }

  public final Object[] getParameters() {
    return parameters;
  }

  public String toString() {
    return super.toString() + "[" + receiverClassName + ";  " + receiverID + ";  " + methodDesc + ";  " + ";  "
           + receiverClassLoaderDesc + "]";
  }

  public static DmiDescriptor readFrom(TCByteBufferInputStream in, DNAEncoding encoding) throws IOException,
      ClassNotFoundException {
    final String receiverClassName = in.readString();
    final long receiverID = in.readLong();
    final String methodDesc = in.readString();
    final String receiverClassLoaderDesc = in.readString();
    final boolean[] isRef = (boolean[]) encoding.decode(in);
    final Object[] parameters = (Object[]) encoding.decode(in);

    final DmiDescriptor rv = new DmiDescriptor(receiverID, receiverClassName, receiverClassLoaderDesc, methodDesc,
                                               parameters, isRef);
    return rv;
  }

  public void writeTo(TCByteBufferOutput out, DNAEncoding encoding) {
    out.writeString(receiverClassName);
    out.writeLong(receiverID);
    out.writeString(methodDesc);
    out.writeString(receiverClassLoaderDesc);
    encoding.encodeArray(isRef, out);
    encoding.encodeArray(parameters, out);
  }
}
