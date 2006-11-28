/*
 * Copyright (c) 2003-2006 Terracotta, Inc. All rights reserved.
 */
package com.tc.simulator.crasher;

import com.tc.exception.TCRuntimeException;

public class HelloWorld {
  
  public void run() {
    int count = 0;
    while (true) {
      System.out.println("count=" + count);
      System.err.println("count=" + count);
      try {
        Thread.sleep(1000);
      } catch (InterruptedException e) {
        throw new TCRuntimeException(e);
      }
      count++;
    }
  }
  
  public static void main(String[] args) {
    new HelloWorld().run();
  }
}
