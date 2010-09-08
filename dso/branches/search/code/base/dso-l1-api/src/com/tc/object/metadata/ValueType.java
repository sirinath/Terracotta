/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.object.metadata;

import com.tc.io.TCByteBufferInput;
import com.tc.object.metadata.NVPair.BooleanNVPair;
import com.tc.object.metadata.NVPair.ByteArrayNVPair;
import com.tc.object.metadata.NVPair.ByteNVPair;
import com.tc.object.metadata.NVPair.CharNVPair;
import com.tc.object.metadata.NVPair.DateNVPair;
import com.tc.object.metadata.NVPair.DoubleNVPair;
import com.tc.object.metadata.NVPair.FloatNVPair;
import com.tc.object.metadata.NVPair.IntNVPair;
import com.tc.object.metadata.NVPair.LongNVPair;
import com.tc.object.metadata.NVPair.ShortNVPair;
import com.tc.object.metadata.NVPair.StringNVPair;

import java.io.IOException;
import java.util.Date;

public enum ValueType {
  BOOLEAN {
    @Override
    public NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new BooleanNVPair(name, in.readBoolean());
    }
  },
  BYTE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new ByteNVPair(name, in.readByte());
    }
  },
  CHAR {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new CharNVPair(name, in.readChar());
    }
  },
  DOUBLE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new DoubleNVPair(name, in.readDouble());
    }
  },
  FLOAT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new FloatNVPair(name, in.readFloat());
    }
  },
  INT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new IntNVPair(name, in.readInt());
    }
  },
  SHORT {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new ShortNVPair(name, in.readShort());
    }
  },
  LONG {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new LongNVPair(name, in.readLong());
    }
  },
  STRING {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new StringNVPair(name, in.readString());
    }
  },
  DATE {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      return new DateNVPair(name, new Date(in.readLong()));
    }
  },
  BYTE_ARRAY {
    @Override
    NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException {
      int len = in.readInt();
      byte[] data = new byte[len];
      in.read(data, 0, len);
      return new ByteArrayNVPair(name, data);
    }
  };

  static {
    int length = ValueType.values().length;
    if (length > 127) {
      // The encoding logic could support all 256 values in the byte or we could expand to 2 bytes if needed
      throw new AssertionError("Current implementation does not allow for more 127 types");
    }
  }

  NVPair deserializeFrom(TCByteBufferInput in) throws IOException {
    String name = in.readString();
    return deserializeFrom(name, in);
  }

  abstract NVPair deserializeFrom(String name, TCByteBufferInput in) throws IOException;

}
