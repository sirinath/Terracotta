/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.io;

import java.io.IOException;
import java.nio.channels.FileLock;

public class TCFileLockImpl implements TCFileLock {
  
  private final FileLock lock;

  public TCFileLockImpl(FileLock lock) {
    this.lock = lock;
  }

  public void release() throws IOException {
    lock.release();
  }
}
