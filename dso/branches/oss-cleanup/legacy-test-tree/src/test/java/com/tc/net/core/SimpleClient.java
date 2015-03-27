/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.tc.net.core;


import com.tc.bytes.TCByteBuffer;
import com.tc.bytes.TCByteBufferFactory;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.GenericNetworkMessage;
import com.tc.net.protocol.GenericNetworkMessageSink;
import com.tc.net.protocol.GenericProtocolAdaptor;

import java.util.concurrent.atomic.AtomicLong;

public class SimpleClient {
  private final int                 numMsgs;
  private final TCConnectionManager connMgr;
  private final TCSocketAddress     addr;
  private final int                 dataSize;
  private final AtomicLong          msgs = new AtomicLong(0);
  private final long                sleepFor;

  public SimpleClient(TCConnectionManager connMgr, TCSocketAddress addr, int numMsgs, int dataSize, long sleepFor) {
    this.connMgr = connMgr;
    this.addr = addr;
    this.numMsgs = numMsgs;
    this.dataSize = dataSize;
    this.sleepFor = sleepFor;
  }

  public void run() throws Exception {
    final GenericNetworkMessageSink recvSink = new GenericNetworkMessageSink() {
      @Override
      public void putMessage(GenericNetworkMessage msg) {
        final long recv = msgs.incrementAndGet();
        if ((recv % 1000) == 0) {
          System.out.println("Processed " + (recv * msg.getTotalLength()) + " bytes...");
        }
      }
    };

    final TCConnection conn = connMgr.createConnection(new GenericProtocolAdaptor(recvSink));
    conn.connect(addr, 3000);

    for (int i = 0; (numMsgs < 0) || (i < numMsgs); i++) {
      TCByteBuffer data[] = TCByteBufferFactory.getFixedSizedInstancesForLength(false, dataSize);
      final GenericNetworkMessage msg = new GenericNetworkMessage(conn, data);
      msg.setSentCallback(new Runnable() {
        @Override
        public void run() {
          msg.setSent();
        }
      });

      conn.putMessage(msg);

      if (sleepFor < 0) {
        msg.waitUntilSent();
      } else {
        Thread.sleep(sleepFor);
      }
    }

    Thread.sleep(5000);
    conn.close(3000);
  }

  public static void main(String args[]) throws Throwable {
    try {
      TCConnectionManager connMgr = new TCConnectionManagerImpl();
      SimpleClient client = new SimpleClient(connMgr, new TCSocketAddress(args[0], Integer.parseInt(args[1])), Integer
          .parseInt(args[3]), Integer.parseInt(args[2]), Integer.parseInt(args[4]));
      client.run();
    } catch (Throwable t) {
      System.err.println("usage: " + SimpleClient.class.getName()
                         + " <host> <port> <msgSize> <numMsgs, -1 for unlimited> <delay, -1 for single fire>\n\n");
      throw t;
    }
  }
}