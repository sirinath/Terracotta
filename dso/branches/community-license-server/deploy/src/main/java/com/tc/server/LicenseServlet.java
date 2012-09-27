/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

import org.mortbay.util.ajax.JSON;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;

import com.tc.license.LicenseUsageManager;
import com.tc.license.LicenseUsageManager.LicenseServerState;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LicenseServlet extends RestfulServlet {

  public static String        LICENSE_USAGE_MANAGER_ATTRIBUTE = "LICENSE_USAGE_MANAGER_ATTRIBUTE";
  private LicenseUsageManager licenseUsageManager;

  @Override
  public void init() {
    this.licenseUsageManager = (LicenseUsageManager) getServletContext().getAttribute(LICENSE_USAGE_MANAGER_ATTRIBUTE);
  }

  public void methodFetchLicense(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    License license = licenseUsageManager.getLicense();
    Map<String, String> responseMap = new HashMap<String, String>();
    responseMap.put(LicenseServerConstants.LICENSE, license.toString());
    sendResponse(response, responseMap, true);
  }

  public void methodState(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    LicenseServerState state = licenseUsageManager.getState();
    Map<String, String> responseMap = new HashMap<String, String>();
    responseMap.put(LicenseServerConstants.STATE, state.toString());
    sendResponse(response, responseMap, true);
  }

  public void methodAllocateL1BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      String cacheName = request.getParameter(LicenseServerConstants.FULLY_QUALIFIED_CACHE_NAME);
      Long memory = Long.parseLong(request.getParameter(LicenseServerConstants.MEMORY));
      licenseUsageManager.allocateL1BigMemory(uuid, cacheName, memory);
      sendResponse(response, null, true);
    } catch (Exception e) {
      sendResponse(response, null, false);
    }
  }

  public void methodReleaseL1BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      String cacheName = request.getParameter(LicenseServerConstants.FULLY_QUALIFIED_CACHE_NAME);
      licenseUsageManager.releaseL1BigMemory(uuid, cacheName);
      sendResponse(response, null, true);
    } catch (Exception e) {
      sendResponse(response, null, false);
    }
  }

  public void methodReloadLicense(final HttpServletRequest request, final HttpServletResponse response)
  throws Throwable {
    try {
      String license = request.getParameter(LicenseServerConstants.LICENSE);
      licenseUsageManager.reloadLicense(license);
    } catch (Exception e) {
    sendResponse(response, null, false);
  }
    
  }

  private void sendResponse(final HttpServletResponse response, Map<String, String> attributes, boolean success)
      throws Exception {
    final Map<String, String> responseMap = new HashMap<String, String>();

    responseMap.put(LicenseServerConstants.RESPONSE_CODE, success ? LicenseServerConstants.SUCCESS_CODE
        : LicenseServerConstants.FAILURE_CODE);
    if (attributes != null) {
      responseMap.putAll(attributes);
    }
    print(response, JSON.toString(responseMap));
  }

  public void methodTest(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    printOk(response);
  }

  public void methodHello(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    print(response, "Hello");
  }

  public void methodException(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    throw new LicenseException("Something Worng with your License");
  }

}
