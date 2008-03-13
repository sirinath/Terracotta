/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCDataInput;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.object.bytecode.JavaLangString;
import com.tc.object.bytecode.ManagerUtil;
import com.tc.object.dna.impl.DNAEncodingImpl;
import com.tc.object.loaders.ClassProvider;
import com.tc.util.Assert;

import java.io.IOException;
import java.lang.reflect.Constructor;

public class ApplicatorDNAEncodingImpl extends DNAEncodingImpl {

  private static final Constructor COMPRESSED_STRING_CONSTRUCTOR;
  private static final TCLogger      logger                               = TCLogging.getLogger(ApplicatorDNAEncodingImpl.class);

  static { 
    try {
      COMPRESSED_STRING_CONSTRUCTOR = String.class.getDeclaredConstructor(new Class[] { Boolean.TYPE, char[].class, Integer.TYPE, Integer.TYPE });
    } catch(Exception e) {
      // should never happen if run with instrumented boot jar
      throw Assert.failure(e.getMessage(), e);
    }
  }
  
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
    
    // read uncompressed byte[] length, but don't actually need it for this use case
    input.readInt();
    
    byte[] data = readByteArray(input);
    
    int stringLength = input.readInt();
    int stringHash = input.readInt();
    
    try {
      // Pack byte[] into char[] (still compressed)
      char[] compressedChars = StringCompressionUtil.toCharArray(data);
      
      // Construct new string with the compressed char[] in it
      String s = (String) COMPRESSED_STRING_CONSTRUCTOR.newInstance(new Object[] { Boolean.TRUE, compressedChars, new Integer(stringLength), new Integer(stringHash) });

      if (STRING_COMPRESSION_LOGGING_ENABLED) {
        logger.info("Read compressed String of compressed size : " + compressedChars.length + ", uncompressed size : " + stringLength
                    + ", hash code : " + stringHash);
      }      
      
      if (isInterned == DNAEncodingImpl.STRING_TYPE_INTERNED) {
        if (STRING_COMPRESSION_LOGGING_ENABLED) {
          logger.info("Decompressing and interning string.");
        }      
        decompress(s);      
        return ManagerUtil.intern(s);
      } else {
        return s;
      }
    } catch (Exception e) {
      throw Assert.failure(e.getMessage(), e);
    }
  }

  private void decompress(Object string) {
    if (string instanceof JavaLangString) {
      ((JavaLangString) string).__tc_decompress();
    }
  }
}
