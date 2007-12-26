/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import EDU.oswego.cs.dl.util.concurrent.ConcurrentHashMap;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.properties.TCPropertiesImpl;
import com.tc.util.concurrent.ThreadUtil;

import java.util.Iterator;
import java.util.Map;

/**
 * Health Checker: This class monitors ESTABLISHED connections by TC Stack's Transport Layer.
 * 
 * @author Manoj
 */
public class ConnectionHealthChecker {

  private static final int                     HC_KEEPALIVE_XTIME = 1000 * TCPropertiesImpl.getProperties()
                                                                      .getInt("tcgroupcomm.hc.keepalive.idletime");
  private static final int                     HC_KEEPALIVE_INTVL = 1000 * TCPropertiesImpl.getProperties()
                                                                      .getInt("tcgroupcomm.hc.keepalive.interval");
  private static final int                     HC_KEEPALIVE_PROBE = TCPropertiesImpl.getProperties()
                                                                      .getInt("tcgroupcomm.hc.keepalive.probes");
  private static Map                           connSet            = new ConcurrentHashMap();
  private static Iterator                      connHlthChkerCtxtIterator;
  private final ConnectionHealthCheckerContext connHlthChkerCtxt;
  private final MessageTransportBase           msgTxBase;
  private static final TCLogger                logger             = TCLogging.getLogger(ConnectionHealthChecker.class);

  private static final boolean                 isHCEnabled        = TCPropertiesImpl.getProperties()
                                                                      .getBoolean("tcgroupcomm.hc.enabled");
  static {
    if (isHCEnabled) {
      final Thread th = new Thread(new healthCheckerPingThread(), "HealthChecker");
      th.setDaemon(true);
      th.start();
      logger.info("Health Checker - Started");
    } else {
      logger.info("Health Checker - Disabled");
    }
  }

  static class healthCheckerPingThread implements Runnable {
    public void run() {

      while (true) {

        ThreadUtil.reallySleep(HC_KEEPALIVE_INTVL);
        connHlthChkerCtxtIterator = connSet.keySet().iterator();

        while (connHlthChkerCtxtIterator.hasNext()) {
          MessageTransportBase mtb = (MessageTransportBase) connHlthChkerCtxtIterator.next();

          // if (!mtb.status.isEstablished()) { XXX MTB close state is not in sync with Conn
          if (mtb.getConnection().isClosed()) {
            synchronized (connSet) {
              connSet.remove(mtb);
            }
            continue;
          }

          ConnectionHealthCheckerContext ctxt = (ConnectionHealthCheckerContext) connSet.get(mtb);

          if (mtb.getConnection().getIdleTime() > HC_KEEPALIVE_XTIME || ctxt.isLastProbeFailed()) {
            TransportHandshakeMessage pingMsg;
            pingMsg = mtb.messageFactory.createPing(mtb.getConnectionId(), mtb.getConnection());

            if (ctxt.isDead()) {
              mtb.fireTransportDisconnectedEvent();
              logger.fatal(mtb.getConnectionId().toString() + " found DEAD. Disconnecting the Client.");
            }

            mtb.send(pingMsg);
            ctxt.incrProbeCount();
            ctxt.pingSent(System.currentTimeMillis());
            logger.info("Probe " + ctxt.getProbeCount() + " sent to idle Connection "
                        + mtb.getConnectionId().toString());

          } else {
            ctxt.resetTimers();
          }
        }
      }
    }
  }

  public ConnectionHealthChecker(MessageTransportBase mtb) {
      this.connHlthChkerCtxt = new ConnectionHealthCheckerContext();
      this.msgTxBase = mtb;
  }

  public void pingSent(long sendTime) {
    this.connHlthChkerCtxt.pingSent(sendTime);
  }

  public void pingReplyRcvd(long rcvdTime) {
    this.connHlthChkerCtxt.pingReplyRcvd(rcvdTime);
    this.connHlthChkerCtxt.resetTimers();
  }

  public void activate() {
    if (isHCEnabled) {
      synchronized (connSet) {
        connSet.put(this.msgTxBase, this.connHlthChkerCtxt);
      }
    }
  }

  class ConnectionHealthCheckerContext {

    private long lastPingSent;
    private long lastPingReplyRcvd;
    private int  probeFailCount;

    public ConnectionHealthCheckerContext() {
      // XXX chk HC enabled ??
      resetTimers();
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

    public long getLastPingSentTime() {
      return this.lastPingSent;
    }

    public long getLastPingReplyRcvdTime() {
      return this.lastPingReplyRcvd;
    }

    public void pingSent(long sentime) {
      this.lastPingSent = sentime;
    }

    public void pingReplyRcvd(long rcvdTime) {
      this.lastPingReplyRcvd = rcvdTime;
    }

    public boolean isDead() {
      if ((this.lastPingSent - this.lastPingReplyRcvd) >= (HC_KEEPALIVE_INTVL * HC_KEEPALIVE_PROBE)) { return true; }
      return false;
    }
  }

}
