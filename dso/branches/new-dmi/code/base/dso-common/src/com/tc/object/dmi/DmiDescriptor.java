/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.dmi;

import com.tc.io.TCByteBufferInputStream;
import com.tc.io.TCByteBufferOutputStream;
import com.tc.io.TCByteBufferOutputStream.Mark;
import com.tc.object.ObjectID;
import com.tc.object.dna.impl.DNAEncoding;
import com.tc.util.Assert;
import com.tc.util.Conversion;

import java.io.IOException;

/**
 * Representation of a distributed method invocation
 */
public class DmiDescriptor {

  private final ObjectID receiverID;
  private final String   receiverClassName;
  private final String   receiverClassLoaderDesc;
  private final String   methodName;
  private final String   paramDesc;
  private final Object[] parameters;

  // public static Object[] prepareParameters(Object[] params, LiteralValues literals, ClientObjectManager
  // objectManager) {
  // final Object[] rv = new Object[params.length];
  // for (int i = 0; i < params.length; i++) {
  // Object param = params[i];
  // if (literals.isLiteralInstance(param)) {
  // rv[i] = param;
  // } else {
  // rv[i] = objectManager.lookupOrCreate(param).getObjectID();
  // }
  // }
  // }

  public DmiDescriptor(ObjectID receiverID, String receiverClassName, String receiverClassLoaderDesc,
                       String methodDesc, Object[] parameters) {
    this(receiverID, receiverClassName, receiverClassLoaderDesc, methodDesc.substring(0, methodDesc.indexOf('(')),
         methodDesc.substring(methodDesc.indexOf('(')), parameters);
  }

  /**
   * @param parameters can contain literals or ObjectID's
   */
  public DmiDescriptor(ObjectID receiverID, String receiverClassName, String receiverClassLoaderDesc,
                       String methodName, String paramDesc, Object[] parameters) {
    Assert.pre(receiverID != null);
    Assert.pre(receiverClassName != null);
    Assert.pre(receiverClassLoaderDesc != null);
    Assert.pre(methodName != null);
    Assert.pre(paramDesc != null);
    Assert.pre(parameters != null);

    this.receiverID = receiverID;
    this.receiverClassName = receiverClassName;
    this.receiverClassLoaderDesc = receiverClassLoaderDesc;
    this.methodName = methodName;
    this.paramDesc = paramDesc;
    this.parameters = parameters;
  }

  public String getReceiverClassName() {
    return receiverClassName;
  }

  public String getReceiverClassLoaderDesc() {
    return receiverClassLoaderDesc;
  }

  public String getParameterDesc() {
    return paramDesc;
  }

  public String getMethodName() {
    return methodName;
  }

  public ObjectID getReceiverID() {
    return receiverID;
  }

  public static boolean isObjectID(Object o) {
    return (o instanceof ObjectID);
  }

  public final Object[] getParameters() {
    return parameters;
  }

  public String toString() {
    return super.toString() + "[" + receiverClassName + ";  " + receiverID + ";  " + methodName + paramDesc + ";  "
           + ";  " + receiverClassLoaderDesc + "]";
  }

  /**
   * Use this method for efficient server-side reading
   */
  public static DmiDescriptor readFrom(TCByteBufferInputStream in) throws IOException {
    // FIXME: this must be optimized, but for now...
    try {
      return readFrom(in, new DNAEncoding(DNAEncoding.SERIALIZER));
    } catch (ClassNotFoundException e) {
      throw new IOException(e.getMessage());
    }
  }

  public static DmiDescriptor readFrom(TCByteBufferInputStream in, DNAEncoding encoding) throws IOException,
      ClassNotFoundException {
    final ObjectID receiverId = new ObjectID(in.readLong());
    final int buffLength = in.readInt();
    final String receiverClassName = in.readString();
    final String receiverClassLoaderDesc = in.readString();
    final String methodName = in.readString();
    final String paramDesc = in.readString();
    final Object[] params = (Object[]) encoding.decode(in);
    final DmiDescriptor rv = new DmiDescriptor(receiverId, receiverClassName, receiverClassLoaderDesc, methodName,
                                               paramDesc, params);
    return rv;
  }

  public void writeTo(TCByteBufferOutputStream out, DNAEncoding encoding) {
    out.writeLong(receiverID.toLong());
    final Mark mark = out.mark();
    out.writeInt(-1);
    final int start = out.getBytesWritten();
    out.writeString(receiverClassName);
    out.writeString(receiverClassLoaderDesc);
    out.writeString(methodName);
    out.writeString(paramDesc);
    encoding.encodeArray(parameters, out);
    final int length = out.getBytesWritten() - start;
    mark.write(Conversion.int2Bytes(length));
  }
}
