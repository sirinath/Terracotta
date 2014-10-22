/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.management.remote.protocol.terracotta;

import com.tc.net.protocol.tcm.MessageChannel;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;

import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorProvider;
import javax.management.remote.JMXServiceURL;
import javax.management.remote.generic.GenericConnector;

public class ClientProvider implements JMXConnectorProvider {

  public static final String JMX_MESSAGE_CHANNEL = "JmxMessageChannel";

  @Override
  public JMXConnector newJMXConnector(final JMXServiceURL jmxserviceurl, final Map initialEnvironment)
      throws IOException {
    if (!jmxserviceurl.getProtocol().equals("terracotta")) {
      MalformedURLException exception = new MalformedURLException("Protocol not terracotta: "
                                                                  + jmxserviceurl.getProtocol());
      throw exception;
    }
    final Map terracottaEnvironment = initialEnvironment != null ? new HashMap(initialEnvironment) : new HashMap();
    final MessageChannel channel = (MessageChannel) terracottaEnvironment.remove(JMX_MESSAGE_CHANNEL);
    final TunnelingMessageConnectionWrapper tmc = new TunnelingMessageConnectionWrapper(channel, false);

    terracottaEnvironment.put(GenericConnector.MESSAGE_CONNECTION, tmc);
    JMXConnector rv = new GenericConnector(terracottaEnvironment);

    JMXConnectStateMachine state = (JMXConnectStateMachine) channel
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    if (!state.connect(channel.getChannelID(), tmc, rv)) {
      tmc.close();
      throw new IOException("JMX connection state transition not accepted for "
                                                         + channel.getChannelID());
    }

    return rv;
  }
}
