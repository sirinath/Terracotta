/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.management.remote.connect;

import com.tc.async.api.AbstractEventHandler;
import com.tc.async.api.EventContext;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.management.remote.protocol.ProtocolProvider;
import com.tc.management.remote.protocol.terracotta.ClientProvider;
import com.tc.management.remote.protocol.terracotta.ClientTunnelingEventHandler;
import com.tc.management.remote.protocol.terracotta.JMXConnectStateMachine;
import com.tc.management.remote.protocol.terracotta.L1ConnectionMessage;
import com.tc.management.remote.protocol.terracotta.TunneledDomainsChanged;
import com.tc.net.TCSocketAddress;
import com.tc.net.protocol.tcm.ChannelID;
import com.tc.net.protocol.tcm.MessageChannel;
import com.tc.statistics.StatisticsGateway;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.remote.JMXConnectionNotification;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

public class ClientConnectEventHandler extends AbstractEventHandler {
  private static final TCLogger                 LOGGER         = TCLogging.getLogger(ClientConnectEventHandler.class);

  private final StatisticsGateway               statisticsGateway;

  final ConcurrentMap<ChannelID, ClientBeanBag> clientBeanBags = new ConcurrentHashMap<ChannelID, ClientBeanBag>();

  public ClientConnectEventHandler(final StatisticsGateway statisticsGateway) {
    this.statisticsGateway = statisticsGateway;
  }

  private static final class ConnectorClosedFilter implements NotificationFilter {
    @Override
    public boolean isNotificationEnabled(final Notification notification) {
      boolean enabled = false;
      if (notification instanceof JMXConnectionNotification) {
        final JMXConnectionNotification jmxcn = (JMXConnectionNotification) notification;
        enabled = jmxcn.getType().equals(JMXConnectionNotification.CLOSED);
      }
      return enabled;
    }
  }

  private static final class ConnectorClosedListener implements NotificationListener {
    private final ClientBeanBag bag;

    ConnectorClosedListener(ClientBeanBag bag) {
      this.bag = bag;
    }

    @Override
    final public void handleNotification(final Notification notification, final Object context) {
      bag.unregisterBeans();
    }
  }

  @Override
  public void handleEvent(final EventContext context) {
    if (context instanceof L1ConnectionMessage) {
      L1ConnectionMessage msg = (L1ConnectionMessage) context;
      if (msg.isConnectingMsg()) {
        addJmxConnection(msg);
      } else {
        removeJmxConnection(msg);
      }
    } else if (context instanceof TunneledDomainsChanged) {
      synchronized (clientBeanBags) {
        TunneledDomainsChanged msg = (TunneledDomainsChanged) context;
        ClientBeanBag bag = clientBeanBags.get(msg.getChannelID());
        if (bag != null) {
          try {
            bag.setTunneledDomains(msg.getTunneledDomains());
            bag.updateRegisteredBeans();
          } catch (IOException e) {
            LOGGER
                .error("Unable to create tunneled JMX connection to all the tunneled domains on the DSO client on host["
                           + msg.getChannel().getRemoteAddress()
                           + "], not all the JMX beans on the client will show up in monitoring tools!!", e);
          }
        }
      }
    } else {
      LOGGER.error("Unknown event context : " + context + " (" + context.getClass() + ")");
    }
  }

  private void addJmxConnection(final L1ConnectionMessage msg) {
    LOGGER.info("addJmxConnection(" + msg.getChannel().getChannelID() + ")");

    final MessageChannel channel = msg.getChannel();
    final TCSocketAddress remoteAddress = channel != null ? channel.getRemoteAddress() : null;
    if (remoteAddress == null) {
      LOGGER.error("Not adding JMX connection for " + (channel == null ? "null" : channel.getChannelID())
                   + ". remoteAddress=" + remoteAddress);
      return;
    }

    final MBeanServer l2MBeanServer = msg.getMBeanServer();

    synchronized (clientBeanBags) {
      if (!clientBeanBags.containsKey(channel.getChannelID())) {
        JMXServiceURL serviceURL;
        try {
          serviceURL = new JMXServiceURL("terracotta", remoteAddress.getAddress().getHostAddress(),
                                         remoteAddress.getPort());
        } catch (MalformedURLException murle) {
          LOGGER.error("Unable to construct a JMX service URL using DSO client channel from host["
                           + channel.getRemoteAddress() + "]; tunneled JMX connection will not be established", murle);
          return;
        }
        Map environment = new HashMap();
        ProtocolProvider.addTerracottaJmxProvider(environment);
        environment.put(ClientProvider.JMX_MESSAGE_CHANNEL, channel);
        environment.put("jmx.remote.x.request.timeout", Long.valueOf(Long.MAX_VALUE));
        environment.put("jmx.remote.x.client.connection.check.period", Long.valueOf(0));
        environment.put("jmx.remote.x.server.connection.timeout", Long.valueOf(Long.MAX_VALUE));

        final JMXConnector jmxConnector;
        try {
          jmxConnector = getJmxConnector(serviceURL, environment);

          final MBeanServerConnection l1MBeanServerConnection = jmxConnector.getMBeanServerConnection();
          statisticsGateway.addStatisticsAgent(channel.getChannelID(), l1MBeanServerConnection);
          ClientBeanBag bag = createClientBeanBag(msg, channel, l2MBeanServer, l1MBeanServerConnection);
          clientBeanBags.put(channel.getChannelID(), bag);

          if (bag.updateRegisteredBeans()) {
            try {
              jmxConnector.addConnectionNotificationListener(new ConnectorClosedListener(bag),
                                                             new ConnectorClosedFilter(), null);
            } catch (Exception e) {
              LOGGER.error("Unable to register a JMX connection listener for the DSO client["
                               + channel.getRemoteAddress()
                               + "], if the DSO client disconnects the then its (dead) beans will not be unregistered",
                           e);
            }
          }
        } catch (IOException ioe) {
          LOGGER.info("Unable to create tunneled JMX connection to the DSO client on host["
                       + channel.getRemoteAddress() + "], this DSO client will not show up in monitoring tools!!");
          return;
        }
      } else {
        LOGGER.warn("We are trying to create a new tunneled JMX connection but already have one for channel["
                    + channel.getRemoteAddress() + "], ignoring new connection message");
      }
    }
  }

  protected ClientBeanBag createClientBeanBag(final L1ConnectionMessage msg, final MessageChannel channel,
                                              final MBeanServer l2MBeanServer,
                                              final MBeanServerConnection l1MBeanServerConnection) {
    return new ClientBeanBag(l2MBeanServer, channel, msg.getUUID(), msg.getTunneledDomains(),
                                          l1MBeanServerConnection);
  }

  protected JMXConnector getJmxConnector(JMXServiceURL serviceURL, Map environment) throws IOException {
    return JMXConnectorFactory.connect(serviceURL, environment);
  }

  private void removeJmxConnection(final L1ConnectionMessage msg) {
    final MessageChannel channel = msg.getChannel();

    clientBeanBags.remove(channel.getChannelID());

    JMXConnectStateMachine state = (JMXConnectStateMachine) channel
        .getAttachment(ClientTunnelingEventHandler.STATE_ATTACHMENT);

    if (state.disconnect()) {
      statisticsGateway.removeStatisticsAgent(channel.getChannelID());
    }
  }
}
