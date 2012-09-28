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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class LicenseUsageState implements Serializable {

  // map of <clientUUID,<FQCacheName,BigMemoryUsed>>
  private final Map<String, Map<String, Long>> l1BigMemoryUsage = new HashMap<String, Map<String, Long>>();
  // map of jvmUUID, NodeName
  private final Map<String, String>            registeredVMs    = new HashMap<String, String>();
  // map of serverUUID,BigMemoryUsed
  private final Map<String, Long>              l2BigMemoryUsage = new HashMap<String, Long>();

  private final SortedSet<JVMLease>            leaseSet         = new TreeSet<JVMLease>();

  private transient License                    license;
  private String                               licenseAsString;
  public static long                           LEASE_PERIOD     = TimeUnit.HOURS.toMillis(24);

  public boolean isVMRegistered(String jvmId) {
    return registeredVMs.containsKey(jvmId);
  }

  public void registerVM(String jvmId, String machineName) {
    registeredVMs.put(jvmId, machineName);
    leaseSet.add(new JVMLease(jvmId, System.currentTimeMillis() + LEASE_PERIOD));
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
    leaseSet.remove(new JVMLease(jvmId, System.currentTimeMillis()));
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

  public long getNextLeaseExpiryTime() {
    if (leaseSet.size() == 0) {
      return System.currentTimeMillis() + LEASE_PERIOD;
    } else return leaseSet.first().getExpiryTime();
  }

  public boolean removeAllExpiredLease() {
    boolean modified = false;
    while (true) {
      if (leaseSet.size() == 0) { return modified; }
      JVMLease lease = leaseSet.first();
      if (lease.getExpiryTime() < System.currentTimeMillis()) {
        unregisterVM(lease.getExpiredVMId());
        leaseSet.remove(lease);
      } else {
        return modified;
      }

    }
  }

  public static class JVMLease implements Comparable<JVMLease> {

    private Long         expiryTime;
    private final String vmId;

    public JVMLease(String vmId, long expiryTime) {
      this.vmId = vmId;
      this.expiryTime = expiryTime;
    }

    public String getExpiredVMId() {
      return vmId;
    }

    public long getExpiryTime() {
      return this.expiryTime;
    }

    public void renewLease(long newExpiryTime) {
      this.expiryTime = newExpiryTime;
    }

    @Override
    public int compareTo(JVMLease o) {
      return this.expiryTime.compareTo(o.expiryTime);
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((vmId == null) ? 0 : vmId.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      JVMLease other = (JVMLease) obj;
      if (vmId == null) {
        if (other.vmId != null) return false;
      } else if (!vmId.equals(other.vmId)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "VMID : " + vmId + "LeaseExpiryTime" + new Date(getExpiryTime());
    }
  }

  public Map getLicenseUsageInfo() {
    Map allInfoMap = new HashMap();
    allInfoMap.put("ClientUsageInfo", l1BigMemoryUsage);
    allInfoMap.put("L2UsageInfo", l2BigMemoryUsage);
    allInfoMap.put("RegisteredVM", registeredVMs);
    allInfoMap.put("LeaseInfo", leaseSet);
    allInfoMap.put("license", licenseAsString);
    return allInfoMap;
  }

  public String getNameForUUID(String UUID) {
    return registeredVMs.get(UUID);
  }

  public long renewLease(String vmId) {
    leaseSet.remove(new JVMLease(vmId, 0));
    leaseSet.add(new JVMLease(vmId, System.currentTimeMillis() + LEASE_PERIOD));
    return LEASE_PERIOD;
  }

}
