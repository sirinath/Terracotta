/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import junit.framework.TestCase;

public class StringDecompressorTest extends TestCase {

  private StringDecompressor stringDecompressor;
  private MockDecompressor mockDecompressor;
  private boolean decompressCalled;
  private String testString;
  
  protected void setUp() throws Exception {
    super.setUp();
    testString = "foo";
    decompressCalled = false;
    mockDecompressor = new MockDecompressor();
    stringDecompressor = new StringDecompressor(StringDecompressor.DEFAULT_ENCODING, mockDecompressor);
  }

  public void testDecompress() throws Exception {
    final byte[] bytes = testString.getBytes();
    BinaryData data = new BinaryData(bytes);
    String actual = (String)stringDecompressor.decompress(data);
    assertEquals(testString, actual);
    assertTrue(decompressCalled);
  }
  
  private class MockDecompressor implements Decompressor{

    public Object decompress(BinaryData compressedData) {
      decompressCalled = true;
      return testString.getBytes();
    }
    
  }
  
}
