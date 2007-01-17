/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.bundles;

public final class BundleException extends Exception {

  public BundleException() {
    super();
  }

  public BundleException(String message) {
    super(message);
  }

  public BundleException(Throwable cause) {
    super(cause);
  }

  public BundleException(String message, Throwable cause) {
    super(message, cause);
  }

}
