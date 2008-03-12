/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

public class ByteToChar {

  public char[] toCharArray(byte[] bytes) {
    
    int remainder = bytes.length%2;
    char[] result = new char[bytes.length/2 + remainder];
    for (int i=0; i< bytes.length; i=i+2){
      int anInt = bytes[i];
      anInt = anInt << 8; //shift first byte up 8 bits, making room for the second byte
      if (i+1 < bytes.length){
        int bitmask = 0x000000FF & bytes[i+1]; //zero out all bits in this bitmask to the left of the 8 bits coming from the byte
        anInt = anInt | bitmask; //now paste those 8 bits into the low bits of "anInt" (whose low 8 bits should be zeroes due to the previous shift)
      }
      //System.out.println("an Int " +anInt + ", hex " + Integer.toHexString(anInt)+ ", binary " + Integer.toBinaryString(anInt));
      result[i/2] = (char) anInt; //cast to char (drop all but low 16 bits)
      
    }
    return result;
  }

  public byte[] toByteArray(char[] chars, int originalLength) {
    byte[] bytes = new byte[originalLength];
    
    int byteIndex = 0;
    for(int i=0; i<chars.length; i++) {
      int anInt = chars[i];
      bytes[byteIndex++] = (byte)(anInt>>8);
      if (byteIndex < bytes.length){
        bytes[byteIndex++] = (byte)(anInt);
      }
    }
    return bytes;
  }

}
