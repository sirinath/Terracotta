/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.object.event;

import com.tc.object.TCObject;

/**
 * TODO Mar 14, 2005: I, steve, am too lazy to write a single sentence describing what this class is for.
 */
public interface DistributedMethodCallManager {
  public void distributedInvoke(Object receiver, TCObject tcObject, String method, Object[] params);

  public void stop(boolean immediate);

  public void start();
}