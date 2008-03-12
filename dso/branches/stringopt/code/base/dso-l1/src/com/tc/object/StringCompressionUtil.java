/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import com.tc.io.TCByteArrayOutputStream;

import java.util.zip.DeflaterOutputStream;

public class StringCompressionUtil {

  static final char COMPRESSION_FLAG = '\uC0FF'; //get it? "cough"!  illegal in UTF-8
  
  public static char[] decompressCompressedChars(char[] compressedString, int uncompressedLength){
    return decompressString(toByteArray(compressedString, uncompressedLength));
  }
  
  public static char[] toCharArray(byte[] bytes) {
    
    int remainder = bytes.length%2;
    char[] result = new char[bytes.length/2 + remainder + 1];
    result[0] = COMPRESSION_FLAG;
    int charIndex = 1;
    for (int i=0; i< bytes.length; i=i+2){
      int anInt = bytes[i];
      anInt = anInt << 8; //shift first byte up 8 bits, making room for the second byte
      if (i+1 < bytes.length){
        int bitmask = 0x000000FF & bytes[i+1]; //zero out all bits in this bitmask to the left of the 8 bits coming from the byte
        anInt = anInt | bitmask; //now paste those 8 bits into the low bits of "anInt" (whose low 8 bits should be zeroes due to the previous shift)
      }
      //System.out.println("an Int " +anInt + ", hex " + Integer.toHexString(anInt)+ ", binary " + Integer.toBinaryString(anInt));
      result[charIndex++] = (char) anInt; //cast to char (drop all but low 16 bits)
      
    }
    return result;
  }

  public static byte[] toByteArray(char[] chars, int originalLength) {
    byte[] bytes = new byte[originalLength];
    
    int byteIndex = 0;
    for(int i=1; i<chars.length; i++) {
      int anInt = chars[i];
      bytes[byteIndex++] = (byte)(anInt>>8);
      if (byteIndex < bytes.length){
        bytes[byteIndex++] = (byte)(anInt);
      }
    }
    return bytes;
  }

  public static byte[] compressString(String string) {
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
  
  public static char[] decompressString(byte[] compressedString){
    return null;
  }

}
