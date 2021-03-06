/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.lang;


public class TCThreadGroup extends ThreadGroup {

  private final ThrowableHandler throwableHandler;

  public TCThreadGroup(ThrowableHandler throwableHandler) {
    this(throwableHandler, "TC Thread Group");
  }

  public TCThreadGroup(ThrowableHandler throwableHandler, String name) {
    super(name);
    this.throwableHandler = throwableHandler;
  }

  public void uncaughtException(Thread thread, Throwable throwable) {
    try {
      super.uncaughtException(thread, throwable);
    } finally {
      throwableHandler.handleThrowable(thread, throwable);
    }
  }

}
