/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.util.Assert;

import java.util.IdentityHashMap;
import java.util.Map;

public class CompressedStringManagerImpl implements CompressedStringManager {

  // TODO - switch to IdentityWeakHashMap
  private final Map compressedStrings = new IdentityHashMap();
  
  public void addCompressedString(String string, byte[] data, int uncompressedLength) {
    synchronized(compressedStrings) {
      compressedStrings.put(string, new CompressedStringData(data, uncompressedLength));
    }
  }

  public char[] decompressString(String string) {
System.out.println("\nDecompressing...");    
    synchronized(compressedStrings) {
      CompressedStringData data = (CompressedStringData) compressedStrings.get(string);
      Assert.assertNotNull(data);

      // TODO
      
      compressedStrings.remove(string);

      // TODO
      return null;
    }
  }
  
  
  /**
   * Just a wrapper for the byte[] and uncompressed byte[] length for 
   * string data.
   */
  private static class CompressedStringData {
    private byte[] data;
    private int uncompressedLength;
    
    CompressedStringData(byte[] data, int uncompressedLength) {
      this.data = data;
      this.uncompressedLength = uncompressedLength;
    }

    public byte[] getData() {
      return data;
    }

    public int getUncompressedLength() {
      return uncompressedLength;
    }
    
  }
}

