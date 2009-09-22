/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.locks;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.object.LiteralValues;
import com.tc.object.ObjectID;
import com.tc.object.TCObject;
import com.tc.object.bytecode.ManagerUtil;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;

public class DsoLockID implements LockID {
  public final static DsoLockID NULL_ID = new DsoLockID(ObjectID.NULL_ID);
  
  private ObjectID oid;
  private Object   obj;
  
  public DsoLockID() {
    // for tc serialization
  }
  
  public DsoLockID(Object obj) {
    this.obj = obj;
    
    TCObject tco = ManagerUtil.lookupExistingOrNull(obj);
    if (tco != null) {
      this.oid = tco.getObjectID();
    }
  }

  public DsoLockID(ObjectID oid) {
    this.oid = oid;
  }
  
  public String asString() {
    return null;
  }

  public LockIDType getLockType() {
    return LockIDType.DSO;
  }

  public boolean isNull() {
    return this == NULL_ID;
  }

  public Object javaObject() {
    return obj;
  }
  
  public boolean isClustered() {
    if (oid != null) {
      if (obj != null) {
        return !ManagerUtil.lookupExistingOrNull(obj).autoLockingDisabled();
      } else {
        //this is not strictly correct
        return true;
      }
    } else {
      switch (LiteralValues.valueFor(obj)) {
        case BIG_DECIMAL:
        case BIG_INTEGER:
        case INTEGER:
          return true;
        case OBJECT:
        case OBJECT_ID:
        case JAVA_LANG_CLASS:
          return false;
        default: //want this default to disappear eventually...
          System.err.println("XXXXXXXXX NOT LOCKING ON LITERAL CLASS " + obj.getClass());
          return false;
      }
    }
  }

  public Object deserializeFrom(TCByteBufferInput serialInput) throws IOException {
    boolean literal = serialInput.readBoolean();
    if (literal) {
      return deserializeLiteral(serialInput);
    } else {
      oid = new ObjectID(serialInput.readLong());
    }
    return this;
  }

  public void serializeTo(TCByteBufferOutput serialOutput) {
    if (oid != null) {
      serialOutput.writeBoolean(false);
      serialOutput.writeLong(oid.toLong());
    } else if (LiteralValues.isLiteralInstance(obj)) {
      serialOutput.writeBoolean(true);
      serializeLiteral(serialOutput);
    } else {
      throw new AssertionError("Attempting clustered lock on an unshared non-literal : " + obj);
    }
  }

  private Object deserializeLiteral(TCByteBufferInput serialInput) throws IOException {
    LiteralValues type = LiteralValues.values()[serialInput.readByte()];
    switch (type) {
      case BIG_DECIMAL:
        obj = new BigDecimal(serialInput.readString());
        return this;
      case BIG_INTEGER:
        int length = serialInput.readInt();
        byte[] data = new byte[length];
        serialInput.readFully(data);
        obj = new BigInteger(data);
        return this;
      case INTEGER:
        obj = new Integer(serialInput.readInt());
        return this;
      default:
        throw new AssertionError();
    }
  }

  private void serializeLiteral(TCByteBufferOutput serialOutput) {
    LiteralValues type = LiteralValues.valueFor(obj);
    serialOutput.writeByte(type.ordinal());
    switch (type) {
      case BIG_DECIMAL:
        serialOutput.writeString(((BigDecimal) obj).toString());
        break;
      case BIG_INTEGER:
        byte[] data = ((BigInteger) obj).toByteArray();
        serialOutput.writeInt(data.length);
        serialOutput.write(data);
        break;
      case INTEGER:
        serialOutput.writeInt(((Integer) obj).intValue());
        break;
      default:
        throw new AssertionError();
    }
  }

  public int hashCode() {
    if (oid != null) {
      return oid.hashCode();
    } else {
      return ManagerUtil.calculateDsoHashCode(obj);
    }
  }

  public boolean equals(Object o) {
    if (o == this) {
      return true;
    } else if (o instanceof DsoLockID) {
      if (oid == null) {
        return obj.equals(((DsoLockID) o).obj);
      } else {
        return oid.equals(((DsoLockID) o).oid);
      }
    } else {
      return false;
    }
  }
  
  public String toString() {
    if (oid == null) {
      return "DsoLockID(" + oid + ")";
    } else {
      return "DsoLockID(" + obj + ")";
    }
  }
}
