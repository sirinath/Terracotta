/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.CursorConfig;
import com.sleepycat.je.Database;
import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.DatabaseEntry;
import com.sleepycat.je.DatabaseException;
import com.sleepycat.je.Environment;
import com.sleepycat.je.EnvironmentConfig;
import com.sleepycat.je.LockMode;
import com.sleepycat.je.OperationStatus;
import com.tc.logging.TCLogging;
import com.tc.object.ObjectID;
import com.tc.objectserver.core.api.ManagedObject;
import com.tc.objectserver.managedobject.ManagedObjectChangeListener;
import com.tc.objectserver.managedobject.ManagedObjectChangeListenerProvider;
import com.tc.objectserver.managedobject.ManagedObjectStateFactory;
import com.tc.objectserver.managedobject.NullManagedObjectChangeListener;
import com.tc.text.PrettyPrinter;
import com.tc.text.PrettyPrinterImpl;
import com.tc.util.Counter;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class SleepycatDBUsage {

  private static final int   LEFT                  = 1;
  private static final int   RIGHT                 = 2;
  private static final int   CENTER                = 3;

  private EnvironmentConfig  enc;
  private Environment        env;
  private DBEnvironment      dbEnv;

  private long               grandTotal;
  private long               totalCount;
  private DatabaseConfig     dbc;
  private boolean            header                = true;
  private long               keyTotal;
  private long               valuesTotal;
  
  private File               dir;
  private SleepycatPersistor persistor;
  protected Map              classMap              = new HashMap();
  protected Set              nullObjectIDSet       = new HashSet();
  protected Counter          objectIDIsNullCounter = new Counter(0);
  protected Set              doesNotExistInSet     = new HashSet();
  protected Counter          totalCounter          = new Counter(0);

  public SleepycatDBUsage(File dir) throws Exception {
    this.dir = dir;
    enc = new EnvironmentConfig();
    enc.setReadOnly(true);
    env = new Environment(dir, enc);
    dbc = new DatabaseConfig();
    dbc.setReadOnly(true);
   
  }
  
  public void printManagedObjectReport() {
    log("---------------------------------- Managed Object Report ----------------------------------------------------");
    log("\t Total number of objects read: " + totalCounter.get());
    log("\t Total number getObjectReferences that yielded isNull references: " + objectIDIsNullCounter.get());
    log("\t Total number of references that does not exist in allObjectIDs set: " + doesNotExistInSet.size());
    log("\t does not exist in allObjectIDs set: " + doesNotExistInSet + " \n");
    log("\t Total number of references without ManagedObjects: " + nullObjectIDSet.size());
    log("\n\t Begin references with null ManagedObject summary --> \n");
    for (Iterator iter = nullObjectIDSet.iterator(); iter.hasNext();) {
      NullObjectData data = (NullObjectData) iter.next();
      log("\t\t " + data);
    }
    log("\t Begin Class Map summary --> \n");

    for (Iterator iter = classMap.keySet().iterator(); iter.hasNext();) {
      String key = (String) iter.next();
      log("\t\t Class: --> " + key + " had --> " + ((Counter) classMap.get(key)).get() + " references");
    }
    log("------------------------------------------End-----------------------------------------------------------------");
  }

  public void managedObjectReport() throws Exception {

    dbEnv = new DBEnvironment(true, dir);
    SerializationAdapterFactory serializationAdapterFactory = new CustomSerializationAdapterFactory();
    final TestManagedObjectChangeListenerProvider managedObjectChangeListenerProvider = new TestManagedObjectChangeListenerProvider();
    persistor = new SleepycatPersistor(TCLogging.getLogger(SleepycatDBUsage.class), dbEnv, serializationAdapterFactory);
    ManagedObjectStateFactory.createInstance(managedObjectChangeListenerProvider, persistor);
    Set objectIDSet = persistor.getManagedObjectPersistor().getAllObjectIDs();
    for (Iterator iter = objectIDSet.iterator(); iter.hasNext();) {
      ObjectID objectID = (ObjectID) iter.next();
      totalCounter.increment();
      ManagedObject managedObject = persistor.getManagedObjectPersistor().loadObjectByID(objectID);
      if (managedObject == null) {
        log("managed object is null : " + objectID);
        nullObjectIDSet.add(new NullObjectData(objectID));
      } else {
        String className = managedObject.getManagedObjectState().getClassName();
        Counter classCounter = (Counter) classMap.get(className);
        if (classCounter == null) {
          classCounter = new Counter(1);
          classMap.put(className, classCounter);
        } else {
          classCounter.increment();
        }
      }

      for (Iterator r = managedObject.getObjectReferences().iterator(); r.hasNext();) {
        ObjectID mid = (ObjectID) r.next();
        totalCounter.increment();
        if (mid == null) {
          log("reference objectID is null and parent: ");
          log(managedObject.toString());
          nullObjectIDSet.add(new NullObjectData(managedObject, null));
        } else {
          if (mid.isNull()) {
            objectIDIsNullCounter.increment();
          } else {
            boolean exitInSet = objectIDSet.contains(mid);
            if (!exitInSet) {
              doesNotExistInSet.add(mid);
            }
          }
        }

      }
    }
  }

  public void report() throws Exception {
    List dbs = env.getDatabaseNames();
    log("Databases in the enviroment : " + dbs);

    log("\nReport on individual databases :\n================================\n");
    for (Iterator i = dbs.iterator(); i.hasNext();) {
      String dbNAme = (String) i.next();
      Database db = env.openDatabase(null, dbNAme, dbc);
      DBStats stats = calculate(db);
      db.close();
      report(stats);
    }
    reportGrandTotals();
    env.close();
    log("\n\n");
    managedObjectReport();
    printManagedObjectReport();
  }

  private void reportGrandTotals() {
    log("\n");
    log("   TOTAL : ", String.valueOf(totalCount), "", "", "", String.valueOf(keyTotal), "", "", "", String
        .valueOf(valuesTotal), String.valueOf(grandTotal));
  }

  private DBStats calculate(Database db) throws DatabaseException {
    CursorConfig config = new CursorConfig();
    Cursor c = db.openCursor(null, config);
    DBStats stats = new DBStats(db.getDatabaseName());
    DatabaseEntry key = new DatabaseEntry();
    DatabaseEntry value = new DatabaseEntry();
    while (OperationStatus.SUCCESS.equals(c.getNext(key, value, LockMode.DEFAULT))) {
      stats.record(key.getData().length, value.getData().length);
    }
    c.close();
    return stats;
  }

  private void report(DBStats stats) {
    if (header) {
      log("DBName", "# Records", "Keys(Bytes)", "Values(Bytes)", "Total(Bytes)");
      log("", "", "min", "max", "avg", "total", "min", "max", "avg", "total", "");
      log("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
      header = false;
    }
    log(stats.getDatabaseName(), stats.getRecordCount(), stats.getKeyMin(), stats.getKeyMax(), stats.getKeyAvg(), stats
        .getTotalKeySize(), stats.getValueMin(), stats.getValueMax(), stats.getValueAvg(), stats.getTotalValueSize(),
        stats.getTotalSize());
    this.keyTotal += stats.getTotalKeySize();
    this.valuesTotal += stats.getTotalValueSize();
    this.grandTotal += stats.getTotalSize();
    this.totalCount += stats.getRecordCount();
  }

  private void log(String databaseName, long recordCount, long keyMin, long keyMax, long keyAvg, long totalKeySize,
                   long valueMin, long valueMax, long valueAvg, long totalValueSize, long totalSize) {
    log(databaseName, String.valueOf(recordCount), String.valueOf(keyMin), String.valueOf(keyMax), String
        .valueOf(keyAvg), String.valueOf(totalKeySize), String.valueOf(valueMin), String.valueOf(valueMax), String
        .valueOf(valueAvg), String.valueOf(totalValueSize), String.valueOf(totalSize));
  }

  private static void log(String nameHeader, String countHeader, String keyHeader, String valueHeader, String sizeHeader) {
    log(format(nameHeader, 20, LEFT) + format(countHeader, 10, RIGHT) + format(keyHeader, 30, CENTER)
        + format(valueHeader, 30, CENTER) + format(sizeHeader, 15, RIGHT));
  }

  private void log(String databaseName, String count, String kmin, String kmax, String kavg, String kTot, String vmin,
                   String vmax, String vavg, String vTot, String totalSize) {
    log(format(databaseName, 20, LEFT) + format(count, 10, RIGHT) + format(kmin, 5, RIGHT) + format(kmax, 10, RIGHT)
        + format(kavg, 5, RIGHT) + format(kTot, 10, RIGHT) + format(vmin, 5, RIGHT) + format(vmax, 10, RIGHT)
        + format(vavg, 5, RIGHT) + format(vTot, 10, RIGHT) + format(totalSize, 15, RIGHT));
  }

  private static String format(String s, int size, int justification) {
    if (s == null || s.length() >= size) { return s; }
    int diff = size - s.length();
    if (justification == LEFT) {
      return s + createSpaces(diff);
    } else if (justification == RIGHT) {
      return createSpaces(diff) + s;
    } else {
      return createSpaces(diff / 2) + s + createSpaces(diff - (diff / 2));
    }
  }

  private static String createSpaces(int i) {
    StringBuffer sb = new StringBuffer();
    while (i-- > 0) {
      sb.append(' ');
    }
    return sb.toString();
  }

  public static void main(String[] args) {
    if (args == null || args.length < 1) {
      usage();
      System.exit(1);
    }

    try {
      File dir = new File(args[0]);
      validateDir(dir);
      SleepycatDBUsage reporter = new SleepycatDBUsage(dir);
      reporter.report();
    } catch (Exception e) {
      e.printStackTrace();
      System.exit(2);
    }
  }

  private static void validateDir(File dir) {
    if (!dir.exists() || !dir.isDirectory()) { throw new RuntimeException("Not a valid directory : " + dir); }
  }

  private static void usage() {
    log("Usage: SleepycatDBUsage <environment home directory>");
  }

  private static void log(String message) {
    System.out.println(message);
  }

  private static final class DBStats {

    private long         count;
    private long         keySize;
    private long         valueSize;
    private long         minKey;
    private long         maxKey;
    private long         minValue;
    private long         maxValue;
    private final String databaseName;

    public DBStats(String databaseName) {
      this.databaseName = databaseName;
    }

    public long getValueAvg() {
      return (count == 0 ? 0 : valueSize / count);
    }

    public long getValueMax() {
      return maxValue;
    }

    public long getValueMin() {
      return minValue;
    }

    public long getKeyAvg() {
      return (count == 0 ? 0 : keySize / count);
    }

    public long getKeyMax() {
      return maxKey;
    }

    public long getKeyMin() {
      return minKey;
    }

    public long getTotalValueSize() {
      return valueSize;
    }

    public long getTotalKeySize() {
      return keySize;
    }

    public long getTotalSize() {
      return keySize + valueSize;
    }

    public String getValueStats() {
      return valueSize + "(" + minValue + "/" + maxValue + "/" + getValueAvg() + ")";
    }

    public String getKeyStats() {
      return keySize + "(" + minKey + "/" + maxKey + "/" + getKeyAvg() + ")";
    }

    public long getRecordCount() {
      return count;
    }

    public String getDatabaseName() {
      return databaseName;
    }

    public void record(int kSize, int vSize) {
      count++;
      keySize += kSize;
      valueSize += vSize;
      if (minKey == 0 || minKey > kSize) {
        minKey = kSize;
      }
      if (maxKey < kSize) {
        maxKey = kSize;
      }
      if (minValue == 0 || minValue > vSize) {
        minValue = vSize;
      }
      if (maxValue < vSize) {
        maxValue = vSize;
      }
    }

  }

  private static class NullObjectData {

    private ManagedObject parent;

    private ObjectID      objectID;

    public NullObjectData(ObjectID objectID) {
      this(null, objectID);
    }

    public NullObjectData(ManagedObject parent, ObjectID objectID) {
      this.parent = parent;
      this.objectID = objectID;
    }

    public ManagedObject getParent() {
      return parent;
    }

    public ObjectID getObjectID() {
      return objectID;
    }

    public String toString() {
      StringWriter writer = new StringWriter();
      PrintWriter pWriter = new PrintWriter(writer);
      PrettyPrinter out = new PrettyPrinterImpl(pWriter);
      out.println();
      out.print("Summary of reference with null ManagedObject").duplicateAndIndent().println();
      out.indent().print("identityHashCode: " + System.identityHashCode(this)).println();
      out.indent().print("objectID: " + objectID).println();
      out.indent().print("parent:" + parent).println();

      return writer.getBuffer().toString();
    }

  }

  private static class TestManagedObjectChangeListenerProvider implements ManagedObjectChangeListenerProvider {

    public ManagedObjectChangeListener getListener() {
      return new NullManagedObjectChangeListener();

    }
  }

}
