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
  
  public void addCompressedString(String string, byte[] data) {
    synchronized(compressedStrings) {
      compressedStrings.put(string, data);
    }
  }

  public char[] decompressString(String string) {
System.out.println("\nDecompressing...");    
    synchronized(compressedStrings) {
      byte[] data = (byte[]) compressedStrings.get(string);
      Assert.assertNotNull(data);

    // TODO
      compressedStrings.remove(string);

return null;
    }
  }
}

