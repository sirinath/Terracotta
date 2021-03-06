/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import org.apache.commons.io.FileUtils;

import com.sleepycat.bind.serial.ClassCatalog;
import com.sleepycat.bind.serial.StoredClassCatalog;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.EnvironmentStats;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.sleepycat.je.StatsConfig;
import com.sleepycat.je.Transaction;
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.util.concurrent.ThreadUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class DBEnvironment {

  private static final TCLogger            clogger                     = CustomerLogging.getDSOGenericLogger();
  private static final TCLogger            logger                      = TCLogging.getLogger(DBEnvironment.class);

  private static final String              GLOBAL_SEQUENCE_DATABASE    = "global_sequence_db";
  private static final String              ROOT_DB_NAME                = "roots";
  private static final String              OBJECT_DB_NAME              = "objects";
  private static final String              OBJECT_OID_STORE_DB_NAME    = "objects_oid_store";
  private static final String              MAPS_OID_STORE_DB_NAME      = "mapsdatabase_oid_store";
  private static final String              OID_STORE_LOG_DB_NAME       = "oid_store_log";

  private static final String              CLIENT_STATE_DB_NAME        = "clientstates";
  private static final String              TRANSACTION_DB_NAME         = "transactions";
  private static final String              STRING_INDEX_DB_NAME        = "stringindex";
  private static final String              CLASS_DB_NAME               = "classdefinitions";
  private static final String              MAP_DB_NAME                 = "mapsdatabase";
  private static final String              CLUSTER_STATE_STORE         = "clusterstatestore";

  private static final Object              CONTROL_LOCK                = new Object();

  private static final DBEnvironmentStatus STATUS_INIT                 = new DBEnvironmentStatus("INIT");
  private static final DBEnvironmentStatus STATUS_ERROR                = new DBEnvironmentStatus("ERROR");
  private static final DBEnvironmentStatus STATUS_OPENING              = new DBEnvironmentStatus("OPENING");
  private static final DBEnvironmentStatus STATUS_OPEN                 = new DBEnvironmentStatus("OPEN");
  private static final DBEnvironmentStatus STATUS_CLOSING              = new DBEnvironmentStatus("CLOSING");
  private static final DBEnvironmentStatus STATUS_CLOSED               = new DBEnvironmentStatus("CLOSED");

  private static final DatabaseEntry       CLEAN_FLAG_KEY              = new DatabaseEntry(new byte[] { 1 });
  private static final byte                IS_CLEAN                    = 1;
  private static final byte                IS_DIRTY                    = 2;
  private static final long                SLEEP_TIME_ON_STARTUP_ERROR = 500;
  private static final int                 STARTUP_RETRY_COUNT         = 5;

  private final List                       createdDatabases;
  private final Map                        databasesByName;
  private final File                       envHome;
  private EnvironmentConfig                ecfg;
  private DatabaseConfig                   dbcfg;
  private ClassCatalogWrapper              catalog;

  private Environment                      env;
  private Database                         controlDB;
  private DBEnvironmentStatus              status                      = STATUS_INIT;
  private DatabaseOpenResult               openResult                  = null;

  private final boolean                    paranoid;

  public DBEnvironment(boolean paranoid, File envHome) throws IOException {
    this(paranoid, envHome, new Properties());
  }

  public DBEnvironment(boolean paranoid, File envHome, Properties jeProperties) throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome);
    this.ecfg = new EnvironmentConfig(jeProperties);
    this.ecfg.setTransactional(true);
    this.ecfg.setAllowCreate(true);
    this.ecfg.setReadOnly(false);
    // this.ecfg.setTxnWriteNoSync(!paranoid);
    this.ecfg.setTxnNoSync(!paranoid);
    this.dbcfg = new DatabaseConfig();
    this.dbcfg.setAllowCreate(true);
    this.dbcfg.setTransactional(true);

    logger.info("Env config = " + this.ecfg + " DB Config = " + this.dbcfg + " JE Properties = " + jeProperties);
  }

  // For tests
  DBEnvironment(boolean paranoid, File envHome, EnvironmentConfig ecfg, DatabaseConfig dbcfg) throws IOException {
    this(new HashMap(), new LinkedList(), paranoid, envHome, ecfg, dbcfg);
  }

  // For tests
  DBEnvironment(Map databasesByName, List createdDatabases, boolean paranoid, File envHome, EnvironmentConfig ecfg,
                DatabaseConfig dbcfg) throws IOException {
    this(databasesByName, createdDatabases, paranoid, envHome);
    this.ecfg = ecfg;
    this.dbcfg = dbcfg;
  }

  /**
   * Note: it is not currently safe to create more than one of these instances in the same process. Sleepycat is
   * supposed to keep more than one process from opening a writable handle to the same database, but it allows you to
   * create more than one writable handle within the same process. So, don't do that.
   */
  private DBEnvironment(Map databasesByName, List createdDatabases, boolean paranoid, File envHome) throws IOException {
    this.databasesByName = databasesByName;
    this.createdDatabases = createdDatabases;
    this.paranoid = paranoid;
    this.envHome = envHome;
    FileUtils.forceMkdir(this.envHome);
  }

  public boolean isParanoidMode() {
    return paranoid;
  }

  public synchronized DatabaseOpenResult open() throws TCDatabaseException {
    if ((status != STATUS_INIT) && (status != STATUS_CLOSED)) { throw new DatabaseOpenException(
                                                                                                "Database environment isn't in INIT/CLOSED state."); }

    status = STATUS_OPENING;
    try {
      env = openEnvironment();
      synchronized (CONTROL_LOCK) {
        // XXX: Note: this doesn't guard against multiple instances in different
        // classloaders...
        controlDB = env.openDatabase(null, "control", this.dbcfg);
        openResult = new DatabaseOpenResult(isClean());
        if (!openResult.isClean()) {
          this.status = STATUS_INIT;
          forceClose();
          return openResult;
        }
      }
      if (!this.paranoid) setDirty();
      this.catalog = new ClassCatalogWrapper(env, dbcfg);
      newDatabase(env, GLOBAL_SEQUENCE_DATABASE);
      newDatabase(env, OBJECT_DB_NAME);
      newDatabase(env, OBJECT_OID_STORE_DB_NAME);
      newDatabase(env, MAPS_OID_STORE_DB_NAME);
      newDatabase(env, OID_STORE_LOG_DB_NAME);
      newDatabase(env, ROOT_DB_NAME);

      newDatabase(env, CLIENT_STATE_DB_NAME);
      newDatabase(env, TRANSACTION_DB_NAME);
      newDatabase(env, STRING_INDEX_DB_NAME);
      newDatabase(env, CLASS_DB_NAME);
      newDatabase(env, MAP_DB_NAME);
      newDatabase(env, CLUSTER_STATE_STORE);
    } catch (DatabaseException e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw new TCDatabaseException(e);
    } catch (Error e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw e;
    } catch (RuntimeException e) {
      this.status = STATUS_ERROR;
      forceClose();
      throw e;
    }

    this.status = STATUS_OPEN;
    return openResult;
  }

  private void cinfo(Object message) {
    clogger.info("DB Environment: " + message);
  }

  public synchronized void close() throws TCDatabaseException {
    assertOpen();
    status = STATUS_CLOSING;
    cinfo("Closing...");

    try {
      for (Iterator i = createdDatabases.iterator(); i.hasNext();) {
        Database db = (Database) i.next();
        cinfo("Closing database: " + db.getDatabaseName() + "...");
        db.close();
      }
      cinfo("Closing class catalog...");
      this.catalog.close();
      setClean();
      if (this.controlDB != null) {
        cinfo("Closing control database...");
        this.controlDB.close();
      }
      if (this.env != null) {
        cinfo("Closing environment...");
        this.env.close();
      }
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
    this.controlDB = null;
    this.env = null;

    status = STATUS_CLOSED;
    cinfo("Closed.");
  }

  public synchronized boolean isOpen() {
    return STATUS_OPEN.equals(status);
  }

  // This is for testing and cleanup on error.
  synchronized void forceClose() {
    List toClose = new ArrayList(createdDatabases);
    toClose.add(controlDB);
    for (Iterator i = toClose.iterator(); i.hasNext();) {
      try {
        Database db = (Database) i.next();
        if (db != null) db.close();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    try {
      if (this.catalog != null) this.catalog.close();
    } catch (Exception e) {
      e.printStackTrace();
    }

    try {
      if (env != null) env.close();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public File getEnvironmentHome() {
    return envHome;
  }

  public synchronized Environment getEnvironment() throws TCDatabaseException {
    assertOpen();
    return env;
  }

  public EnvironmentStats getStats(StatsConfig config) throws TCDatabaseException {
    try {
      return env.getStats(config);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  public synchronized Database getObjectDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(OBJECT_DB_NAME);
  }

  public synchronized Database getObjectOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(OBJECT_OID_STORE_DB_NAME);
  }

  public synchronized Database getMapsOidStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(MAPS_OID_STORE_DB_NAME);
  }

  public synchronized Database getOidStoreLogDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(OID_STORE_LOG_DB_NAME);
  }

  public synchronized ClassCatalogWrapper getClassCatalogWrapper() throws TCDatabaseException {
    assertOpen();
    return catalog;
  }

  public synchronized Database getRootDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(ROOT_DB_NAME);
  }

  public Database getClientStateDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(CLIENT_STATE_DB_NAME);
  }

  public Database getTransactionDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(TRANSACTION_DB_NAME);
  }

  public Database getGlobalSequenceDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(GLOBAL_SEQUENCE_DATABASE);
  }

  public Database getClassDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(CLASS_DB_NAME);
  }

  public Database getMapsDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(MAP_DB_NAME);
  }

  public Database getStringIndexDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(STRING_INDEX_DB_NAME);
  }

  public Database getClusterStateStoreDatabase() throws TCDatabaseException {
    assertOpen();
    return (Database) databasesByName.get(CLUSTER_STATE_STORE);
  }

  private void assertNotError() throws TCDatabaseException {
    if (STATUS_ERROR == status) throw new TCDatabaseException("Attempt to operate on an environment in an error state.");
  }

  private void assertOpening() {
    if (STATUS_OPENING != status) throw new AssertionError("Database environment should be opening but isn't");
  }

  private void assertOpen() throws TCDatabaseException {
    assertNotError();
    if (STATUS_OPEN != status) throw new DatabaseNotOpenException("Database environment should be open but isn't.");
  }

  private void assertClosing() {
    if (STATUS_CLOSING != status) throw new AssertionError("Database environment should be closing but isn't");
  }

  private boolean isClean() throws TCDatabaseException {
    assertOpening();
    DatabaseEntry value = new DatabaseEntry(new byte[] { 0 });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.get(tx, CLEAN_FLAG_KEY, value, LockMode.DEFAULT);
      tx.commit();
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    return OperationStatus.NOTFOUND.equals(stat)
           || (OperationStatus.SUCCESS.equals(stat) && value.getData()[0] == IS_CLEAN);
  }

  private void setDirty() throws TCDatabaseException {
    assertOpening();
    DatabaseEntry value = new DatabaseEntry(new byte[] { IS_DIRTY });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) throw new TCDatabaseException("Unexpected operation status "
                                                                             + "trying to unset clean flag: " + stat);
    try {
      tx.commitSync();
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private Transaction newTransaction() throws TCDatabaseException {
    try {
      Transaction tx = env.beginTransaction(null, null);
      return tx;
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private void setClean() throws TCDatabaseException {
    assertClosing();
    DatabaseEntry value = new DatabaseEntry(new byte[] { IS_CLEAN });
    Transaction tx = newTransaction();
    OperationStatus stat;
    try {
      stat = controlDB.put(tx, CLEAN_FLAG_KEY, value);
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
    if (!OperationStatus.SUCCESS.equals(stat)) throw new TCDatabaseException("Unexpected operation status "
                                                                             + "trying to set clean flag: " + stat);
    try {
      tx.commitSync();
    } catch (Exception e) {
      throw new TCDatabaseException(e.getMessage());
    }
  }

  private void newDatabase(Environment e, String name) throws TCDatabaseException {
    try {
      Database db = e.openDatabase(null, name, dbcfg);
      createdDatabases.add(db);
      databasesByName.put(name, db);
    } catch (Exception de) {
      throw new TCDatabaseException(de.getMessage());
    }
  }

  private Environment openEnvironment() throws TCDatabaseException {
    int count = 0;
    while (true) {
      try {
        return new Environment(envHome, ecfg);
      } catch (Exception dbe) {
        if (++count <= STARTUP_RETRY_COUNT) {
          logger.warn("Unable to open DB environment. " + dbe.getMessage() + " Retrying after "
                      + SLEEP_TIME_ON_STARTUP_ERROR + " ms");
          ThreadUtil.reallySleep(SLEEP_TIME_ON_STARTUP_ERROR);
        } else {
          throw new TCDatabaseException(dbe.getMessage());
        }
      }
    }
  }

  public static final String getClusterStateStoreName() {
    return CLUSTER_STATE_STORE;
  }

  private static final class DBEnvironmentStatus {
    private final String description;

    DBEnvironmentStatus(String desc) {
      this.description = desc;
    }

    public String toString() {
      return this.description;
    }
  }

  public static final class ClassCatalogWrapper {

    private final StoredClassCatalog catalog;
    private boolean                  closed = false;

    private ClassCatalogWrapper(Environment env, DatabaseConfig cfg) throws DatabaseException {
      catalog = new StoredClassCatalog(env.openDatabase(null, "java_class_catalog", cfg));
    }

    public final ClassCatalog getClassCatalog() {
      return this.catalog;
    }

    synchronized void close() throws DatabaseException {
      if (closed) throw new IllegalStateException("Already closed.");
      this.catalog.close();
      closed = true;
    }
  }

}