/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteArrayOutputStream;

import java.util.zip.DeflaterOutputStream;

import junit.framework.TestCase;

public class ByteToCharTest extends TestCase {

  private ByteToChar byteToChar;
  
  protected void setUp() throws Exception {
    super.setUp();
    byteToChar = new ByteToChar();
  }

  public void testWithCompression() throws Exception {
    String test = getTestString();
    byte[] compressed = compressString(test);
    char[] chars = byteToChar.toCharArray(compressed);
    String compressedString = new String(chars);
    assertTrue(compressedString.length() < test.length());
  }
  
  public void testConvertTwoBytesToExpectedChar() throws Exception {
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) -1, '\uFFFF');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0x7F, '\uFF7F');
    helpTestConvertTwoBytesToExpectedChar((byte)1,(byte) 1, '\u0101');
    helpTestConvertTwoBytesToExpectedChar((byte)0,(byte) 0, '\u0000');
    helpTestConvertTwoBytesToExpectedChar((byte)-1,(byte) 0, '\uFF00');
    helpTestConvertTwoBytesToExpectedChar((byte)0x80,(byte) 0x80, '\u8080');
  }
  
  private void helpTestConvertTwoBytesToExpectedChar(byte firstByte, byte secondByte, char expected){
    char[] result = byteToChar.toCharArray(new byte[]{firstByte, secondByte});
    assertEquals(1, result.length);
    assertEquals((int)expected, (int) result[0]);
    assertEquals(expected, result[0]);
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
  
  private void helpTestRoundtrip(byte[] bytes) {
    char[] c = byteToChar.toCharArray(bytes);
    byte[] b = byteToChar.toByteArray(c, bytes.length);
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

  private byte[] compressString(String string) {
    try {      
      TCByteArrayOutputStream byteArrayOS = new TCByteArrayOutputStream(4096);
      // Stride is 512 bytes by default, should I increase ?
      DeflaterOutputStream dos = new DeflaterOutputStream(byteArrayOS);
      byte[] uncompressed = string.getBytes("UTF-8");
      dos.write(uncompressed);
      dos.close();
      byte[] compressed = byteArrayOS.getInternalArray();
      return compressed;
    } catch (Exception e) {
      throw new AssertionError(e);
    }
  }
}
