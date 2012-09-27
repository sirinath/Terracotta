/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.licenseserver;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

public class LicenseStateMessage extends AbstractGroupMessage {
  public static final int  LICENSE_STATE_SYNC_MESSAGE = 0x00;

  private LicenseUsageState licenseUsageState;

  // To make serialization happy
  public LicenseStateMessage() {
    super(-1);
  }

  public LicenseStateMessage(LicenseUsageState state) {
    super(LICENSE_STATE_SYNC_MESSAGE);
    this.licenseUsageState = state;
  }

  @Override
  protected void basicDeserializeFrom(TCByteBufferInput in) throws IOException {
    ObjectInputStream objectInputStream = new ObjectInputStream(new TCInputStreamAdapter(in));
    try {
      this.licenseUsageState = (LicenseUsageState) objectInputStream.readObject();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    try {
      final ObjectOutputStream writer = new ObjectOutputStream(new TCOutputStreamAdapter(out));
      writer.writeObject(licenseUsageState);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public LicenseUsageState getLicenseUsageState() {
    return licenseUsageState;
  }

  private static class TCOutputStreamAdapter extends OutputStream {

    private final TCByteBufferOutput out;

    public TCOutputStreamAdapter(TCByteBufferOutput out) {
      this.out = out;
    }

    @Override
    public void write(int b) {
      out.writeInt(b);
    }

  }
  
  private static class TCInputStreamAdapter extends InputStream {
    private final TCByteBufferInput in;

    public TCInputStreamAdapter(TCByteBufferInput in) {
      this.in = in;
    }

    @Override
    public int read() throws IOException {
      return in.readInt();
    }

  }

}
