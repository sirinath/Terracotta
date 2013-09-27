/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import org.junit.Assert;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.tc.io.TCByteBufferOutputStream;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.net.protocol.tcm.MessageMonitor;
import com.tc.net.protocol.tcm.TCMessage;
import com.tc.net.protocol.tcm.TCMessageType;
import com.tc.object.session.SessionID;
import com.tc.test.TCTestCase;
import com.tc.util.concurrent.ThreadUtil;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;

import javax.management.remote.message.Message;

public class TunnelingMessageConnectionWrapperTest extends TCTestCase {
  public void testClose() throws Exception {
    ThreadMXBean bean = ManagementFactory.getThreadMXBean();
    int initialThreads = bean.getThreadCount();
    MessageChannel mockChanel = Mockito.mock(MessageChannel.class);
    final CountDownLatch messageSendLatch = new CountDownLatch(1);
    Mockito.when(mockChanel.createMessage((TCMessageType) Matchers.any())).thenAnswer(new Answer<TCMessage>() {

      @Override
      public TCMessage answer(InvocationOnMock invocation) throws Throwable {
        return new JmxRemoteTunnelMessage(new SessionID(0), Mockito.mock(MessageMonitor.class),
            new TCByteBufferOutputStream(), null, TCMessageType.JMXREMOTE_MESSAGE_CONNECTION_MESSAGE) {
          @Override
          public void send() {
            messageSendLatch.countDown();
          }
        };
      }
    }
    );
    TunnelingMessageConnection connection = new TunnelingMessageConnectionWrapper(mockChanel,
                                                                                  true);
    connection.writeMessage(Mockito.mock(Message.class));
    // wait until the message is send..
    messageSendLatch.await();
    connection.close();
    ThreadUtil.reallySleep(2000);
    int finalThreads = bean.getThreadCount();
    if (finalThreads > initialThreads) {
      bean.dumpAllThreads(bean.isObjectMonitorUsageSupported(), bean.isSynchronizerUsageSupported());
      Assert.fail();
    }
  }


}