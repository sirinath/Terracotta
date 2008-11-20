/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.object.handshakemanager;

import com.tc.async.impl.NullSink;
import com.tc.cluster.Cluster;
import com.tc.logging.TCLogging;
import com.tc.net.GroupID;
import com.tc.net.NodeID;
import com.tc.net.protocol.tcm.TestChannelIDProvider;
import com.tc.object.ClientIDProvider;
import com.tc.object.ClientIDProviderImpl;
import com.tc.object.msg.ClientHandshakeMessage;
import com.tc.object.msg.ClientHandshakeMessageFactory;
import com.tc.object.msg.TestClientHandshakeMessage;
import com.tc.object.session.NullSessionManager;
import com.tc.properties.TCPropertiesConsts;
import com.tc.properties.TCPropertiesImpl;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.NoExceptionLinkedQueue;
import com.tc.util.concurrent.ThreadUtil;
import com.tc.util.sequence.BatchSequence;
import com.tc.util.sequence.BatchSequenceProvider;
import com.tc.util.sequence.BatchSequenceReceiver;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ClientHandshakeManagerTest extends TCTestCase {
  private static final String               clientVersion = "x.y.z";
  private ClientIDProvider                  cip;
  private ClientHandshakeManagerImpl        mgr;
  private TestClientHandshakeMessageFactory chmf;
  private TestClientHandshakeCallback       callback;

  public void setUp() throws Exception {
    cip = new ClientIDProviderImpl(new TestChannelIDProvider());
    chmf = new TestClientHandshakeMessageFactory();
    callback = new TestClientHandshakeCallback();
    mgr = new ClientHandshakeManagerImpl(TCLogging.getLogger(ClientHandshakeManagerImpl.class), cip, chmf,
                                         new NullSink(), new NullSessionManager(),
                                         new BatchSequence(new TestSequenceProvider(), 100), new Cluster(),
                                         clientVersion, Collections.singletonList(callback));
    newMessage();
  }

  private void newMessage() {
    chmf.message = new TestClientHandshakeMessage();
  }

  public void tests() {

    assertEquals(1, callback.paused.get());
    assertEquals(0, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());

    final AtomicBoolean done = new AtomicBoolean(false);
    new Thread(new Runnable() {
      public void run() {
        mgr.waitForHandshake();
        done.set(true);
      }
    }).start();

    ThreadUtil.reallySleep(2000);
    assertFalse(done.get());

    mgr.connected(GroupID.ALL_GROUPS);

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(0, callback.unpaused.get());

    assertFalse(done.get());

    TestClientHandshakeMessage sentMessage = (TestClientHandshakeMessage) chmf.newMessageQueue.take();
    assertTrue(chmf.newMessageQueue.isEmpty());

    // make sure that the manager called send on the message...
    sentMessage.sendCalls.take();
    assertTrue(sentMessage.sendCalls.isEmpty());

    // make sure RuntimeException is thrown if client/server versions don't match and version checking is enabled
    try {
      mgr.acknowledgeHandshake(cip.getClientID(), false, "1", new String[] {}, clientVersion + "a.b.c", sentMessage
          .getChannel());
      if (checkVersionMatchEnabled()) {
        fail();
      }
    } catch (RuntimeException e) {
      if (!checkVersionMatchEnabled()) {
        fail();
      }
    }

    // now ACK for real
    mgr.acknowledgeHandshake(cip.getClientID(), false, "1", new String[] {}, clientVersion, sentMessage.getChannel());

    assertEquals(1, callback.paused.get());
    assertEquals(1, callback.initiateHandshake.get());
    assertEquals(1, callback.unpaused.get());

    while (!done.get()) {
      // Will fail with a timeout
      ThreadUtil.reallySleep(1000);
    }
  }

  private boolean checkVersionMatchEnabled() {
    return TCPropertiesImpl.getProperties().getBoolean(TCPropertiesConsts.L1_CONNECT_VERSION_MATCH_CHECK);
  }

  private static class TestClientHandshakeMessageFactory implements ClientHandshakeMessageFactory {

    public TestClientHandshakeMessage   message;
    public final NoExceptionLinkedQueue newMessageQueue = new NoExceptionLinkedQueue();

    public ClientHandshakeMessage newClientHandshakeMessage() {
      newMessageQueue.put(message);
      return message;
    }

  }

  public class TestSequenceProvider implements BatchSequenceProvider {

    long sequence = 1;

    public synchronized void requestBatch(BatchSequenceReceiver receiver, int size) {
      receiver.setNextBatch(sequence, sequence + size);
      sequence += size;
    }

  }

  private static final class TestClientHandshakeCallback implements ClientHandshakeCallback {

    AtomicInteger paused            = new AtomicInteger();
    AtomicInteger unpaused          = new AtomicInteger();
    AtomicInteger initiateHandshake = new AtomicInteger();

    public void initializeHandshake(NodeID thisNode, NodeID remoteNode, ClientHandshakeMessage handshakeMessage) {
      initiateHandshake.incrementAndGet();
    }

    public void pause(NodeID remoteNode) {
      paused.incrementAndGet();
    }

    public void unpause(NodeID remoteNode) {
      unpaused.incrementAndGet();
    }

  }

}
