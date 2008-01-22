/*
 * All content copyright (c) 2003-2008 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.net.protocol.transport;

import com.tc.net.TCSocketAddress;
import com.tc.util.Assert;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ConnectionHealthCheckerSocketMgr {
  private ServerSocket      servSocket;
  private final InetAddress localBindAddr;
  private Thread            hcSockMgrThread;
  private boolean           hcSockMgrThreadRunning = false;

  public ConnectionHealthCheckerSocketMgr(InetAddress addr) {
    this.localBindAddr = addr;
    hcSockMgrThread = new Thread(new HCAcceptorThread(), "HC SocketMgr Acceptor Thread");
    hcSockMgrThread.setDaemon(true);
    hcSockMgrThread.start();
  }

  public synchronized void stop() {
    if (hcSockMgrThreadRunning) {
      hcSockMgrThreadRunning = false;
    }
  }

  public boolean connectToPeerHCSocketMgr(TCSocketAddress addr) {
    Socket clntSocket = null;
    if (addr == null || addr.getPort() == 0) return false;
    try {
      clntSocket = new Socket();
      InetSocketAddress inetAddr = new InetSocketAddress(addr.getAddress(), addr.getPort());
      try {
        clntSocket.connect(inetAddr, 5000); // XXX Timeout needed ???
      } catch (SocketTimeoutException ste) {
        return false;
      }
    } catch (IOException ioe) {
      //
    }

    if ((clntSocket != null) && clntSocket.isConnected()) {
      Thread th = new Thread(new ClntSocketCloserThread(clntSocket), "HC SocketMgr Peer connected sckt close thread");
      th.start();
      return true;
    }
    return false;
  }

  class HCAcceptorThread implements Runnable {
    public void run() {
      hcSockMgrThreadRunning = true;
      while (true) {
        try {

          if (!hcSockMgrThreadRunning) { return; }

          servSocket = new ServerSocket(0, 5, localBindAddr);
          Socket clntSocket = null;
          servSocket.setSoTimeout(2000);
          try {
            clntSocket = servSocket.accept();
          } catch (SocketTimeoutException ste) {
            continue;
          }
          if (clntSocket != null) {
            Thread th = new Thread(new ClntSocketCloserThread(clntSocket), "HC SocketMgr clnt sckt close thread");
            th.start();
          }
        } catch (IOException ioe) {
          // 
        }
      }
    }
  }

  class ClntSocketCloserThread implements Runnable {
    Socket clntSoc;

    public ClntSocketCloserThread(Socket clnt) {
      this.clntSoc = clnt;
    }

    public void run() {
      ThreadUtil.reallySleep(2000);
      try {
        clntSoc.close();
      } catch (IOException e) {
        //
      }
      return;
    }
  }

  public InetAddress getBindAddress() {
    Assert.eval(servSocket.getInetAddress() != null);
    return servSocket.getInetAddress();
  }

  public int getBindPort() {
    return servSocket.getLocalPort();
  }

  public synchronized boolean isRunning() {
    return hcSockMgrThreadRunning;
  }

}
