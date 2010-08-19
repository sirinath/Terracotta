/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.objectserver.storage.berkeleydb;

import com.sleepycat.je.Database;
import com.sleepycat.je.Transaction;
import com.tc.objectserver.storage.api.PersistenceTransaction;
import com.tc.util.Assert;

public abstract class AbstractBerkeleyDatabase {
  protected final Database db;

  public AbstractBerkeleyDatabase(Database db) {
    this.db = db;
  }

  protected Transaction pt2nt(PersistenceTransaction tx) {
    Object o = tx.getTransaction();
    if (o != null) {
      Assert.eval(o instanceof Transaction);
      return (Transaction) o;
    } else {
      return null;
    }
  }

  public final Database getDatabase() {
    return db;
  }
}
