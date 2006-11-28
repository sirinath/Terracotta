/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.lockmanager.api;

import junit.framework.TestCase;

/**
 * TODO Nov 18, 2004:
 */
public class LockIDTest extends TestCase {
  public void tests() throws Exception {
    assertTrue(LockLevel.isWrite(LockLevel.WRITE));
    assertFalse(LockLevel.isRead(LockLevel.WRITE));

    assertFalse(LockLevel.isWrite(LockLevel.READ));
    assertTrue(LockLevel.isRead(LockLevel.READ));

  }
}