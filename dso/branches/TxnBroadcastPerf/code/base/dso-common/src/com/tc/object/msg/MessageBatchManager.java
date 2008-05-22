/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.msg;

import com.tc.net.protocol.tcm.MessageChannel;

public interface MessageBatchManager {

  public void sendBatch(DSOMessageBase msg);

  public void flush(MessageChannel channel);
}
