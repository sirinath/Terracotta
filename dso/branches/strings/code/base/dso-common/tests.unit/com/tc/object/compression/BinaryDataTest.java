/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.object.compression;

import com.tc.exception.ImplementMe;
import com.tc.io.TCDataInput;
import com.tc.io.TCDataOutput;

import java.io.IOException;
import java.util.Arrays;

import junit.framework.TestCase;

public class BinaryDataTest extends TestCase {

  private byte[] bytes;
  private MockTCDataOutput mockOutput;
  private MockTCDataInput mockInput;
  private static final int TEST_LENGTH = 200;
  
  protected void setUp() throws Exception {
    super.setUp();
    bytes = getTestData();
    mockOutput = new MockTCDataOutput();
    mockInput = new MockTCDataInput(bytes);
  }
  
  public void testUncompressedData() throws Exception {
    BinaryData data = new BinaryData(bytes);
    assertSame(bytes, data.getBytes());
    assertEquals(BinaryData.DEFAULT_UNCOMPRESSED_LENGTH, data.getUncompressedLength());
    assertFalse(data.isCompressed());
  }
  
  public void testCompressedData() throws Exception {
    final int uncompressedLength = TEST_LENGTH;
    BinaryData data = new BinaryData(bytes, uncompressedLength);
    assertSame(bytes, data.getBytes());
    assertEquals(uncompressedLength, data.getUncompressedLength());
    assertTrue(data.isCompressed());
  }

  public void testReadUncompressedFromTCDataInput() throws Exception {
    BinaryData data = BinaryData.readUncompressed(mockInput);
    assertTrue(Arrays.equals(this.bytes, data.getBytes()));
    assertFalse(data.isCompressed());
    assertEquals(1, mockInput.readIntCalled);
  }
  
  public void testReadCompressedFromTCDataInput() throws Exception {
    BinaryData data = BinaryData.readCompressed(mockInput);
    assertTrue(Arrays.equals(this.bytes, data.getBytes()));
    assertTrue(data.isCompressed());
    assertEquals(this.bytes.length, data.getUncompressedLength());
    assertEquals(2, mockInput.readIntCalled);
  }
  
  public void testWriteCompressedToTCDataOutput() throws Exception {
    final int uncompressedLength = TEST_LENGTH;
    BinaryData data = new BinaryData(bytes, uncompressedLength);
    data.writeTo(mockOutput);
    assertSame(bytes, mockOutput.data);
    assertEquals(bytes.length, mockOutput.compressedLength);
    assertEquals(bytes.length, mockOutput.length);
    assertEquals(uncompressedLength, mockOutput.uncompressedLength);
    assertEquals(0, mockOutput.offset);
  }
  
  public void testWriteUncompressedToTCDataOutput() throws Exception {
    BinaryData data = new BinaryData(bytes);
    data.writeTo(mockOutput);
    assertSame(bytes, mockOutput.data);
    assertEquals(bytes.length, mockOutput.uncompressedLength);
    assertEquals(bytes.length, mockOutput.length);
    assertEquals(0, mockOutput.offset);
  }
  
  public void testIsEqual() throws Exception {
    assertEquals(new BinaryData("foo".getBytes()), new BinaryData("foo".getBytes()));
    assertEquals(new BinaryData("foo".getBytes(), 222), new BinaryData("foo".getBytes(), 222));
  }
  
  public void testIsNotEqual() throws Exception {
    assertFalse(new BinaryData("foo".getBytes()).equals(new BinaryData("bar".getBytes())));
    assertFalse(new BinaryData("foo".getBytes(), 222).equals(new BinaryData("foo".getBytes(), 333)));
  }
  
  public void testNotEqualsNull() throws Exception {
    assertFalse(new BinaryData("foo".getBytes()).equals(null));
  }
  
  public void testNotEqualsSomeOtherObject() throws Exception {
    assertFalse(new BinaryData("foo".getBytes()).equals(new Object()));
  }
  
  public void testHashCodeSame() throws Exception {
    assertEquals(new BinaryData("foo".getBytes()).hashCode(), new BinaryData("foo".getBytes()).hashCode());
  }
  
  public void testHashCodeDifferent() throws Exception {
    assertFalse(new BinaryData("foo".getBytes()).hashCode() == new BinaryData("bar".getBytes()).hashCode());
  }
  
  // PRIVATE
  
  private byte[] getTestData() {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<10000; i++){
      sb.append("foo" + i);
    }
    return sb.toString().getBytes();
  }  
  
  private class MockTCDataInput implements TCDataInput{

    private final byte[] bytes;
    private int readIntCalled = 0;
    
    public MockTCDataInput(byte[] bytes) {
      this.bytes = bytes;
    }

    public void readFully(byte[] b) throws IOException {
      System.arraycopy(bytes, 0, b, 0, bytes.length);
    }
    
    public int readInt() throws IOException {
      readIntCalled++;
      return bytes.length;
    }

    public int read(byte[] b, int off, int len) throws IOException {
      throw new ImplementMe();
    }

    public String readString() throws IOException {
      throw new ImplementMe();
    }

    public boolean readBoolean() throws IOException {
      throw new ImplementMe();
    }

    public byte readByte() throws IOException {
      throw new ImplementMe();
    }

    public char readChar() throws IOException {
      throw new ImplementMe();
    }

    public double readDouble() throws IOException {
      throw new ImplementMe();
    }

    public float readFloat() throws IOException {
      throw new ImplementMe();
    }

    public void readFully(byte[] b, int off, int len) throws IOException {
      throw new ImplementMe();
    }

    public String readLine() throws IOException {
      throw new ImplementMe();
    }

    public long readLong() throws IOException {
      throw new ImplementMe();
    }

    public short readShort() throws IOException {
      throw new ImplementMe();
    }

    public String readUTF() throws IOException {
      throw new ImplementMe();
    }

    public int readUnsignedByte() throws IOException {
      throw new ImplementMe();
    }

    public int readUnsignedShort() throws IOException {
      throw new ImplementMe();
    }

    public int skipBytes(int n) throws IOException {
      throw new ImplementMe();
    }
    
  }
  
  private class MockTCDataOutput implements TCDataOutput{
    private static final int UNDEFINED = -100;
    private int uncompressedLength = UNDEFINED;
    private int compressedLength = UNDEFINED;
    private byte[] data;
    private int offset;
    private int length;
    
    public void write(byte[] value, int offset, int length) {
      this.data = value;
      this.offset = offset;
      this.length = length;
    }
    
    public void writeInt(int value) {
      if (uncompressedLength == UNDEFINED){
        uncompressedLength = value;
      } else {
        compressedLength = value;
      }
    }
    
    public void close() {
      throw new ImplementMe();
    }

    public void write(int value) {
      throw new ImplementMe();
    }

    public void write(byte[] value) {
      throw new ImplementMe();

    }

    public void writeBoolean(boolean value) {
      throw new ImplementMe();

    }

    public void writeByte(int value) {
      throw new ImplementMe();

    }

    public void writeChar(int value) {
      throw new ImplementMe();

    }

    public void writeDouble(double value) {
      throw new ImplementMe();

    }

    public void writeFloat(float value) {
      throw new ImplementMe();

    }

    public void writeLong(long value) {
      throw new ImplementMe();

    }

    public void writeShort(int value) {
      throw new ImplementMe();

    }

    public void writeString(String string) {
      throw new ImplementMe();

    }    
    
  }  
  
}
