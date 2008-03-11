/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object;

public class ByteToChar {

  public char[] convert(byte[] bytes) {
    
    int remainder = bytes.length%2;
    char[] result = new char[bytes.length/2 + remainder];
    for (int i=0; i< bytes.length; i=i+2){
      short aShort = bytes[i];
      char aChar = (char) (aShort << 4);
      if (i+1 < bytes.length){
        aChar += bytes[i+1];
      }
      result[i/2] = aChar;
    }
    return result;
  }

}
