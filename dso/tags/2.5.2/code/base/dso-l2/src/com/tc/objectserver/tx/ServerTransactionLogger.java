/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.objectserver.tx;

import com.tc.logging.TCLogger;
import com.tc.net.groups.NodeID;
import com.tc.object.tx.ServerTransactionID;

import java.util.Collection;
import java.util.Set;

public class ServerTransactionLogger implements ServerTransactionListener {

  private final TCLogger                       logger;
  private final ServerTransactionManagerConfig config;

  private long                                 outStandingTxns = 0;
  private long                                 last            = 0;

  public ServerTransactionLogger(TCLogger logger, ServerTransactionManagerConfig config) {
    this.logger = logger;
    this.config = config;
  }

  public void addResentServerTransactionIDs(Collection stxIDs) {
    logger.info("addResentTransactions: " + stxIDs);
  }

  public void clearAllTransactionsFor(NodeID deadNode) {
    logger.info("clearAllTransactionsFor: " + deadNode);
  }

  public void transactionManagerStarted(Set cids) {
    logger.info("trasactionManagerStarted: " + cids);
  }

  public void incomingTransactions(NodeID source, Set serverTxnIDs) {
    if (config.isVerboseLogging()) logger.info("incomingTransactions: " + source + ", " + serverTxnIDs);
    incrementOutStandingTxns(serverTxnIDs.size());
  }

  private synchronized void incrementOutStandingTxns(int count) {
    outStandingTxns += count;
    if (needToLogStats()) {
      logStats();
    }
  }

  private synchronized void decrementOutStandingTxns(int count) {
    outStandingTxns -= count;
    if (needToLogStats()) {
      logStats();
    }
  }

  private boolean needToLogStats() {
    if (!config.isPrintStatsEnabled()) return false;
    long now = System.currentTimeMillis();
    boolean log = (now - last) > 1000;
    if (log) {
      last = now;
    }
    return log;
  }

  private void logStats() {
    logger.info("Number of pending transactions in the System : " + outStandingTxns);
  }

  public void transactionApplied(ServerTransactionID stxID) {
    if (config.isVerboseLogging()) logger.info("transactionApplied: " + stxID);
  }

  public void transactionCompleted(ServerTransactionID stxID) {
    if (config.isVerboseLogging()) logger.info("transactionCompleted: " + stxID);
    decrementOutStandingTxns(1);
  }

}
