/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.admin.model;

public interface IProductVersion {
  String version();

  String patchLevel();

  String patchVersion();

  String license();

  String copyright();

  String buildID();
}
