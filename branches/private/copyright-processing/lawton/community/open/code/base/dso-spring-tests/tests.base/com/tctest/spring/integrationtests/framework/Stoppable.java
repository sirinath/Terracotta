/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.integrationtests.framework;

public interface Stoppable {

  void start() throws Exception;

  void stop() throws Exception;

  public void stopIgnoringExceptions();

  boolean isStopped();

}
