/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_87 extends OrganicObjectGraph {

  private int size = 8;
  private int[] types = new int[] { 3, 2, 0, 3, 2, 2, 1, 0 };

  private double f0;
  private short f1;
  private int f2;
  private double f3;
  private short f4;
  private short f5;
  private String f6;
  private int f7;

  public OrganicObjectGraphNode_87(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_87() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 0:
        f0 = value;
      case 3:
        f3 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 1:
        f1 = value;
      case 4:
        f4 = value;
      case 5:
        f5 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 2:
        f2 = value;
      case 7:
        f7 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 6:
        f6 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_87)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_87"); return false; }
    OrganicObjectGraphNode_87 obj = (OrganicObjectGraphNode_87) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (!("" + f6).equals("" + obj.f6)) return false;
    if (f7 != obj.f7) return false;
    return super.equals(obj);
  }
}
