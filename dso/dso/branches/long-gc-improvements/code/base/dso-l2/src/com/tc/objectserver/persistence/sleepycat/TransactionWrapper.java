/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Transaction;
import com.tc.io.serializer.TCCustomByteArrayOutputStream;
import com.tc.objectserver.persistence.api.PersistenceTransaction;

import java.util.HashMap;
import java.util.Map;

class TransactionWrapper implements PersistenceTransaction {
  private final Transaction tx;
  private final Map         properties = new HashMap(1);
  private TCCustomByteArrayOutputStream buffer;

  public TransactionWrapper(Transaction tx) {
    this.tx = tx;
  }

  public Transaction getTransaction() {
    return tx;
  }

  public void commit() {
    if (tx != null) try {
      tx.commit();
    } catch (DatabaseException e) {
      throw new DBException(e);
    } finally {
      if (buffer != null) {
        buffer.reset();
        buffer = null;
      }
    }
  }

  public Object getProperty(Object key) {
    return properties.get(key);
  }

  public Object setProperty(Object key, Object value) {
    return properties.put(key, value);
  }

  public void setBuffer(TCCustomByteArrayOutputStream buffer) {
    if (this.buffer == null) {
      this.buffer = buffer;
    } else if (this.buffer != buffer) {
      throw new RuntimeException("fix me"); //TODO, what should get thrown here
    }
  }
}