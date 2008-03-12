/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;



import java.util.Arrays;

import junit.framework.TestCase;

public class StringCompressionUtilTest extends TestCase {

  public void testConvertTwoBytesToExpectedChar() throws Exception {
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) -1, '\uFFFF');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0x7F, '\uFF7F');
    helpTestConvertTwoBytesToExpectedChar((byte)1,(byte) 1, '\u0101');
    helpTestConvertTwoBytesToExpectedChar((byte)0,(byte) 0, '\u0000');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0, '\uFF00');
    helpTestConvertTwoBytesToExpectedChar((byte)0x80,(byte) 0x80, '\u8080');
  }
  
  private void helpTestConvertTwoBytesToExpectedChar(byte firstByte, byte secondByte, char expected){
    char[] result = StringCompressionUtil.toCharArray(new byte[]{firstByte, secondByte});
    assertEquals(2, result.length);
    assertEquals(expected, result[1]);
  }
  
  public void testRoundtripOddNumberOfBytes() throws Exception {
    helpTestRoundtrip(new byte[]{-1,-1,-1});
  }
  
  public void testRoundtrip() {
    helpTestRoundtrip(new byte[] {} );
    for(byte a=Byte.MIN_VALUE; ; a++) {
      for(byte b=Byte.MIN_VALUE; ; b++) {
        byte[] test = new byte[] { a, b};
        //System.out.println("Testing { " + (int)a + ", " + (int)b + " }");
        helpTestRoundtrip(test);
        if (b==Byte.MAX_VALUE){
          break;
        }
      }
      if (a==Byte.MAX_VALUE){
        break;
      }
    }
  }
  
  public void testDoNotDecompressAlreadyDecompressedString() throws Exception {
    String test = "foo";
    char[] testChars = new char[test.length()];
    test.getChars(0, test.length(), testChars, 0);
    assertNull(StringCompressionUtil.decompressCompressedChars(testChars, test.length()));
  }
  
  public void testDoNotDecompressEmptyString() throws Exception {
    String test = "";
    char[] testChars = new char[test.length()];
    test.getChars(0, test.length(), testChars, 0);
    assertNull(StringCompressionUtil.decompressCompressedChars(testChars, test.length()));
  }  
  
  public void testDecompressCompressedString() throws Exception {
    String test = getTestString();
    byte[] testBytes = test.getBytes("UTF-8");
    char[] testChars = new char[test.length()];
    test.getChars(0, test.length(), testChars, 0);    
    
    char[] compressedChars = StringCompressionUtil.compressToChars(testBytes);
    
    assertTrue(compressedChars.length < testChars.length);
    
    byte[] uncompressed = StringCompressionUtil.decompressCompressedChars(compressedChars, test.length());
    assertTrue(Arrays.equals(testBytes, uncompressed));
  }
  
  private void helpTestRoundtrip(byte[] bytes) {
    char[] c = StringCompressionUtil.toCharArray(bytes);
    byte[] b = StringCompressionUtil.toByteArray(c, bytes.length);
    assertEquals(bytes.length, b.length);
    for(int i=0; i<bytes.length; i++) {
      assertEquals("Mismatch in index " + i, bytes[i], b[i]);
    }
  }  
  
  private String getTestString() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<9999; i++){
      sb.append("foo");
      sb.append(i);
    }
    return sb.toString();
  }
}
