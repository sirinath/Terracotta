/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import junit.framework.TestCase;

public class ByteArrayCompressorTest extends TestCase {

  private Compressor compressor;
  
  protected void setUp() throws Exception {
    super.setUp();
    compressor = new ByteArrayCompressor();
  }
  
  public void testSimple() throws Exception {
    final byte[] testData = getTestData();
    BinaryData binaryData = compressor.compress(testData);
    assertTrue(binaryData.isCompressed());
    assertEquals(testData.length, binaryData.getUncompressedLength());
    assertTrue(testData.length > binaryData.getBytes().length);
  }
  
  private byte[] getTestData() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<10000; i++){
      sb.append("foo" + i);
    }
    return sb.toString().getBytes();
  }


}
