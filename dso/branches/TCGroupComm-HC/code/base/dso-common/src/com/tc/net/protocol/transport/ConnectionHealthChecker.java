/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.TCSocketAddress;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.Map;

/**
 * Health Checker: This class monitors ESTABLISHED connections by TC Stack's Transport Layer.
 * 
 * @author Manoj
 */
public class ConnectionHealthChecker {

  private final TCLogger                   logger;
  private Map                              connSet           = new ConcurrentHashMap();
  private Iterator                         connHlthChkerCtxtIterator;
  private boolean                          enabled;
  private Thread                           pingThread;
  private ConnectionHealthCheckerSocketMgr hcSocketMgr;
  private boolean                          pingThreadRunning = false;

  private long                             totalHCProbeSent  = 0;

  public ConnectionHealthChecker(HealthCheckerConfig healthCheckerConfig) {
    Assert.assertNotNull(healthCheckerConfig);
    logger = TCLogging.getLogger(ConnectionHealthChecker.class.getName() + ": "
                                 + healthCheckerConfig.getHealthCheckerName());

    if (healthCheckerConfig.isKeepAliveEnabled()) {
      enabled = true;
      pingThread = new Thread(new healthCheckerPingThread(healthCheckerConfig), "HealthChecker");
      pingThread.setDaemon(true);
    } else {
      enabled = false;
      logger.info("Health Checker - Disabled");
    }

    // Listener to detect long GC; XXX shd be configurable - enable/disable
    try {
      hcSocketMgr = new ConnectionHealthCheckerSocketMgr(InetAddress.getLocalHost());
      while (!hcSocketMgr.isRunning()) {
        ThreadUtil.reallySleep(1000);
      }
      logger.info("Health Checker Sock Mgr : " + hcSocketMgr.getBindAddress().getHostAddress() + ":" + hcSocketMgr.getBindPort());
    } catch (UnknownHostException e) {
      //
    }
  }

  public ConnectionHealthCheckerContext checkHealthFor(MessageTransportBase mtb) {
    if (enabled) {
      ConnectionHealthCheckerContext connHCCtxt = new ConnectionHealthCheckerContext(mtb);
      return connHCCtxt;
    } else {
      logger.info("Health monitoring agent for " + mtb.getConnectionId() + " NOT requested");
      return null;
    }
  }

  public synchronized void stop() {
    pingThreadRunning = false;
  }

  public synchronized boolean isRunning() {
    return pingThreadRunning;
  }

  class healthCheckerPingThread implements Runnable {
    private final int keepaliveIdleTime;
    private final int keepaliveInterval;
    private final int keepaliveProbes;

    public healthCheckerPingThread(HealthCheckerConfig healthCheckerConfig) {
      keepaliveIdleTime = healthCheckerConfig.getKeepAliveIdleTime() * 1000;
      keepaliveInterval = healthCheckerConfig.getKeepAiveInterval() * 1000;
      keepaliveProbes = healthCheckerConfig.getKeepAliveProbes();
      if (keepaliveIdleTime - keepaliveInterval <= 0) {
        logger.info("keepalive_interval period should be less than keepalive_idletime");
        logger.info("Disabling HealthChecker for this CommsMgr");
        pingThreadRunning = false;
      } else if (keepaliveIdleTime <= 0 || keepaliveInterval <= 0 || keepaliveProbes <= 0) {
        logger.info("keepalive Ideltime/Interval/Probes cannot be 0 or negative");
        logger.info("Disabling HealthChecker for this CommsMgr");
        pingThreadRunning = false;
      }
    }

    public void run() {
      while (true) {

        if (!pingThreadRunning) {
          logger.info("HealthChecker SHUTDOWN");
          return;
        }

        synchronized (connSet) {
          connHlthChkerCtxtIterator = connSet.keySet().iterator();
        }

        while (connHlthChkerCtxtIterator.hasNext()) {
          MessageTransportBase mtb = (MessageTransportBase) connHlthChkerCtxtIterator.next();

          // if (!mtb.status.isEstablished()) XXX MTB close state is not in sync with Conn
          if (mtb.getConnection().isClosed()) {
            synchronized (connSet) {
              logger.info(mtb.getConnectionId().toString() + " Closed. Disabling Health Monitoring for the same.");
              connSet.remove(mtb);
            }
            continue;
          }

          ConnectionHealthCheckerContext ctxt = (ConnectionHealthCheckerContext) connSet.get(mtb);

          if ((mtb.getConnection().getIdleTime() >= this.keepaliveIdleTime) || ctxt.isLastProbeFailed()) {
            logger.info("Pinging IDLE Connection " + mtb.getConnectionId().toString());
            TransportHandshakeMessage pingMsg;
            pingMsg = mtb.messageFactory.createPing(mtb.getConnectionId(), mtb.getConnection(), ctxt
                .getLocalHClsnrInfo());

            if (ctxt.isDead((this.keepaliveIdleTime + (this.keepaliveInterval * this.keepaliveProbes)))) {
              if (!hcSocketMgr.connectToPeerHCSocketMgr(ctxt.getPeerHClsnrInfo())) {
                mtb.fireTransportDisconnectedEvent();
                logger.fatal(mtb.getConnectionId().toString() + " found DEAD. Disconnecting the Client.");
              } else {
                logger.fatal(mtb.getConnectionId().toString() + " NOT responding for PINGs. Might be in long GC");
              }
              continue;
            }

            mtb.send(pingMsg); // XXX Should this be sent in a separate thread ??
            ctxt.pingSent(System.currentTimeMillis());

          } else {
            ctxt.resetTimers();
          }
        }

        ThreadUtil.reallySleep(this.keepaliveInterval);
      }
    }
  }

  // HC Stats - START
  public synchronized int getTotalConnsUnderMonitor() {
    synchronized (connSet) {
      return connSet.size();
    }
  }

  public synchronized long getTotalProbesSentOnAllConns() {
    return this.totalHCProbeSent;
  }

  // HC Stats - END

  class ConnectionHealthCheckerContext {

    private final MessageTransportBase mtb;
    private final TCSocketAddress      localHClsnrInfo;
    private TCSocketAddress            peerHClsnrInfo     = null;
    private long                       lastPingSent;
    private long                       lastPingReplyRcvd;
    private long                       totalCtxtProbeSent = 0;
    private int                        probeFailCount;

    public ConnectionHealthCheckerContext(MessageTransportBase mtb) {
      this.mtb = mtb;
      this.localHClsnrInfo = new TCSocketAddress(hcSocketMgr.getBindAddress(), hcSocketMgr.getBindPort());
      resetTimers();
    }

    public TCSocketAddress getLocalHClsnrInfo() {
      return localHClsnrInfo;
    }

    private void setPeerHClsnrInfo(TCSocketAddress addr) {
      this.peerHClsnrInfo = addr;
    }

    public TCSocketAddress getPeerHClsnrInfo() {
      return peerHClsnrInfo;
    }

    public void startMonitoring() {

      synchronized (connSet) {
        connSet.put(this.mtb, this);
      }

      if (!pingThreadRunning) {
        pingThreadRunning = true;
        pingThread.start(); // XXX Exception ??
        logger.info("Health Checker - Started");
      }

      logger.info("Health monitoring agent for " + mtb.getConnectionId() + " started");
    }

    public int getProbeCount() {
      return probeFailCount;
    }

    public boolean isLastProbeFailed() {
      if (this.probeFailCount > 0) { return true; }
      return false;
    }

    public void resetTimers() {
      this.probeFailCount = 0;
      this.lastPingReplyRcvd = System.currentTimeMillis();
      this.lastPingSent = System.currentTimeMillis();
    }

    public void incrProbeCount() {
      this.probeFailCount += 1;
    }

    public void decrProbeCount() {
      this.probeFailCount -= 1;
      Assert.eval(this.probeFailCount >= 0);
    }

    public long getLastPingSentTime() {
      return this.lastPingSent;
    }

    public long getLastPingReplyRcvdTime() {
      return this.lastPingReplyRcvd;
    }

    public void pingSent(long sentime) {
      this.lastPingSent = sentime;
      this.totalCtxtProbeSent += 1;
      totalHCProbeSent += 1;
      incrProbeCount();
    }

    public void pingReplyRcvd(long rcvdTime, TCSocketAddress addr) {
      this.lastPingReplyRcvd = rcvdTime;
      decrProbeCount();
      if (addr.getPort() != 0) {
        setPeerHClsnrInfo(addr);
      } else {
        // The other end doesn't have the HC enabled. So, NO HC Sock Lsnr
      }
    }

    public boolean isDead(long deadSleepTime) {
      if ((this.lastPingSent - this.lastPingReplyRcvd) >= (deadSleepTime)) { return true; }
      return false;
    }
  }
}
