/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnectionManager;
import com.tc.net.core.TCConnectionManagerJDK14;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Iterator;

/**
 * Health Checker: This class monitors ESTABLISHED connections by TC Stack's Transport Layer.
 * 
 * @author Manoj
 */
public class ConnectionHealthChecker implements MessageTransportListener {

  private final TCLogger                   logger;
  private Thread                           pingThread;

  private ConcurrentReaderHashMap          connSet                 = new ConcurrentReaderHashMap();
  private final SetOnceFlag                shutdown                = new SetOnceFlag();
  private SynchronizedBoolean              enabled                 = new SynchronizedBoolean(false);
  private SynchronizedBoolean              pingThreadRunning       = new SynchronizedBoolean(false);

  /* Extra Checks */
  public final static int                  MAX_EXTRACHECK_VALIDITY = 3;
  private final static TCConnectionManager connectionManager       = new TCConnectionManagerJDK14();

  /* Stats */
  private long                             totalHCProbeSent        = 0;

  public ConnectionHealthChecker(HealthCheckerConfig healthCheckerConfig) {
    Assert.assertNotNull(healthCheckerConfig);
    logger = TCLogging.getLogger(ConnectionHealthChecker.class.getName() + ": "
                                 + healthCheckerConfig.getHealthCheckerName());

    if (healthCheckerConfig.isHealthCheckerEnabled()) {
      enabled.set(true);
      pingThread = new Thread(new HealthCheckerPingThread(healthCheckerConfig), "HealthChecker");
      pingThread.setDaemon(true);
    } else {
      enabled.set(false);
    }

    if (!enabled.get()) {
      logger.info("Health Checker - Disabled");
    }

  }

  public synchronized void start() {
    if (enabled.get()) {
      if (!pingThreadRunning.get()) {
        pingThreadRunning.set(true);
        try {
          pingThread.start();
        } catch (Exception e) {
          logger.warn("Ping Thread Exception " + e);
          pingThreadRunning.set(false);
          logger.info("Health Checker - Restarting ...");
          start(); // XXX should there be upper limit on this recursive call??
        }
        logger.info("Health Checker - Started");
      }
    }
  }

  public synchronized void stop() {
    if (enabled.get()) {
      if (shutdown.attemptSet()) {
        pingThreadRunning.set(false);
      } else {
        logger.info("Connection Health Checker STOP already started");
      }
    }
  }

  public synchronized boolean isRunning() {
    return pingThreadRunning.get();
  }

  public synchronized boolean isEnabled() {
    return enabled.get();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // HealthChecker Ping Thread can anyway determine this in the next probe interval thru mtb.isConnected and remove it
    // from its radar. still lets do it earlier
    MessageTransportBase mtb = (MessageTransportBase) transport;
    logger.info(mtb.getConnectionId().toString() + " CLOSED. Disabling Health Monitoring for the same.");
    connSet.remove(mtb);
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
  }

  public void notifyTransportConnected(MessageTransport transport) {
    if (enabled.get()) {
      MessageTransportBase mtb = (MessageTransportBase) transport;
      ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextImpl(mtb);
      mtb.setHealthCheckerContext(context);
    }
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    //
  }

  class HealthCheckerPingThread implements Runnable {
    private final int     keepaliveIdleTime;
    private final int     keepaliveInterval;
    private final int     keepaliveProbes;
    private final boolean doExtraChecks;
    private final boolean dummy;

    public HealthCheckerPingThread(HealthCheckerConfig healthCheckerConfig) {
      keepaliveIdleTime = healthCheckerConfig.getKeepAliveIdleTime() * 1000;
      keepaliveInterval = healthCheckerConfig.getKeepAliveInterval() * 1000;
      keepaliveProbes = healthCheckerConfig.getKeepAliveProbes();
      doExtraChecks = healthCheckerConfig.doExtraChecks();
      // For testing
      dummy = ((HealthCheckerConfigImpl) healthCheckerConfig).isDummy();

      if (keepaliveIdleTime - keepaliveInterval < 0) {
        logger.info("keepalive_interval period should be less than keepalive_idletime");
        logger.info("Disabling HealthChecker for this CommsMgr");
        enabled.set(false);
      } else if (keepaliveIdleTime <= 0 || keepaliveInterval <= 0 || keepaliveProbes <= 0) {
        logger.info("keepalive Ideltime/Interval/Probes cannot be 0 or negative");
        logger.info("Disabling HealthChecker for this CommsMgr");
        enabled.set(false);
      }
    }

    public void run() {
      Iterator connectionIterator;
      while (true) {

        if (!pingThreadRunning.get()) {
          logger.info("HealthChecker SHUTDOWN");
          return;
        }

        connectionIterator = connSet.keySet().iterator();

        while (connectionIterator.hasNext()) {
          MessageTransportBase mtb = (MessageTransportBase) connectionIterator.next();

          if (!mtb.isConnected()) {
            logger.info(mtb.getConnectionId().toString() + " Closed. Disabling Health Monitoring for the same.");
            connSet.remove(mtb);
            continue;
          }

          ConnectionHealthCheckerContext connContext = (ConnectionHealthCheckerContext) connSet.get(mtb);

          if ((mtb.getConnection().getIdleReceiveTime() >= this.keepaliveIdleTime) || connContext.isDying()) {

            if (connContext.isDead((/* this.keepaliveIdleTime + */(this.keepaliveInterval * this.keepaliveProbes)))) {
              if (doExtraChecks) {
                if (connContext.extraCheck()) {
                  if (connContext.getExtraCheckSuccessCount() <= MAX_EXTRACHECK_VALIDITY) {
                    // XXX .. hack .. lets lie the HealthChcker that we have received the PING REPLY
                    connContext.receivePingReply();
                    continue;
                  }
                }
              }

              // Conn is dead. Diconnect the transport.
              mtb.disconnect();
              connSet.remove(mtb);
              continue;
            }

            logger.info("Pinging IDLE Connection " + mtb.getConnectionId().toString());
            if (!dummy) {
              connContext.sendPing();
            } else {
              /* For Testing only */
              connContext.sendDummyPing();
            }

          } else {
            connContext.refresh();
          }
        }

        ThreadUtil.reallySleep(this.keepaliveInterval);
      }
    }
  }

  // HC Stats - START
  public synchronized int getTotalConnsUnderMonitor() {
    return connSet.size();
  }

  public synchronized long getTotalProbesSentOnAllConns() {
    return this.totalHCProbeSent;
  }

  // HC Stats - END

  class ConnectionHealthCheckerContextImpl implements ConnectionHealthCheckerContext {

    private long                                   lastPingSent;
    private long                                   lastPingReplyRcvd;
    private int                                    probeFailCount;
    private final MessageTransportBase             mtb;
    private final HealthCheckerProbeMessageFactory messageFactory;

    // Extra Health Checks
    private HealthCheckerExtra                     extraCheck               = null;
    private Thread                                 extraCheckThread         = null;
    private SynchronizedInt                        extraCheckThreadRunCount = new SynchronizedInt(0);
    private SynchronizedBoolean                    extraCheckThreadRunning  = new SynchronizedBoolean(false);
    private SynchronizedBoolean                    extraCheckResult         = new SynchronizedBoolean(false);
    private int                                    extraCheckSucessCount    = 0;

    public ConnectionHealthCheckerContextImpl(MessageTransportBase mtb) {
      this.mtb = mtb;
      this.messageFactory = new TransportMessageFactoryImpl();
      refresh();

      if (shutdown.isSet()) {
        logger.info("Conection Health Checker is Shutting Down. Request not taken.");
        return;
      }

      connSet.put(this.mtb, this);
      logger.info("Health monitoring agent for " + mtb.getConnectionId() + " started");
    }

    public void sendDummyPing() {
      HealthCheckerProbeMessage pingMessage = this.messageFactory.createDummyPing(mtb.getConnectionId(), mtb
          .getConnection());
      this.mtb.send(pingMessage);
      this.pingSent(System.currentTimeMillis());
    }

    public void receiveDummyPing() {
      // shhh. keep quite
    }

    public void sendPing() {
      HealthCheckerProbeMessage pingMessage = this.messageFactory
          .createPing(mtb.getConnectionId(), mtb.getConnection());
      this.mtb.send(pingMessage);
      this.pingSent(System.currentTimeMillis());
    }

    private void pingSent(long sentime) {
      this.lastPingSent = sentime;
      totalHCProbeSent += 1;
      incrProbeCount();
    }

    public void receivePing() {
      sendPingReply();
      // XXX may note down the time in future
    }

    public void sendPingReply() {
      HealthCheckerProbeMessage pingReplyMessage = this.messageFactory.createPingReply(mtb.getConnectionId(), mtb
          .getConnection());
      this.mtb.send(pingReplyMessage);
      // XXX may note down the time in future
    }

    public void receivePingReply() {
      this.lastPingReplyRcvd = System.currentTimeMillis();
      decrProbeCount();
    }

    public boolean isDying() {
      if (this.probeFailCount > 0) { return true; }
      return false;
    }

    public void refresh() {
      this.probeFailCount = 0;
      this.lastPingReplyRcvd = System.currentTimeMillis();
      this.lastPingSent = System.currentTimeMillis();
      this.extraCheckSucessCount = 0;
      this.extraCheckThreadRunCount.set(0);
      this.extraCheckResult.set(false);
    }

    private void incrProbeCount() {
      this.probeFailCount += 1;
    }

    private void decrProbeCount() {
      this.probeFailCount -= 1;
      Assert.eval(this.probeFailCount >= 0);
    }

    public boolean isDead(long deadSleepTime) {
      // System.out.println("XXX chking dead .. DST=" + deadSleepTime + " LPS=" + this.lastPingSent + " LPRR="
      // + this.lastPingReplyRcvd + " DIFF=" + (this.lastPingSent - this.lastPingReplyRcvd));
      if ((this.lastPingSent - this.lastPingReplyRcvd) >= (deadSleepTime)) { return true; }
      return false;
    }

    public boolean extraCheck() {
      if (extraCheck == null) {
        extraCheck = new HealthCheckerExtra(mtb.getRemoteAddress(), connectionManager);
      }

      if (!extraCheckThreadRunning.get()) {
        extraCheckThread = new Thread(new Runnable() {

          public void run() {
            extraCheckThreadRunning.set(true);
            boolean rv = extraCheck.detect(); // May consume time
            extraCheckResult.set(rv);
            extraCheckThreadRunCount.increment();
            extraCheckThreadRunning.set(false);
          }
        }, "HealthChecker ExtraDetectThread");

        extraCheckThread.setDaemon(true);
        extraCheckThread.start();

      }

      if (extraCheckThreadRunCount.get() <= 0) {
        // We are not sure about the Extra check result as none of the threads have returned
        // Its better to say the peer is alive than to say its dead
        // Though ExtraChecks continuously return TRUE, there is a max count beyond which Healthchecker Ping Thread will
        // NOT respect this value
        extraCheckSucessCount++;
        return true;
      } else {
        if (extraCheckResult.get()) extraCheckSucessCount++;
        return extraCheckResult.get();
      }
    }

    public int getExtraCheckSuccessCount() {
      return extraCheckSucessCount;
    }
  }

}
