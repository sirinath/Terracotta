package com.tctest.performance.sampledata;

public final class OrganicObjectGraphNode_61 extends OrganicObjectGraph {

  private int size = 47;
  private int[] types = new int[] { 2, 1, 0, 2, 0, 0, 2, 0, 1, 0, 0, 1, 1, 2, 1, 0, 0, 3, 1, 2, 0, 0, 3, 0, 1, 0, 3, 0, 0, 3, 3, 2, 1, 3, 3, 3, 0, 2, 3, 3, 1, 2, 3, 1, 0, 3, 0 };

  private short f0;
  private String f1;
  private int f2;
  private short f3;
  private int f4;
  private int f5;
  private short f6;
  private int f7;
  private String f8;
  private int f9;
  private int f10;
  private String f11;
  private String f12;
  private short f13;
  private String f14;
  private int f15;
  private int f16;
  private double f17;
  private String f18;
  private short f19;
  private int f20;
  private int f21;
  private double f22;
  private int f23;
  private String f24;
  private int f25;
  private double f26;
  private int f27;
  private int f28;
  private double f29;
  private double f30;
  private short f31;
  private String f32;
  private double f33;
  private double f34;
  private double f35;
  private int f36;
  private short f37;
  private double f38;
  private double f39;
  private String f40;
  private short f41;
  private double f42;
  private String f43;
  private int f44;
  private double f45;
  private int f46;

  public OrganicObjectGraphNode_61(int sequenceNumber, String envKey) {
    super(sequenceNumber, envKey);
  }

  public OrganicObjectGraphNode_61() {
    super();
  }

  protected int getSize() {
    return size;
  }

  protected int getType(int index) {
    return types[index];
  }

  protected void setValue(int index, short value) {
    switch (index) {
      case 0:
        f0 = value;
      case 3:
        f3 = value;
      case 6:
        f6 = value;
      case 13:
        f13 = value;
      case 19:
        f19 = value;
      case 31:
        f31 = value;
      case 37:
        f37 = value;
      case 41:
        f41 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, String value) {
    switch (index) {
      case 1:
        f1 = value;
      case 8:
        f8 = value;
      case 11:
        f11 = value;
      case 12:
        f12 = value;
      case 14:
        f14 = value;
      case 18:
        f18 = value;
      case 24:
        f24 = value;
      case 32:
        f32 = value;
      case 40:
        f40 = value;
      case 43:
        f43 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, int value) {
    switch (index) {
      case 2:
        f2 = value;
      case 4:
        f4 = value;
      case 5:
        f5 = value;
      case 7:
        f7 = value;
      case 9:
        f9 = value;
      case 10:
        f10 = value;
      case 15:
        f15 = value;
      case 16:
        f16 = value;
      case 20:
        f20 = value;
      case 21:
        f21 = value;
      case 23:
        f23 = value;
      case 25:
        f25 = value;
      case 27:
        f27 = value;
      case 28:
        f28 = value;
      case 36:
        f36 = value;
      case 44:
        f44 = value;
      case 46:
        f46 = value;
      default:
        break;
    }
  }

  protected void setValue(int index, double value) {
    switch (index) {
      case 17:
        f17 = value;
      case 22:
        f22 = value;
      case 26:
        f26 = value;
      case 29:
        f29 = value;
      case 30:
        f30 = value;
      case 33:
        f33 = value;
      case 34:
        f34 = value;
      case 35:
        f35 = value;
      case 38:
        f38 = value;
      case 39:
        f39 = value;
      case 42:
        f42 = value;
      case 45:
        f45 = value;
      default:
        break;
    }
  }

  public boolean equals(Object rawObj) {
    if (!(rawObj instanceof OrganicObjectGraphNode_61)) { System.out.println("not instanceof"); System.out.println(rawObj.getClass().getName() + "=OrganicObjectGraphNode_61"); return false; }
    OrganicObjectGraphNode_61 obj = (OrganicObjectGraphNode_61) rawObj;
    if (f0 != obj.f0) return false;
    if (!("" + f1).equals("" + obj.f1)) return false;
    if (f2 != obj.f2) return false;
    if (f3 != obj.f3) return false;
    if (f4 != obj.f4) return false;
    if (f5 != obj.f5) return false;
    if (f6 != obj.f6) return false;
    if (f7 != obj.f7) return false;
    if (!("" + f8).equals("" + obj.f8)) return false;
    if (f9 != obj.f9) return false;
    if (f10 != obj.f10) return false;
    if (!("" + f11).equals("" + obj.f11)) return false;
    if (!("" + f12).equals("" + obj.f12)) return false;
    if (f13 != obj.f13) return false;
    if (!("" + f14).equals("" + obj.f14)) return false;
    if (f15 != obj.f15) return false;
    if (f16 != obj.f16) return false;
    if (f17 != obj.f17) return false;
    if (!("" + f18).equals("" + obj.f18)) return false;
    if (f19 != obj.f19) return false;
    if (f20 != obj.f20) return false;
    if (f21 != obj.f21) return false;
    if (f22 != obj.f22) return false;
    if (f23 != obj.f23) return false;
    if (!("" + f24).equals("" + obj.f24)) return false;
    if (f25 != obj.f25) return false;
    if (f26 != obj.f26) return false;
    if (f27 != obj.f27) return false;
    if (f28 != obj.f28) return false;
    if (f29 != obj.f29) return false;
    if (f30 != obj.f30) return false;
    if (f31 != obj.f31) return false;
    if (!("" + f32).equals("" + obj.f32)) return false;
    if (f33 != obj.f33) return false;
    if (f34 != obj.f34) return false;
    if (f35 != obj.f35) return false;
    if (f36 != obj.f36) return false;
    if (f37 != obj.f37) return false;
    if (f38 != obj.f38) return false;
    if (f39 != obj.f39) return false;
    if (!("" + f40).equals("" + obj.f40)) return false;
    if (f41 != obj.f41) return false;
    if (f42 != obj.f42) return false;
    if (!("" + f43).equals("" + obj.f43)) return false;
    if (f44 != obj.f44) return false;
    if (f45 != obj.f45) return false;
    if (f46 != obj.f46) return false;
    return super.equals(obj);
  }
}
