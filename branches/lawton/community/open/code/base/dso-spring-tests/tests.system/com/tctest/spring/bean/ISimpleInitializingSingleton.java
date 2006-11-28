/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tctest.spring.bean;

public interface ISimpleInitializingSingleton {

  String getName();

  long getId();

  long getInnerId();
  
  boolean isTheSameInstance();

}