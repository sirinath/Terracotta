/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseEntry;
import com.tc.io.serializer.TCCustomByteArrayOutputStream;
import com.tc.io.serializer.TCObjectInputStream;
import com.tc.io.serializer.TCObjectOutputStream;
import com.tc.io.serializer.TCCustomByteArrayOutputStream.CustomArray;
import com.tc.io.serializer.api.Serializer;
import com.tc.io.serializer.impl.StringUTFSerializer;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectSerializer;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.io.ByteArrayInputStream;
import java.io.IOException;

public class CustomSerializationAdapter implements SerializationAdapter {

  private final TCCustomByteArrayOutputStream   baout;
  private final TCObjectOutputStream    out;
  private final ManagedObjectSerializer moSerializer;
  private final StringUTFSerializer     stringSerializer;

  private final Object                  serializerLock = new Object();

  public CustomSerializationAdapter(ManagedObjectSerializer moSerializer, StringUTFSerializer stringSerializer) {
    this.moSerializer = moSerializer;
    this.stringSerializer = stringSerializer;
    baout = new TCCustomByteArrayOutputStream();
    out = new TCObjectOutputStream(baout);
  }

  public void serializeManagedObject(DatabaseEntry entry, ManagedObject managedObject, PersistenceTransaction tx) throws IOException {
    synchronized (serializerLock) {
      serialize(entry, managedObject, moSerializer, tx);
    }
  }

  public synchronized void serializeString(DatabaseEntry entry, String string, PersistenceTransaction tx) throws IOException {
    synchronized (serializerLock) {
      serialize(entry, string, stringSerializer, tx);
    }
  }

  private void serialize(DatabaseEntry entry, Object o, Serializer serializer, PersistenceTransaction tx) throws IOException {
    tx.setBuffer(baout);
    serializer.serializeTo(o, out);
    out.flush();
    CustomArray bytes = baout.getCurrentBytes();
    entry.setData(bytes.buffer, bytes.offset, bytes.length);
    baout.endOfChunk();
  }

  public synchronized ManagedObject deserializeManagedObject(DatabaseEntry data) throws IOException,
      ClassNotFoundException {
    return (ManagedObject) deserialize(data, moSerializer);
  }

  public String deserializeString(DatabaseEntry data) throws IOException, ClassNotFoundException {
    return (String) deserialize(data, stringSerializer);
  }

  private Object deserialize(DatabaseEntry entry, Serializer serializer) throws IOException, ClassNotFoundException {
    ByteArrayInputStream bain = new ByteArrayInputStream(entry.getData());
    TCObjectInputStream in = new TCObjectInputStream(bain);
    return serializer.deserializeFrom(in);
  }
}
