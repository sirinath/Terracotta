/**
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.concurrent;

/**
 * Thrown by TCFuture to indicate an exception (as opposed to a non-exception) result
 * 
 * @author teck
 */
public class TCExceptionResultException extends Exception {

  public TCExceptionResultException() {
    super();
  }

  public TCExceptionResultException(String message) {
    super(message);

  }

  public TCExceptionResultException(Throwable cause) {
    super(cause);
  }

  public TCExceptionResultException(String message, Throwable cause) {
    super(message, cause);
  }

}