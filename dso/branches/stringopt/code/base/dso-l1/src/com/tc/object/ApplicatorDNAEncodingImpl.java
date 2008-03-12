/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.object.compression.CompressedStringManager;
import com.tc.object.dna.impl.DNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class ApplicatorDNAEncodingImpl extends DNAEncodingImpl {

  private final CompressedStringManager compressedStringManager;

  /**
   * Used in the Applicators. The policy is set to APPLICATOR.
   */
  public ApplicatorDNAEncodingImpl(ClassProvider classProvider, CompressedStringManager compressedStringManager) {
    super(APPLICATOR, classProvider);
    this.compressedStringManager = compressedStringManager;
  }

  protected boolean useStringEnumRead(byte type) {
    return true;
  }

  protected boolean useClassProvider(byte type) {
    return true;
  }

  protected boolean useUTF8String(byte type) {
    return true;
  }

  protected Object readCompressedString(TCDataInput input) throws IOException {
    byte isInterned = input.readByte();
    int stringUncompressedByteLength = input.readInt();
    byte[] data = readByteArray(input);

    int stringLength = input.readInt();
    int stringHash = input.readInt();

    try {
      Constructor c = String.class.getDeclaredConstructor(new Class[] { Integer.TYPE, Integer.TYPE });
      String s = (String) c.newInstance(new Object[] { new Integer(stringLength), new Integer(stringHash) });
      this.compressedStringManager.addCompressedString(s, data, stringUncompressedByteLength);
      if (isInterned == DNAEncodingImpl.STRING_TYPE_INTERNED) {
        return s.intern();
      } else {
        return s;
      }
    } catch (Exception e) {
      throw Assert.failure(e.getMessage(), e);
    }
  }

}
