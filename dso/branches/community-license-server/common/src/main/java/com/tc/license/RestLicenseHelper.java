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
import org.terracotta.license.ehcache.LicenseServerConstants;

import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.util.ProductInfo;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import junit.framework.Assert;

public class RestLicenseHelper {
  private static final TCLogger CONSOLE_LOGGER         = CustomerLogging.getConsoleLogger();
  private static final Logger LOGGER = LoggerFactory.getLogger(RestLicenseHelper.class);
  private static String       URL_PREFIX = getLicenseServerUrl();
  private static final String LICENSE_SERVER_URL_KEY = "licenseServerUrl";

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

  private static String getStringFrom(HttpEntity entity) throws IllegalStateException, IOException {
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
  private static Map<String, String> executeQuery(String servletPath, Map<String, Serializable> keyValuePairs) {
    HttpResponse response = null;
    HttpGet httpGet = null;
    HttpClient httpClient = new DefaultHttpClient();
    HttpParams params = new BasicHttpParams();
    for (Entry<String, Serializable> entry : keyValuePairs.entrySet()) {
      params.setParameter(entry.getKey(), entry.getValue());
    }
    boolean executed = false;
    while (!executed) {
      try {
        httpGet = new HttpGet(URL_PREFIX + servletPath);
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
        HashMap<String, String> jsonMap = (HashMap<String, String>) JSON.parse(getStringFrom(entity));
        String status = jsonMap.get(LicenseServerConstants.RESPONSE_CODE);
        if (status.equals(LicenseServerConstants.FAILURE_CODE)) {
          String failure_message = jsonMap.get(LicenseServerConstants.FAILURE_MESSAGE);
          Assert.assertNotNull(failure_message);
          throw new LicenseException(failure_message);
        }
        return jsonMap;
      } catch (ClientProtocolException e) {
        LOGGER.warn(servletPath + " got ClientProtocolException " + e + " Sleeping for 1 sec before retry");
        reallySleep(TimeUnit.SECONDS.toMillis(1L));
      } catch (IOException e) {
        LOGGER.warn(servletPath + " got ClientProtocolException " + e + " Sleeping for 1 sec before retry");
        reallySleep(TimeUnit.SECONDS.toMillis(1L));
      } finally {
        httpClient.getConnectionManager().shutdown();
      }
    }
    return null;
  }

  public static void verifyServerStripingCapability() {
    verifyCapability(CAPABILITY_SERVER_STRIPING);
  }

  public static void verifyOperatorConsoleCapability() {
    verifyCapability(CAPABILITY_OPERATOR_CONSOLE);
  }

  public static int maxClientCount() {
    assertLicenseValid();
    return getLicense().maxClientCount();
  }

  public static void verifySecurityCapability() {
    verifyCapability(CAPABILITY_SECURITY);
  }

  public static boolean enterpriseEdition() {
    return ProductInfo.getInstance().isEnterprise();
  }

  public static String licensedCapabilities() {
    assertLicenseValid();
    return getLicense().getRequiredProperty(LICENSE_CAPABILITIES);
  }

  public static void verifyServerArrayOffheapCapability(String maxOffHeapConfigured) {
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    // TODO: Allocate L2 BigMemory
  }

  public static void verifyAuthenticationCapability() {
    verifyCapability(CAPABILITY_AUTHENTICATION);
  }
}
