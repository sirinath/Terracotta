/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.core.TCConnection;
import com.tc.net.core.TestTCConnection;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.test.TCTestCase;
import com.tc.util.Assert;
import com.tc.util.SequenceGenerator;

import java.util.ArrayList;

public class WireProtocolGroupMessageImplTest extends TCTestCase {

  public void testBasic() {
    TCConnection conn = new TestTCConnection();
    WireProtocolGroupMessage wpmg = WireProtocolGroupMessageImpl.wrapMessages(getMessages(10, conn), conn);
    Assert.assertEquals(10, wpmg.getTotalMessageCount());

  }

  private ArrayList<TCNetworkMessage> getMessages(final int count, final TCConnection conn) {
    ArrayList<TCNetworkMessage> messages = new ArrayList<TCNetworkMessage>();
    MessageMonitor monitor = new NullMessageMonitor();
    SequenceGenerator seq = new SequenceGenerator(1);
    for (int i = 0; i < count; i++) {
      TCNetworkMessage msg = new PingMessage(monitor);
      ((PingMessage) msg).initialize(seq);
      msg.seal();
      messages.add(msg);
    }
    return messages;
  }
}
