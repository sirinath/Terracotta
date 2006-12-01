/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */

package com.tctest.spring.bean;


public class ScopedBean {
  private String value = "Jonas";
  
  public String getValue() {
    return value;
  }
  
  public void setValue(String value) {
    this.value = value;
  }

}

