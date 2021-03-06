/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.persistence.sleepycat.util;

import com.tc.objectserver.persistence.sleepycat.AbstractDBUtilsTestBase;
import com.tc.objectserver.persistence.sleepycat.SleepycatPersistor;
import com.tc.objectserver.persistence.sleepycat.util.DBDiff;

import java.io.File;

public class DBDiffTest extends AbstractDBUtilsTestBase {

  public void testSleepycatTCDBDiff() throws Exception {

    File databaseDir1 = new File(getTempDirectory().toString() + File.separator + "db-data-1");
    databaseDir1.mkdirs();
    
    File databaseDir2 = new File(getTempDirectory().toString() + File.separator + "db-data-2");
    databaseDir2.mkdirs();

    SleepycatPersistor sleepycatPersistor1 = getSleepycatPersistor(databaseDir1);
    populateSleepycatDB(sleepycatPersistor1);
    sleepycatPersistor1.close();
    
    reset();
    
    SleepycatPersistor sleepycatPersistor2 = getSleepycatPersistor(databaseDir2);
    populateSleepycatDB(sleepycatPersistor2);
    sleepycatPersistor2.close();


    DBDiff sleepycatTCDBDiff = new DBDiff(databaseDir1, databaseDir2, false);
    sleepycatTCDBDiff.diff();
    
    assertEquals(sleepycatTCDBDiff.diffStringIndexer, false);
    assertEquals(sleepycatTCDBDiff.diffManagedObjects, false);
    assertEquals(sleepycatTCDBDiff.diffClientStates, false);
    assertEquals(sleepycatTCDBDiff.diffTransactions, false);
    assertEquals(sleepycatTCDBDiff.diffGeneratedClasses, false);
    
    
  }

}
