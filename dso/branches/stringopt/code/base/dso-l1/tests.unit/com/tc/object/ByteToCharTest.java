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

  public void testSimple() throws Exception {
    
    String test = getTestString();
    byte[] uncompressed = test.getBytes();
    byte[] compressed = compressString(test);
//    System.out.println("uncompressed length " + uncompressed.length + ", hashCode " + uncompressed.hashCode());
//    System.out.println("compressed length " + compressed.length + ", hashCode " + compressed.hashCode());
    
    char[] chars = byteToChar.convert(compressed);
    
    String compressedString = new String(chars);
//    System.out.println("<!><!><!><!> "+compressedString + ", chars length " + chars.length);
//    System.out.println("uncompressed length " + test.length() + ", hashCode " + test.hashCode());
//    System.out.println("compressed length " + compressedString.length() + ", hashCode " + compressedString.hashCode());
    assertTrue(compressedString.length() < test.length());
  }
  
  public void testRoundTrip() throws Exception {
    helpAssert((byte)0xF, (byte)0xF, (char)0xFF);
    helpAssert((byte)0xD, (byte)0xF, (char)0xDF);
  }
  
  private void helpAssert(byte firstByte, byte secondByte, char expected){
    char[] result = byteToChar.convert(new byte[]{firstByte, secondByte});
    assertEquals(1, result.length);
    assertEquals(expected, result[0]);
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
