/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package com.tc.server;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.ProductInfo;

import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;

class UpdateCheckAction extends TimerTask {
  private static final TCLogger consoleLogger         = CustomerLogging.getConsoleLogger();

  private static String         UPDATE_PROPERTIES_URL = "http://www.terracotta.org/kit/reflector?kitID=default&pageID=update.properties";
  private static ProductInfo    productInfo           = ProductInfo.getInstance();

  private TCServer              server;
  private long                  periodMillis;

  UpdateCheckAction(TCServer server, int periodDays) {
    super();
    this.server = server;
    periodMillis = checkPeriodMillis(periodDays);
    silenceHttpClientLoggers();
  }

  public static void start(TCServer server, int periodDays) {
    UpdateCheckAction action = new UpdateCheckAction(server, periodDays);
    new Timer("Update Checker", true).schedule(action, 0, action.getCheckPeriodMillis());
  }
  
  public long getCheckPeriodMillis() {
    return periodMillis;
  }
  
  public static void silenceHttpClientLoggers() {
    Logger.getLogger("org.apache.commons.httpclient.HttpClient").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.params.DefaultHttpParams").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.methods.GetMethod").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.HttpMethodDirector").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.HttpConnection").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.HttpMethodBase").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.HttpState").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.HttpParser").setLevel(Level.OFF);
    Logger.getLogger("org.apache.commons.httpclient.cookie.CookieSpec").setLevel(Level.OFF);
    Logger.getLogger("httpclient.wire.header").setLevel(Level.OFF);
    Logger.getLogger("httpclient.wire.content").setLevel(Level.OFF);
  }

  public URL constructCheckURL() throws MalformedURLException {
    String propsUrl = System.getProperty("terracotta.update-checker.url", UPDATE_PROPERTIES_URL);
    StringBuffer sb = new StringBuffer(propsUrl);

    sb.append(UPDATE_PROPERTIES_URL.equals(propsUrl) ? '&' : '?');

    sb.append("id=");
    sb.append(URLEncoder.encode(Integer.toString(getIpAddress())));
    sb.append("os-name=");
    sb.append(URLEncoder.encode(System.getProperty("os.name")));
    sb.append("&jvm-name=");
    sb.append(URLEncoder.encode(System.getProperty("java.vm.name")));
    sb.append("&jvm-version=");
    sb.append(URLEncoder.encode(System.getProperty("java.vm.version")));
    sb.append("&platform=");
    sb.append(URLEncoder.encode(System.getProperty("os.arch")));
    sb.append("&tc-version=");
    sb.append(URLEncoder.encode(productInfo.rawVersion()));
    sb.append("&tc-product=");
    sb.append(productInfo.edition().equals("opensource") ? "oss" : "ee");
    sb.append("&uptime-secs=");
    sb.append((System.currentTimeMillis() - server.getStartTime())/1000);
    sb.append("&source=server");

    return new URL(sb.toString());
  }

  private static int getIpAddress() {
    try {
      return InetAddress.getLocalHost().hashCode();
    } catch (UnknownHostException uhe) {
      return 0;
    }
  }

  private void showMessage(String msg) {
    consoleLogger.info(msg);
  }

  public Properties getResponseBody(URL url, HttpClient client) throws ConnectException, IOException {
    GetMethod get = new GetMethod(url.toString());

    get.setFollowRedirects(true);
    try {
      int status = client.executeMethod(get);
      if (status != HttpStatus.SC_OK) { throw new ConnectException(
                                                                   "The http client has encountered a status code other than ok for the url: "
                                                                       + url + " status: "
                                                                       + HttpStatus.getStatusText(status)); }
      Properties props = new Properties();
      props.load(get.getResponseBodyAsStream());
      return props;
    } finally {
      get.releaseConnection();
    }
  }

  private void doUpdateCheck() {
    showMessage("Update Checker: Checking...");

    InputStream is = null;
    try {
      StringBuffer sb = new StringBuffer();
      String version = productInfo.rawVersion();
      if (version.indexOf('.') != -1) {
        URL url = constructCheckURL();
        HttpClient httpClient = new HttpClient();
        Properties props = getResponseBody(url, httpClient);

        String propVal = props.getProperty("general.notice");
        if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
          showMessage("Update Checker: "+propVal);
        }

        propVal = props.getProperty(version + ".notice");
        if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
          showMessage("Update Checker: "+propVal);
        }

        propVal = props.getProperty(version + ".updates");
        if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
          StringTokenizer st = new StringTokenizer(propVal, ",");
          while (st.hasMoreElements()) {
            String newVersion = st.nextToken();
            sb.append(newVersion);

            propVal = props.getProperty(newVersion + ".release-notes");
            if (propVal != null && (propVal = propVal.trim()) != null && propVal.length() > 0) {
              sb.append(" [");
              sb.append(propVal);
              sb.append("]");
            }
          }
        }
      }
      if (sb.length() > 0) {
        showMessage("Update Checker: Available updates:");
        showMessage("Update Checker:   * "+sb.toString());
      } else {
        showMessage("Update Checker: No updates found");
      }
    } catch (Exception e) {
      consoleLogger.info("Update Checker: Check failed (" + e.getClass().getName() + ": " + e.getMessage() + ")");
    } finally {
      IOUtils.closeQuietly(is);
    }

    showMessage("Update Checker: Next check at " + new Date(System.currentTimeMillis() + periodMillis));
  }

  public void run() {
    doUpdateCheck();
  }

  private static long checkPeriodMillis(int days) {
    Long minutes = Long.getLong("terracotta.update-checker.next-check-minutes");
    long nextCheckTime;

    if (minutes != null) {
      nextCheckTime = minutes.longValue() * 60 * 1000;
    } else {
      nextCheckTime = days * 24 * 60 * 60 * 1000;
    }

    return nextCheckTime;
  }
}
