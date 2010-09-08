/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.io.TCSerializable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public abstract class NVPair implements TCSerializable {

  private static final NVPair      TEMPLATE  = new Template();
  private static final ValueType[] ALL_TYPES = ValueType.values();

  private final String             name;

  NVPair(String name) {
    this.name = name;
  }

  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getType() + "(" + getName() + "," + valueAsString() + ")";
  }

  abstract String valueAsString();

  public Object deserializeFrom(TCByteBufferInput in) throws IOException {
    byte ordinal = in.readByte();
    ValueType type = ALL_TYPES[ordinal];
    return type.deserializeFrom(in);
  }

  public void serializeTo(TCByteBufferOutput out) {
    out.writeString(getName());

    ValueType type = getType();

    out.writeByte(type.ordinal());
    type.serializeTo(this, out);
  }

  public abstract ValueType getType();

  public static NVPair deserializeInstance(TCByteBufferInput in) throws IOException {
    return (NVPair) TEMPLATE.deserializeFrom(in);
  }

  private static class Template extends NVPair {

    Template() {
      super("");
    }

    @Override
    String valueAsString() {
      throw new AssertionError();
    }

    @Override
    public ValueType getType() {
      throw new AssertionError();
    }
  }

  public static class ByteNVPair extends NVPair {
    private final byte value;

    public ByteNVPair(String name, byte value) {
      super(name);
      this.value = value;
    }

    public byte getValue() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.BYTE;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }
  }

  public static class BooleanNVPair extends NVPair {
    private final boolean value;

    public BooleanNVPair(String name, boolean value) {
      super(name);
      this.value = value;
    }

    public boolean getValue() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.BOOLEAN;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

  }

  public static class CharNVPair extends NVPair {
    private final char value;

    public CharNVPair(String name, char value) {
      super(name);
      this.value = value;
    }

    public char getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.CHAR;
    }
  }

  public static class DoubleNVPair extends NVPair {
    private final double value;

    public DoubleNVPair(String name, double value) {
      super(name);
      this.value = value;
    }

    public double getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.DOUBLE;
    }
  }

  public static class FloatNVPair extends NVPair {
    private final float value;

    public FloatNVPair(String name, float value) {
      super(name);
      this.value = value;
    }

    public float getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.FLOAT;
    }
  }

  public static class IntNVPair extends NVPair {
    private final int value;

    public IntNVPair(String name, int value) {
      super(name);
      this.value = value;
    }

    public int getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.INT;
    }
  }

  public static class ShortNVPair extends NVPair {
    private final short value;

    public ShortNVPair(String name, short value) {
      super(name);
      this.value = value;
    }

    public short getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.SHORT;
    }

  }

  public static class LongNVPair extends NVPair {
    private final long value;

    public LongNVPair(String name, long value) {
      super(name);
      this.value = value;
    }

    public long getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return String.valueOf(value);
    }

    @Override
    public ValueType getType() {
      return ValueType.LONG;
    }
  }

  public static class StringNVPair extends NVPair {
    private final String value;

    public StringNVPair(String name, String value) {
      super(name);
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return value;
    }

    @Override
    public ValueType getType() {
      return ValueType.STRING;
    }

  }

  public static class ByteArrayNVPair extends NVPair {
    private final byte[] value;

    public ByteArrayNVPair(String name, byte[] value) {
      super(name);
      this.value = value;
    }

    public byte[] getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      List<Byte> list = new ArrayList<Byte>(value.length);
      for (byte b : value) {
        list.add(b);
      }
      return list.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.BYTE_ARRAY;
    }

  }

  public static class DateNVPair extends NVPair {
    private final Date value;

    public DateNVPair(String name, Date value) {
      super(name);
      this.value = value;
    }

    public Date getValue() {
      return value;
    }

    @Override
    String valueAsString() {
      return value.toString();
    }

    @Override
    public ValueType getType() {
      return ValueType.DATE;
    }
  }

}
