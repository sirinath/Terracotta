/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.licenseserver;

import com.tc.io.TCByteBufferInput;
import com.tc.io.TCByteBufferOutput;
import com.tc.net.groups.AbstractGroupMessage;

import java.io.IOException;

public class LicenseStateMessage extends AbstractGroupMessage {
  public static final int  LICENSE_STATE_SYNC_MESSAGE = 0x00;

  public LicenseUsageState licenseUsageState;

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
    //
  }

  @Override
  protected void basicSerializeTo(TCByteBufferOutput out) {
    //
  }

}
