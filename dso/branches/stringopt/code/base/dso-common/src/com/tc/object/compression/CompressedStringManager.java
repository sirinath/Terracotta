/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

public interface CompressedStringManager {

  void addCompressedString(String string, byte[] compressedData, int uncompressedLength);

  char[] decompressString(String string);

}
