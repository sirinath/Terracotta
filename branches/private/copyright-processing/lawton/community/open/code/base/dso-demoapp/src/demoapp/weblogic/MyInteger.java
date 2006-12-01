/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package demoapp.weblogic;

public class MyInteger {

  private final int i;

  public MyInteger(int i) {
    this.i = i;
  }

  public String toString() {
    return getClass().getName() + "(" + i + ")";
  }

}
