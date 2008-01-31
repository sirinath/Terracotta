/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

public interface ConnectionHealthCheckerContext {

  /* Is the Transport IDLE for maxIdleTime ( = KEEPALIVE_IDELTIME + ( KEEPALIVE_INTERVAL * KEEPALIVE_PROBECOUNT) ) */
  public boolean isDead(long maxIdleTime);

  /* Has the transport received a PING_REPLY for the last PING probe sent */
  public boolean isDying();

  /* Transport is lively */
  public void refresh();

  /* Probe Message send and receive */
  public void sendPing();

  public void receivePing();

  public void sendDummyPing();

  public void receiveDummyPing();

  public void sendPingReply();

  public void receivePingReply();

  /* Do Extra Health Checks other than above Probes */
  public boolean extraCheck();

  /* Get # of successful extra checks done */
  public int getExtraCheckSuccessCount();
}
