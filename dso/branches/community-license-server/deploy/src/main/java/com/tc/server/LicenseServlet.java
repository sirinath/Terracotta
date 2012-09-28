/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.server;

import org.mortbay.util.ajax.JSON;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;

import com.tc.license.LicenseServerConstants;
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

  public void methodRegisterNode(final HttpServletRequest request, final HttpServletResponse response) throws Exception {
    try {
      String jvmId = request.getParameter(LicenseServerConstants.JVM_UUID);
      String machineName = request.getParameter(LicenseServerConstants.MACHINE_NAME);
      String checksum = request.getParameter(LicenseServerConstants.CHECKSUM);
      licenseUsageManager.registerNode(jvmId, machineName, checksum);

      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodUnregisterNode(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception {
    try {
      String jvmId = request.getParameter(LicenseServerConstants.JVM_UUID);
      licenseUsageManager.unregisterNode(jvmId);
      sendResponse(response, getSuccessResponseMap());

    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodFetchLicense(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    License license = licenseUsageManager.getLicense();
    Map<String, String> responseMap = getSuccessResponseMap();
    responseMap.put(LicenseServerConstants.LICENSE, license.toString());
    sendResponse(response, responseMap);
  }

  public void methodState(final HttpServletRequest request, final HttpServletResponse response) throws Throwable {
    LicenseServerState state = licenseUsageManager.getState();
    Map<String, String> responseMap = getSuccessResponseMap();
    responseMap.put(LicenseServerConstants.STATE, state.toString());
    sendResponse(response, responseMap);
  }

  public void methodAllocateL1BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      String cacheName = request.getParameter(LicenseServerConstants.FULLY_QUALIFIED_CACHE_NAME);
      Long memory = Long.parseLong(request.getParameter(LicenseServerConstants.MEMORY));
      licenseUsageManager.allocateL1BigMemory(uuid, cacheName, memory);
      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodReleaseL1BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      String cacheName = request.getParameter(LicenseServerConstants.FULLY_QUALIFIED_CACHE_NAME);
      licenseUsageManager.releaseL1BigMemory(uuid, cacheName);
      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodAllocateL2BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      Long memory = Long.parseLong(request.getParameter(LicenseServerConstants.MEMORY));
      licenseUsageManager.allocateL2BigMemory(uuid, memory);
      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodReleaseL2BigMemory(final HttpServletRequest request, final HttpServletResponse response)
      throws Exception {
    try {
      String uuid = request.getParameter(LicenseServerConstants.JVM_UUID);
      licenseUsageManager.releaseL2BigMemory(uuid);
      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  public void methodReloadLicense(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      String license = request.getParameter(LicenseServerConstants.LICENSE);
      licenseUsageManager.reloadLicense(license);
      sendResponse(response, getSuccessResponseMap());
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }
  }

  /**
   * To be used only for internal testing purposes. Disabled this Production.
   */
  public void methodLicenseUsageInfo(final HttpServletRequest request, final HttpServletResponse response)
      throws Throwable {
    try {
      Map licenseUsageInfo = licenseUsageManager.getLicenseUsageInfo();
      Map<String, String> responseMap = getSuccessResponseMap();
      responseMap.put(LicenseServerConstants.LICENSE_USAGE_INFO, JSON.toString(licenseUsageInfo));
    } catch (Exception e) {
      sendResponse(response, getFailureResponseMap(e));
    }

  }

  private void sendResponse(final HttpServletResponse response, Map<String, String> attributes) throws Exception {
    final Map<String, String> responseMap = (attributes == null) ? new HashMap<String, String>() : attributes;
    print(response, JSON.toString(responseMap));
  }

  private Map<String, String> getSuccessResponseMap() {
    Map<String, String> paramMap = new HashMap<String, String>();
    paramMap.put(LicenseServerConstants.RESPONSE_CODE, LicenseServerConstants.SUCCESS_CODE);
    return paramMap;
  }

  private Map<String, String> getFailureResponseMap(Exception e) {
    Map<String, String> paramMap = new HashMap<String, String>();
    paramMap.put(LicenseServerConstants.RESPONSE_CODE, LicenseServerConstants.FAILURE_CODE);
    paramMap.put(LicenseServerConstants.FAILURE_MESSAGE, e.getMessage());
    return paramMap;
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
