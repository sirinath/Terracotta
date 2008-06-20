/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.logging;

import org.apache.commons.io.FileUtils;

import com.tc.exception.ImplementMe;
import com.tc.io.TCFile;
import com.tc.io.TCFileImpl;
import com.tc.object.config.schema.NewL2DSOConfig;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CallbackDirtyDatabaseCleanUpAdapter implements CallbackOnExitHandler {

  private File                      l2DataPath;
  private TCLogger                  logger;
  private CallbackOnExitActionState actionState = new CallbackOnExitActionState();

  public CallbackDirtyDatabaseCleanUpAdapter(TCLogger logger, File l2DataPath) {
    this.logger = logger;
    this.l2DataPath = l2DataPath;
  }

  public void callbackOnExit() {
    throw new ImplementMe();
  }

  public void callbackOnExit(Throwable t) {
    String errorMessage = "\n\nTerracotta Persistent-data startup exception:\n\n";
    System.err.print(errorMessage);
    t.printStackTrace(System.err);
    System.err.flush();

    actionState.actionFailure(); // lets move to success once everything is done
    boolean dirtyDbAutoDelete = TCPropertiesImpl.getProperties()
        .getBoolean(TCPropertiesConsts.L2_NHA_DIRTYDB_AUTODELETE);

    String statusMessage;
    if (!dirtyDbAutoDelete) {
      statusMessage = "Dirty DB Auto-delete not requested. Exiting..";
      logger.info(statusMessage);
      System.err.println(statusMessage);
      return;
    } else {
      statusMessage = "Dirty DB Auto-delete requested.";
      logger.info(statusMessage);
      System.err.println(statusMessage);
    }

    String dataPath = this.l2DataPath.getAbsolutePath();
    Assert.assertNotBlank(dataPath);

    TCFile dirtyDbBackupPath = new TCFileImpl(new File(dataPath + File.separator
                                                       + NewL2DSOConfig.DIRTY_OBJECTDB_BACKUP_DIRNAME));
    if (!dirtyDbBackupPath.exists()) {
      statusMessage = "Creating dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath();
      logger.info(statusMessage);
      System.err.println(statusMessage);
      try {
        dirtyDbBackupPath.forceMkdir();
      } catch (IOException ioe) {
        throw new RuntimeException("Not able to create Dirty DB Backup Directory '"
                                   + dirtyDbBackupPath.getFile().getAbsolutePath() + "'");
      }
    } else {
      statusMessage = "dirtyDbBackupPath : " + dirtyDbBackupPath.getFile().getAbsolutePath();
      logger.info(statusMessage);
    }

    SimpleDateFormat dateFormat = new SimpleDateFormat("yyMMddHHmmss");
    Date d = new Date();
    String timeStamp = dateFormat.format(d);
    File dirtyDbSourcedir = new File(dataPath + File.separator + NewL2DSOConfig.OBJECTDB_DIRNAME + File.separator);
    File dirtyDbBackupDestDir = new File(dirtyDbBackupPath + File.separator
                                         + NewL2DSOConfig.DIRTY_OBJECTDB_BACKUP_PREFIX + timeStamp);

    try {
      FileUtils.copyDirectoryToDirectory(dirtyDbSourcedir, dirtyDbBackupDestDir);
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }

    try {
      FileUtils.forceDelete(dirtyDbSourcedir);
    } catch (IOException e1) {
      throw new RuntimeException(e1);
    }

    File reasonFile = new File(dirtyDbBackupDestDir, "reason.txt");
    try {
      FileOutputStream out = new FileOutputStream(reasonFile);
      out.write(d.toString().getBytes());
      out.write(errorMessage.getBytes());
      PrintStream ps = new PrintStream(out);
      t.printStackTrace(ps);
      out.close();
    } catch (Exception e1) {
      throw new RuntimeException(e1);
    }
    logger.info("Successfully moved dirty objectdb to " + dirtyDbBackupDestDir.getAbsolutePath() + ".");
    actionState.actionSuccess();
  }

  public CallbackOnExitActionState getCallbackOnExitActionState() {
    return this.actionState;
  }

  public boolean isRestartNeeded() {
    return true;
  }
}