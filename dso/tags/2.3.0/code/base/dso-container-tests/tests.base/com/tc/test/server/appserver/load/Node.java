/*
 * All content copyright (c) 2003-2006 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.test.server.appserver.load;

import org.apache.commons.httpclient.Cookie;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpState;
import org.apache.commons.httpclient.MultiThreadedHttpConnectionManager;

import EDU.oswego.cs.dl.util.concurrent.SynchronizedRef;

import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.test.server.util.HttpUtil;
import com.tc.util.concurrent.ThreadUtil;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import junit.framework.Assert;

public class Node implements Runnable {
  protected static final TCLogger logger = TCLogging.getLogger(Node.class);
  protected final HttpClient      client;
  protected final HttpState[]     sessions;
  protected final long            duration;
  protected final int             numRequests[];
  protected final SynchronizedRef error  = new SynchronizedRef(null);
  protected final URL[]           mutateUrls;
  protected final URL[]           validateUrls;
  protected final Random          random = new Random();

  public Node(URL mutateUrl, URL validateUrl, int numSessions, long duration) {
    this(new URL[] { mutateUrl }, new URL[] { validateUrl }, numSessions, duration);
  }

  public Node(URL[] mutateUrls, URL[] validateUrls, int numSessions, long duration) {
    MultiThreadedHttpConnectionManager connMgr = new MultiThreadedHttpConnectionManager();
    connMgr.getParams().setConnectionTimeout(120 * 1000);
    this.client = new HttpClient(connMgr);

    this.mutateUrls = mutateUrls;
    this.validateUrls = validateUrls;
    this.sessions = createStates(numSessions);
    this.duration = duration;
    this.numRequests = new int[sessions.length];
  }

  public void checkError() throws Throwable {
    Throwable t = (Throwable) error.get();
    if (t != null) { throw t; }
  }

  public void run() {
    try {
      makeRequests();
      validate();
    } catch (Throwable t) {
      logger.error(t);
      error.set(t);
    }
  }

  private void validate() throws ConnectException, IOException {
    for (int i = 0; i < sessions.length; i++) {
      int expect = numRequests[i];
      if (expect == 0) { throw new AssertionError("No requests were ever made for client " + i); }
      HttpState httpState = sessions[i];
      client.setState(httpState);

      for (int u = 0; u < validateUrls.length; u++) {
        int actual = HttpUtil.getInt(validateUrls[u], client);
        Assert.assertEquals(getSessionID(httpState), expect, actual);
        logger.info("validated value of " + expect + " for client " + i + " on " + validateUrls[u]);
        // Recording the request that was just made. This is needed for RequestCountTest.
        numRequests[i]++;
      }
    }
  }

  private void makeRequests() throws Exception {
    final int numURLS = mutateUrls.length;

    int session = 0;
    final long end = System.currentTimeMillis() + duration;
    while (System.currentTimeMillis() <= end) {
      HttpState httpState = sessions[session];
      client.setState(httpState);
      URL mutateUrl = numURLS == 1 ? mutateUrls[0] : mutateUrls[random.nextInt(mutateUrls.length)];

      final long start = System.currentTimeMillis();
      try {
        int newVal = HttpUtil.getInt(mutateUrl, client);
        numRequests[session]++;
        Assert.assertEquals(getSessionID(httpState), numRequests[session], newVal);
        session = (session + 1) % sessions.length;
        ThreadUtil.reallySleep(random.nextInt(5) + 1);
      } catch (Exception e) {
        logger.error("Elapsed time for failed request was " + (System.currentTimeMillis() - start) + " millis, url = "
                     + mutateUrl);
        throw e;
      }
    }
  }

  private String getSessionID(HttpState httpState) {
    List sessionCookies = new ArrayList();
    Cookie[] cookies = httpState.getCookies();
    for (int i = 0; i < cookies.length; i++) {
      Cookie cookie = cookies[i];
      if (cookie.getName().toLowerCase().indexOf("jsessionid") >= 0) {
        sessionCookies.add(cookie.getName() + "=" + cookie.getValue() + " at path " + cookie.getPath());
      }
    }

    if (sessionCookies.isEmpty()) { return "no session cookie yet"; }
    return sessionCookies.toString();
  }

  private static HttpState[] createStates(int numSessions) {
    HttpState[] rv = new HttpState[numSessions];
    for (int i = 0; i < numSessions; i++) {
      rv[i] = new HttpState();
    }
    return rv;
  }
}
