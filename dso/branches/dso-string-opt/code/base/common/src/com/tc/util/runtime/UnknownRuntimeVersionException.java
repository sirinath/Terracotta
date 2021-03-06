/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.util.runtime;

public final class UnknownRuntimeVersionException extends Exception {
  UnknownRuntimeVersionException(final String jvmVersion, final String badVersion) {
    super("Unable to parse runtime version '" + badVersion + "' for JVM version '" + jvmVersion + "'");
  }
}