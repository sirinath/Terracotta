/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

import java.util.EventListener;

public interface DBBackupListener extends EventListener {
  void backupEnabled();

  void backupStarted();

  void backupCompleted();

  void backupFailed(String message);

  void backupProgress(int percentCopied);
}
