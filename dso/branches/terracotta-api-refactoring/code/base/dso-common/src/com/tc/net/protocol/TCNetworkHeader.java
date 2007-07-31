/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright notice.  All rights reserved.
 */
package com.tc.net.protocol;

import com.tc.bytes.ITCByteBuffer;
import com.tc.lang.Recyclable;

/**
 * Generic network header interface
 * 
 * @author teck
 */
public interface TCNetworkHeader extends Recyclable {
  int getHeaderByteLength();

  ITCByteBuffer getDataBuffer();

  void validate() throws TCProtocolException;

}