/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.net.core.MockTCConnection;
import com.tc.net.core.TCConnection;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerJDK14;
import com.tc.net.core.TCListener;
import com.tc.net.protocol.ProtocolAdaptorFactory;
import com.tc.net.protocol.TCNetworkMessage;
import com.tc.net.protocol.TCProtocolAdaptor;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.NullMessageMonitor;
import com.tc.net.protocol.tcm.msgs.PingMessage;
import com.tc.util.SequenceGenerator;
import com.tc.util.TCTimeoutException;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import junit.framework.TestCase;

public class WireProtocolGroupMessageImplTest extends TestCase {
  private TCConnectionManager connMgr;
  private TCListener          server;
  private final AtomicLong    sentMessages = new AtomicLong(0);
  private final AtomicLong    rcvdMessages = new AtomicLong(0);
  private final AtomicBoolean fullySent    = new AtomicBoolean(false);

  protected void setUp() throws Exception {
    connMgr = new TCConnectionManagerJDK14();

    ProtocolAdaptorFactory factory = new ProtocolAdaptorFactory() {
      public TCProtocolAdaptor getInstance() {
        return new WireProtocolAdaptorImpl(new ServerWPMGSink());
      }
    };

    server = connMgr.createListener(new TCSocketAddress(5678), factory);
  }

  protected void tearDown() throws Exception {
    connMgr.shutdown();
    server.stop();
  }

  Random r = new Random();

  public void testBasic() throws TCTimeoutException, IOException, InterruptedException {
    final TCConnection conn = connMgr.createConnection(new WireProtocolAdaptorImpl(new ClientWPMGSink()));
    conn.connect(new TCSocketAddress(server.getBindPort()), 3000);

    Thread checker = new Thread(new Runnable() {
      public void run() {
        while (!fullySent.get()) {
          ThreadUtil.reallySleep(5000);
          System.out.println("XXX Waiting for all msgs send");
        }

        while (rcvdMessages.get() != sentMessages.get()) {
          ThreadUtil.reallySleep(5000);
          System.out.println("XXX SentMsgs: " + sentMessages + "; RcvdMsgs: " + rcvdMessages);
        }

        System.out.println("XXX SentMsgs: " + sentMessages + "; RcvdMsgs: " + rcvdMessages);
        System.out.println("XXX SuccesS");

      }
    });
    checker.start();

    for (int i = 0; i < 50; i++) {
      r.setSeed(System.currentTimeMillis());
      int count = r.nextInt(100);
      ArrayList<TCNetworkMessage> messages = getMessages(count, conn);
      for (TCNetworkMessage msg : messages)
        conn.putMessage(msg);
      sentMessages.addAndGet(count);
      ThreadUtil.reallySleep(1000);
    }

    fullySent.set(true);
    System.out.println("XXX TOTAL MSGS SENT " + sentMessages);
    checker.join();
  }

  private ArrayList<TCNetworkMessage> getMessages(final int count, final TCConnection conn) {
    MessageMonitor monitor = new NullMessageMonitor();
    SequenceGenerator seq = new SequenceGenerator(1);
    TransportMessageFactoryImpl msgFactory = new TransportMessageFactoryImpl();
    ArrayList<TCNetworkMessage> messages = new ArrayList<TCNetworkMessage>();
    for (int i = 0; i < count; i++) {
      r.setSeed(System.currentTimeMillis());
      int value = r.nextInt(10);
      switch (value) {
        case 0:
        case 1:
          messages.add(msgFactory.createSyn(new ConnectionID(1), new MockTCConnection(), (short) 1, 1));
          break;
        default:
          messages.add(getDSOMessage(monitor, seq));
          break;
      }

    }
    return messages;
  }

  private TCNetworkMessage getDSOMessage(final MessageMonitor monitor, final SequenceGenerator seq) {
    TCNetworkMessage msg = new PingMessage(monitor);
    ((PingMessage) msg).initialize(seq);
    msg.seal();
    return msg;
  }

  class ClientWPMGSink implements WireProtocolMessageSink {
    public void putMessage(WireProtocolMessage message) {
      System.out.println("XXX Client : " + message);
    }

  }

  class ServerWPMGSink implements WireProtocolMessageSink {
    public void putMessage(WireProtocolMessage message) {
      rcvdMessages.incrementAndGet();
      message.recycle();
      if (rcvdMessages.get() % 25 == 0) System.out.println("XXX RCVD MSGS " + rcvdMessages);
    }
  }

}
