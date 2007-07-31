/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.ITCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.core.TCConnection;

public class NullProtocolAdaptor implements TCProtocolAdaptor {

  public NullProtocolAdaptor() {
    super();
  }

  public void addReadData(TCConnection source, ITCByteBuffer[] data, int length) {
    return;
  }

  public ITCByteBuffer[] getReadBuffers() {
    return TCByteBufferFactory.getFixedSizedInstancesForLength(false, 4096);
  }
}