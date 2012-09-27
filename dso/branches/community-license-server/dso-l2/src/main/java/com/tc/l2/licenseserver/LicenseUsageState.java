/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.tc.l2.licenseserver;

import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.EnterpriseLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;

import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class LicenseUsageState implements Serializable {

  // map of <clientUUID,<FQCacheName,BigMemoryUsed>>
  private final Map<String, Map<String, Long>>                        l1BigMemoryUsage = new HashMap<String, Map<String, Long>>();
  // map of jvmUUID, NodeName
  private final Map<String, String>            registeredVMs    = new HashMap<String, String>();
  // map of serverUUID,BigMemoryUsed
  private final Map<String, Long>                                     l2BigMemoryUsage = new HashMap<String, Long>();

  private transient License                    license;
  private String                               licenseAsString;

  public boolean isVMRegistered(String jvmId) {
    return registeredVMs.containsKey(jvmId);
  }

  public void registerVM(String jvmId, String machineName) {
    registeredVMs.put(jvmId, machineName);
  }

  public void allocateL1BM(String clientUUID, String fullyQualifiedCacheName, long memoryInBytes) {
    Map<String, Long> map = l1BigMemoryUsage.get(clientUUID);
    if (map == null) {
      map = new HashMap<String, Long>();
      l1BigMemoryUsage.put(clientUUID, map);
    }
    map.put(fullyQualifiedCacheName, memoryInBytes);
  }

  public void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName) {
    Map<String, Long> map = l1BigMemoryUsage.get(clientUUID);
    if (map != null) {
      map.remove(fullyQualifiedCacheName);
    }

  }

  public void allocateL2BigMemory(String serverUUID, long maxOffHeapConfiguredInBytes) {
    l2BigMemoryUsage.put(serverUUID, maxOffHeapConfiguredInBytes);
  }

  public void releaseL2BigMemory(String serverUUID) {
    l2BigMemoryUsage.remove(serverUUID);

  }

  public void unregisterVM(String jvmId) {
    registeredVMs.remove(jvmId);
    if (l1BigMemoryUsage.containsKey(jvmId)) {
      l1BigMemoryUsage.remove(jvmId);
      return;
    }
    if (l2BigMemoryUsage.containsKey(jvmId)) {
      l2BigMemoryUsage.remove(jvmId);
      return;
    }

  }
  
  /**
   * L1 always asks for aggregate memory. Therefore we exclude the client that made the request for the current
   * allocation.
   */
  public long getCurrentL1OffHeapUsage(String excludeClientUUID, String excludeCacheName) {
    long total = 0L;
    for (String client : l1BigMemoryUsage.keySet()) {
      for (Map.Entry<String, Long> entry : l1BigMemoryUsage.get(client).entrySet()) {
        if (client.equals(excludeClientUUID) && excludeCacheName.equals(excludeCacheName)) {
          continue;
        }
        total += entry.getValue();
      }
    }
    return total;
  }

  public long getCurrentL2OffHeapUsage(String excludeClientUUID) {
    long total = 0L;
    for (Map.Entry<String, Long> entry : l2BigMemoryUsage.entrySet()) {
      total += entry.getValue();
    }
    return total;
  }

  public void setLicense(License licenseResolved) {
    this.license = licenseResolved;
    this.licenseAsString = licenseResolved.toString();
  }

  public License getLicense() {
    if (license == null) {
      AbstractLicenseResolverFactory factory = new EnterpriseLicenseResolverFactory();
      License licenseResolved = factory.resolveLicense(new ByteArrayInputStream(licenseAsString.getBytes()));
      validateLicense(licenseResolved);
      this.license = licenseResolved;
    }
    return license;
  }

  private void validateLicense(License licenseParam) {

    if (licenseParam == null) {
      throw new LicenseException("Your Terracotta license is null ");
    } else {
      if (licenseParam.isExpired()) { throw new LicenseException("Your Terracotta license has expired "
                                                                 + license.expirationDate()); }
    }
  }
}
