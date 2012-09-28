/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

import static org.terracotta.license.LicenseConstants.CAPABILITY_AUTHENTICATION;
import static org.terracotta.license.LicenseConstants.CAPABILITY_OPERATOR_CONSOLE;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SECURITY;
import static org.terracotta.license.LicenseConstants.CAPABILITY_SERVER_STRIPING;
import static org.terracotta.license.LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP;
import static org.terracotta.license.LicenseConstants.LICENSE_CAPABILITIES;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.SerializationUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.mortbay.util.ajax.JSON;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.EnterpriseLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class RestLicenseHelper {

  private static final long       LOG_EXPIRED_LICENSE_PERIOD = 1000L * 60 * 60;
  private static final TCLogger   CONSOLE_LOGGER             = CustomerLogging.getConsoleLogger();
  private static final Logger     LOGGER                     = LoggerFactory.getLogger(RestLicenseHelper.class);
  private static String           URL_PREFIX                 = getLicenseServerUrl();
  private static final String     HTTP                       = "http://";
  public static final String      LICENSE_SERVER_URL_KEY     = "licenseServerUrl";
  private static final String[]   licenseServers             = getLicenseServerUrls();

  private static final String     JVM_ID                     = JvmIDUtil.getJvmID();
  private static final String     JVM_NAME                   = JvmIDUtil.getMachineName();

  private static volatile boolean vmRegistered               = false;

  private static Timer            expiredTimer;
  private static Timer            leaseTimer;
  private static final String     DELIMITER                  = "|";
  private static final File       stateDumpFile              = getStateDumpFile();

  private static File getStateDumpFile() {
    String filePath = System.getProperty("user.home") + File.separator + "terracotta" + File.separator + JVM_NAME
                      + ".dump";
    return new File(filePath);
  }

  private static String[] getLicenseServerUrls() {
    String allUrls = System.getProperty(LICENSE_SERVER_URL_KEY, "localhost:8080");
    return allUrls.split(",");
  }

  public static boolean verifyCapability(String capability) {
    assertLicenseValid();
    if (!getLicense().isCapabilityEnabled(capability)) { throw new LicenseException(
                                                                                    "Your license key doesn't allow usage of '"
                                                                                        + capability + "' capability"); }
    return false;
  }

  private static String getLicenseServerUrl() {
    return System.getProperty(LICENSE_SERVER_URL_KEY, "http://localhost:8080/");
  }

  public static License assertLicenseValid() {
    License license = getLicense();
    Date expirationDate = license.expirationDate();
    if (expirationDate != null && expirationDate.before(new Date())) { throw new LicenseException(
                                                                                                  "Your Terracotta license has expired on "
                                                                                                      + expirationDate); }
    return license;
  }

  public static License getLicense() {
    Map<String, Serializable> keyValuePairs = new HashMap<String, Serializable>();
    Map<String, String> responseMap = executeQuery(LicenseServerConstants.GET_LICENSE_PATH, keyValuePairs);
    String licenseString = responseMap.get(LicenseServerConstants.LICENSE);
    AbstractLicenseResolverFactory factory = new EnterpriseLicenseResolverFactory();
    return factory.resolveLicense(new ByteArrayInputStream(licenseString.getBytes()));
  }

  public static void reallySleep(long millis) {
    boolean interrupted = false;
    try {
      long millisLeft = millis;
      while (millisLeft > 0) {
        long start = System.currentTimeMillis();
        try {
          Thread.sleep(millisLeft);
        } catch (InterruptedException e) {
          interrupted = true;
        }
        millisLeft -= System.currentTimeMillis() - start;
      }
    } finally {
      if (interrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  private static Map<String, String> executeQuery(String servletPath, Map<String, Serializable> keyValuePairs) {
    HttpResponse response = null;
    HttpGet httpGet = null;
    HttpClient httpClient = new DefaultHttpClient();
    HttpParams params = new BasicHttpParams();
    for (Entry<String, Serializable> entry : keyValuePairs.entrySet()) {
      params.setParameter(entry.getKey(), entry.getValue());
    }
    boolean executed = false;
    int serverIndex = 0;
    do {
      String currentLicenseServerUrl = HTTP + licenseServers[serverIndex++] + "/";
      try {
        httpGet = new HttpGet(currentLicenseServerUrl + servletPath);
        httpGet.setParams(params);
        response = httpClient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        executed = true;
        StringBuilder builder = new StringBuilder();
        if (entity != null) {
          InputStream instream = null;
          BufferedReader reader = null;
          try {
            instream = entity.getContent();
            reader = new BufferedReader(new InputStreamReader(instream));
            String str = reader.readLine();
            while (str != null) {
              builder.append(str);
              str = reader.readLine();
            }
          } finally {
            instream.close();
            reader.close();
          }
        }
        HashMap<String, String> jsonMap = (HashMap<String, String>) JSON.parse(getStringForm(entity));
        String status = jsonMap.get(LicenseServerConstants.RESPONSE_CODE);
        if (status.equals(LicenseServerConstants.FAILURE_CODE)) {
          String failure_message = jsonMap.get(LicenseServerConstants.FAILURE_MESSAGE);
          throw new LicenseException(failure_message);
        }
        return jsonMap;
      } catch (ClientProtocolException e) {
        LOGGER.warn(httpGet.getURI() + " got ClientProtocolException Sleeping for 1 sec before retry " + e);
        reallySleep(TimeUnit.SECONDS.toMillis(1L));
      } catch (IOException e) {
        LOGGER.warn(httpGet.getURI() + " got IOException Sleeping for 1 sec before retry " + e);
        reallySleep(TimeUnit.SECONDS.toMillis(1L));
      } finally {
        httpClient.getConnectionManager().shutdown();
      }
      serverIndex = serverIndex % licenseServers.length;
    } while (!executed);

    return null;
  }

  private static String getStringForm(HttpEntity entity) throws IllegalStateException, IOException {
    StringBuilder builder = new StringBuilder();
    if (entity != null) {
      InputStream instream = entity.getContent();
      BufferedReader reader = new BufferedReader(new InputStreamReader(instream));
      String str = reader.readLine();
      while (str != null) {
        builder.append(str);
        str = reader.readLine();
      }
    }
    return builder.toString();
  }

  public static void verifyServerStripingCapability() {
    verifyCapability(CAPABILITY_SERVER_STRIPING);
  }

  public static void verifyOperatorConsoleCapability() {
    verifyCapability(CAPABILITY_OPERATOR_CONSOLE);
  }

  // public static int maxClientCount() {
  // assertLicenseValid();
  // return getLicense().maxClientCount();
  // }

  public static void verifySecurityCapability() {
    verifyCapability(CAPABILITY_SECURITY);
  }

  public static String licensedCapabilities() {
    assertLicenseValid();
    return getLicense().getRequiredProperty(LICENSE_CAPABILITIES);
  }

  public static void verifyServerArrayOffheapCapability(String maxOffHeapConfigured) {
    registerJvmWithLicenseManagerIfRequired(getLicense());
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    Map<String, Serializable> keyValuePairs = new HashMap<String, Serializable>();
    keyValuePairs.put(LicenseServerConstants.MEMORY, maxOffHeapConfigured);
    keyValuePairs.put(LicenseServerConstants.JVM_UUID, JVM_ID);
    executeQuery(LicenseServerConstants.REQUEST_L2_BM_PATH, keyValuePairs);

  }

  public static void verifyAuthenticationCapability() {
    verifyCapability(CAPABILITY_AUTHENTICATION);
  }

  private static void registerJvmWithLicenseManagerIfRequired(License license) {
    if (!vmRegistered) {
      // releasePreviousL2BMUsage();
      Map<String, Serializable> keyValuePairs = new HashMap<String, Serializable>();
      keyValuePairs.put(LicenseServerConstants.JVM_UUID, JVM_ID);
      keyValuePairs.put(LicenseServerConstants.SECRET_CODE, encode(license.signature(), JVM_ID));
      keyValuePairs.put(LicenseServerConstants.JVM_NAME, JVM_NAME);
      // now register this jvm to licenseManager
      executeQuery(LicenseServerConstants.REGISTER_PATH, keyValuePairs);
      afterGranted();
      vmRegistered = true;
    }
  }

  private synchronized static void afterGranted() {
    License license = getLicense();
    Date expiryDate = license.expirationDate();
    scheduleExpiredTimerIfNeeded(expiryDate);
    scheduleLeaseTimerIfNeeded(expiryDate);
  }

  private static void unRegisterJvm(String jvmId) {
    Map<String, Serializable> keyValuePairs = new HashMap<String, Serializable>();
    keyValuePairs.put(LicenseServerConstants.JVM_UUID, jvmId);
    executeQuery(LicenseServerConstants.UNREGISTER_PATH, keyValuePairs);
  }

  private static void saveStateToFile() {
    StringBuffer sb = new StringBuffer(JVM_ID);
    try {
      SerializationUtils.serialize(sb, new FileOutputStream(stateDumpFile));
    } catch (FileNotFoundException e) {
      LOGGER.warn("saveStateToFile File " + stateDumpFile.getAbsolutePath() + " does not exist " + e);
    }
  }

  private static void releasePreviousBMUsage() {
    if (stateDumpFile.exists()) {
      try {
        String str = FileUtils.readFileToString(stateDumpFile);
        String oldJvmId = str.split(DELIMITER)[0];
        unRegisterJvm(oldJvmId);
        FileUtils.forceDelete(stateDumpFile);
      } catch (IOException e) {
        LOGGER.warn("releasePreviousL1BMUsage File " + stateDumpFile.getAbsolutePath() + " does not exist " + e);
      }
    }
  }

  private static void scheduleLeaseTimerIfNeeded(Date expirationDate) {
    if (expirationDate != null && new Date().before(expirationDate)) {
      killLeaseTimer();
      leaseTimer = new Timer("Lease renew timer", true);
      long leaseTime = renewBMLease();
      if (leaseTime > TimeUnit.HOURS.toMillis(1L)) {
        leaseTimer.schedule(getLeaseTimerTask(), leaseTime / 2);
      } else {
        LOGGER.warn("Not scheduling lease timer because lease time is " + leaseTime);
      }
    }
  }

  private static TimerTask getLeaseTimerTask() {
    return new TimerTask() {
      @Override
      public void run() {
        Date expiryDate = getLicense().expirationDate();
        scheduleLeaseTimerIfNeeded(expiryDate);
      }
    };
  }

  private static long renewBMLease() {
    Map<String, Serializable> keyValuePairs = new HashMap<String, Serializable>();
    keyValuePairs.put(LicenseServerConstants.JVM_UUID, JVM_ID);
    Map<String, String> responseMap = executeQuery(LicenseServerConstants.EXTEND_LEASE_PATH, keyValuePairs);
    long leaseTime = Long.parseLong(responseMap.get(LicenseServerConstants.LEASE_TIME));
    return leaseTime;
  }

  private static void scheduleExpiredTimerIfNeeded(Date expirationDate) {
    if (expirationDate != null && new Date().before(expirationDate)) {
      killExpiryTimer();
      expiredTimer = new Timer("Licensed expired timer", true);
      long delayTime = expirationDate.getTime() - System.currentTimeMillis();
      delayTime = delayTime < 0 ? 100 : delayTime;
      expiredTimer.scheduleAtFixedRate(getExpiredTimerTask(), delayTime, LOG_EXPIRED_LICENSE_PERIOD);
    }
  }

  private static TimerTask getExpiredTimerTask() {
    return new TimerTask() {
      @Override
      public void run() {
        LOGGER.error("Your Terracotta license has expired {}", getLicense().expirationDate());
      }
    };
  }

  private synchronized static void killLeaseTimer() {
    if (leaseTimer != null) {
      leaseTimer.cancel();
      leaseTimer = null;
    }
  }

  private synchronized static void killExpiryTimer() {
    if (expiredTimer != null) {
      expiredTimer.cancel();
      expiredTimer = null;
    }
  }

  private static String encode(String signature, String jvmId) {
    return "DUMMY_CODE";
  }
}
