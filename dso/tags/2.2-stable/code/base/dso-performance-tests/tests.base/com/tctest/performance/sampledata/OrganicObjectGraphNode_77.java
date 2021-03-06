/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_77 extends OrganicObjectGraph {

  private int size = 13;
  private int[] types = new int[] { 3, 3, 0, 0, 3, 1, 2, 2, 0, 3, 2, 2, 3 };

  private double f0;
  private double f1;
  private int f2;
  private int f3;
  private double f4;
  private String f5;
  private short f6;
  private short f7;
  private int f8;
  private double f9;
  private short f10;
  private short f11;
  private double f12;

  public OrganicObjectGraphNode_77(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_77() {
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
      case 1:
        f1 = value;
      case 4:
        f4 = value;
      case 9:
        f9 = value;
      case 12:
        f12 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 2:
        f2 = value;
      case 3:
        f3 = value;
      case 8:
        f8 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 5:
        f5 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 6:
        f6 = value;
      case 7:
        f7 = value;
      case 10:
        f10 = value;
      case 11:
        f11 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_77)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_77"); return false; }
    OrganicObjectGraphNode_77 obj = (OrganicObjectGraphNode_77) rawObj;
    if (f0 != obj.f0) return false;
    if (f1 != obj.f1) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (!("" + f5).equals("" + obj.f5)) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (f8 != obj.f8) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (f11 != obj.f11) return false;
    if (f12 != obj.f12) return false;
    return super.equals(obj);
  }
}
