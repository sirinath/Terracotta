/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.object.dna.impl.DNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class ApplicatorDNAEncodingImpl extends DNAEncodingImpl {

  /**
   * Used in the Applicators. The policy is set to APPLICATOR.
   */
  public ApplicatorDNAEncodingImpl(ClassProvider classProvider) {
    super(APPLICATOR, classProvider);
  }

  protected boolean useStringEnumRead(byte type) {
    return true;
  }

  protected boolean useClassProvider(byte type, byte typeToCheck) {
    return true;
  }

  protected boolean useUTF8String(byte type) {
    return true;
  }

  protected Object readCompressedString(TCDataInput input) throws IOException {
    byte isInterned = input.readByte();
    byte[] data = readByteArray(input);

    int stringLength = input.readInt();
    int stringHash = input.readInt();

    try {
      // Pack byte[] into char[] (still compressed)
      char[] compressedChars = StringCompressionUtil.toCharArray(data);
      
      // Construct new string with the compressed char[] in it
      Constructor c = String.class.getDeclaredConstructor(new Class[] { Boolean.TYPE, Class.forName("[C"), Integer.TYPE, Integer.TYPE });
      String s = (String) c.newInstance(new Object[] { Boolean.TRUE, compressedChars, new Integer(stringLength), new Integer(stringHash) });
      
      if (isInterned == DNAEncodingImpl.STRING_TYPE_INTERNED) {
        //force decompress then intern
        s.getChars(0, 1, new char[1], 0);
        return s.intern();
      } else {
        return s;
      }
    } catch (Exception e) {
      throw Assert.failure(e.getMessage(), e);
    }
  }

}
