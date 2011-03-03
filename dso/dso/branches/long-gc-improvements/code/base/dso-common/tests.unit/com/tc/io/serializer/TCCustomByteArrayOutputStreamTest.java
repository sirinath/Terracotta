/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.io.serializer;

import com.tc.io.serializer.TCCustomByteArrayOutputStream.CustomArray;
import com.tc.test.TCTestCase;

public class TCCustomByteArrayOutputStreamTest extends TCTestCase {

  private byte[] bytes_1 = {9, 8, 7, 6, 5};
  private byte[] bytes_2 = {'a', 'b'};
  
  public void testBasic() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();

    custom.write(bytes_1, 0, bytes_1.length);
    custom.write(1);
    CustomArray result = custom.getCurrentBytes();    
    int result_2 = result.buffer[result.length-1];
    compareArrays(bytes_1, 0, bytes_1.length, result.buffer, result.offset, result.length-1);
    assertEquals(result_2, 1);    
    
    custom.endOfChunk();
    custom.write(bytes_2);
    result = custom.getCurrentBytes();
    custom.endOfChunk();    
    compareArrays(bytes_2, 0, bytes_2.length, result.buffer, result.offset, result.length);
  }
  
  public void testEmpty() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    assertEquals(result.length, 0);
  }
  
  public void testOne() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    custom.write(77);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    assertEquals(result.length, 1);
    assertEquals(result.buffer[result.offset], 77);
  }
  
  public void testBig1() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    byte[] big_buffer = new byte[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE+1];
    fillArray(big_buffer);
    custom.write(big_buffer);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset, result.length);
  }
  
  public void testBigMix() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    byte[] big_buffer = new byte[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE*2];
    fillArray(big_buffer);
    custom.write(bytes_2);
    custom.write(big_buffer);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    assertEquals(big_buffer.length+bytes_2.length, result.length);
    compareArrays(bytes_2, 0, bytes_2.length, result.buffer, result.offset, bytes_2.length);
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset+bytes_2.length, result.length-bytes_2.length);
    
    custom.write(bytes_1);
    result = custom.getCurrentBytes();
    custom.endOfChunk();
    compareArrays(bytes_1, 0, bytes_1.length, result.buffer, result.offset, result.length);
    
    custom.write(big_buffer);
    result = custom.getCurrentBytes();
    custom.endOfChunk();
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset, result.length);
  }  
  
  public void testBigSmall() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    byte[] big_buffer = new byte[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE+10];
    fillArray(big_buffer);    
    custom.write(big_buffer);
    custom.write(bytes_2);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    assertEquals(big_buffer.length+bytes_2.length, result.length);
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset, big_buffer.length);
    compareArrays(bytes_2, 0, bytes_2.length, result.buffer, result.offset+big_buffer.length, result.length-big_buffer.length);
  }  
  
  public void testEdge1() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    byte[] big_buffer = new byte[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE];
    fillArray(big_buffer);    
    custom.write(big_buffer);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset, result.length);    
  }
  
  public void testEdge2() {
    TCCustomByteArrayOutputStream custom = new TCCustomByteArrayOutputStream();
    byte[] big_buffer = new byte[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE-1];
    fillArray(big_buffer);    
    custom.write(big_buffer);
    custom.write(1);
    custom.write(1);
    CustomArray result = custom.getCurrentBytes();
    custom.endOfChunk();
    compareArrays(big_buffer, 0, big_buffer.length, result.buffer, result.offset, big_buffer.length);
    assertEquals(1, result.buffer[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE-1]);
    assertEquals(1, result.buffer[TCCustomByteArrayOutputStream.DEFAULT_BUFFER_SIZE]);
  }
  
  private static void compareArrays(byte[] first, int first_offset, int first_length, byte[] second, int second_offset, int second_length) {
    assertEquals(first_length, second_length);
    for (int i=0; i < first_length; i++) {
      assertEquals(first[first_offset+i], second[second_offset+i]);
    }
  }
  
  private void fillArray(byte[] buffer) {
    for (int i=0; i <  buffer.length; i++) {
      buffer[i] = 'x';
    }
  }
}
