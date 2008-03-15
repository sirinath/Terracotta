/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.InflaterInputStream;

/**
 * Utilities to compress/decompress a UTF-8 String
 */
public class StringCompressionUtil {

  static final char COMPRESSION_FLAG = '\uC0FF'; //get it? "cough"!  illegal in UTF-8
  
  public static byte[] decompressCompressedChars(char[] compressedString, int uncompressedLength){
    if (isCompressed(compressedString)){
      return decompressString(toByteArray(compressedString, uncompressedLength), uncompressedLength);
    }
    return null;
  }
  
  public static boolean isCompressed(char[] compressedString) {
    //Give it the "cough" test, heh heh
    return (compressedString.length > 0 && COMPRESSION_FLAG == compressedString[0]);
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
    //skip the first char since it is just the compression flag
    for(int i=1; i<chars.length; i++) {
      int anInt = chars[i];
      bytes[byteIndex++] = (byte)(anInt>>8);
      if (byteIndex < bytes.length){
        bytes[byteIndex++] = (byte)(anInt);
      }
    }
    return bytes;
  }

  public static byte[] decompressString(byte[] compressedString, int uncompressedLength){
    try {
      ByteArrayInputStream bais = new ByteArrayInputStream(compressedString);
      InflaterInputStream iis = new InflaterInputStream(bais);
      byte uncompressed[] = new byte[uncompressedLength];
      int read;
      int offset = 0;
      while (uncompressedLength > 0 && (read = iis.read(uncompressed, offset, uncompressedLength)) != -1) {
        offset += read;
        uncompressedLength -= read;
      }
      iis.close();
      return uncompressed;
    } catch (IOException e) {
      throw new AssertionError(e);
    }
  }

}
