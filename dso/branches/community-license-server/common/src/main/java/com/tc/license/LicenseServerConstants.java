/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.license;

public interface LicenseServerConstants {

  // Request Attributes
  final static String JVM_UUID                   = "jvm_uuid";
  final static String FULLY_QUALIFIED_CACHE_NAME = "cache_name";
  final static String MEMORY     = "memory";
  final static String SECRET_CODE                = "secret_code";
  final static String JVM_NAME                   = "jvm_name";

  // Response Attributes
  final static String RESPONSE_CODE              = "response_code";
  final static String SUCCESS_CODE               = "success";
  final static String FAILURE_CODE               = "failure";
  final static String LICENSE                    = "license";
  final static String STATE                      = "state";
  final static String FAILURE_MESSAGE            = "failure_message";
  final static String LEASE_TIME                 = "lease_time";

  // Servlet paths
  final static String GET_LICENSE_PATH           = "fetchLicense";
  final static String RELEASE_L1_BM_PATH         = "releaseL1BigMemory";
  final static String REQUEST_L1_BM_PATH         = "allocateL1BigMemory";
  final static String EXTEND_LEASE_PATH          = "extendLease";
  final static String REGISTER_PATH              = "registerNode";
  final static String UNREGISTER_PATH            = "unRegisterNode";
  final static String MACHINE_NAME               = "machine_name";
  final static String CHECKSUM                   = "checksum";
  static final String LICENSE_USAGE_INFO         = "licenseUsageInfo";
  static final String REQUEST_L2_BM_PATH         = "allocateL2BigMemory";
  static final String RELEASE_L2_BM_PATH         = "releaseL2BigMemory";

}
