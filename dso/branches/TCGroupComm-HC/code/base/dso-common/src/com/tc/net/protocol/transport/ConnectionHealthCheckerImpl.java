/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentReaderHashMap;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedBoolean;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedInt;
import EDU.oswego.cs.dl.util.concurrent.SynchronizedLong;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.core.TCConnectionManager;
import com.tc.util.Assert;
import com.tc.util.concurrent.SetOnceFlag;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Iterator;

/**
 * Health Checker: This class monitors ESTABLISHED connections by TC Stack's Transport Layer.
 * 
 * @author Manoj
 */
public class ConnectionHealthCheckerImpl implements ConnectionHealthChecker {

  private final TCLogger            logger;
  private Thread                    pingThread;

  private ConcurrentReaderHashMap   connSet                 = new ConcurrentReaderHashMap();
  private final SetOnceFlag         shutdown                = new SetOnceFlag();
  private SetOnceFlag               started                 = new SetOnceFlag();

  /* Extra Checks */
  public final static int           MAX_EXTRACHECK_VALIDITY = 3;
  private final TCConnectionManager connectionManager;

  /* Stats */
  private SynchronizedLong          totalProbeSent          = new SynchronizedLong(0);

  public ConnectionHealthCheckerImpl(HealthCheckerConfig healthCheckerConfig, TCConnectionManager connManager) {
    Assert.assertNotNull(healthCheckerConfig);
    Assert.eval(healthCheckerConfig.isHealthCheckerEnabled());
    logger = TCLogging.getLogger(ConnectionHealthCheckerImpl.class.getName() + ": "
                                 + healthCheckerConfig.getHealthCheckerName());

    pingThread = new Thread(new HealthCheckerPingThread(healthCheckerConfig), "HealthChecker");
    pingThread.setDaemon(true);
    connectionManager = connManager;

    logger.info("HealthChecker - Disabled");

  }

  public void start() {
    if (started.attemptSet()) {
      pingThread.start();
      logger.info("HealthChecker - Started");
    } else {
      logger.warn("HealthChecker already started");
    }
  }

  public void stop() {
    if (shutdown.attemptSet()) {
      logger.info("HealthChecker STOP requested");
    } else {
      logger.info("HealthChecker STOP already requested");
    }
  }

  public boolean isRunning() {
    return started.isSet();
  }

  public void notifyTransportClosed(MessageTransport transport) {
    // HealthChecker Ping Thread can anyway determine this in the next probe interval thru mtb.isConnected and remove it
    // from its radar. still lets do it earlier
    logger.info(transport.getConnectionId().toString() + " CLOSED. Disabling Health Monitoring for the same.");
    connSet.remove(transport);
  }

  public void notifyTransportConnectAttempt(MessageTransport transport) {
    //
  }

  public void notifyTransportConnected(MessageTransport transport) {
    MessageTransportBase mtb = (MessageTransportBase) transport;
    ConnectionHealthCheckerContext context = new ConnectionHealthCheckerContextImpl(mtb);
    mtb.setHealthCheckerContext(context);
  }

  public void notifyTransportDisconnected(MessageTransport transport) {
    //
  }

  class HealthCheckerPingThread implements Runnable {
    private final int     pingIdleTime;
    private final int     pingInterval;
    private final int     pingProbes;
    private final boolean doExtraChecks;
    private final boolean dummy;

    public HealthCheckerPingThread(HealthCheckerConfig healthCheckerConfig) {
      pingIdleTime = healthCheckerConfig.getPingIdleTime() * 1000;
      pingInterval = healthCheckerConfig.getPingInterval() * 1000;
      pingProbes = healthCheckerConfig.getPingProbes();
      doExtraChecks = healthCheckerConfig.doSocketConnect();
      // For testing
      dummy = ((HealthCheckerConfigImpl) healthCheckerConfig).isDummy();

      if ((pingIdleTime - pingInterval < 0) || pingIdleTime <= 0 || pingInterval <= 0 || pingProbes <= 0) {
        logger
            .info("keepalive_interval period should be less than keepalive_idletime And keepalive Ideltime/Interval/Probes cannot be 0 or negative.");
        logger.info("Disabling HealthChecker for this CommsMgr");
        throw new AssertionError("HealthChecker Config Error");
      }
    }

    public void run() {
      while (true) {

        if (shutdown.isSet()) {
          logger.info("HealthChecker SHUTDOWN");
          return;
        }

        Iterator connectionIterator = connSet.keySet().iterator();

        while (connectionIterator.hasNext()) {
          MessageTransportBase mtb = (MessageTransportBase) connectionIterator.next();

          if (!mtb.isConnected()) {
            logger.info(mtb.getConnectionId().toString() + " Closed. Disabling Health Monitoring for the same.");
            connSet.remove(mtb);
            continue;
          }

          ConnectionHealthCheckerContext connContext = (ConnectionHealthCheckerContext) connSet.get(mtb);

          if ((mtb.getConnection().getIdleReceiveTime() >= this.pingIdleTime) || connContext.isDying()) {

            if (connContext.isDead((/* this.keepaliveIdleTime + */(this.pingInterval * this.pingProbes)))) {
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

        ThreadUtil.reallySleep(this.pingInterval);
      }
    }
  }

  // HC Stats - START
  public synchronized int getTotalConnsUnderMonitor() {
    return connSet.size();
  }

  public long getTotalProbesSentOnAllConns() {
    return this.totalProbeSent.get();
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
      totalProbeSent.increment();
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
