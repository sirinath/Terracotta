package com.tc.license;

import static org.terracotta.license.LicenseConstants.CAPABILITY_EHCACHE_OFFHEAP;
import static org.terracotta.license.LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP;
import static org.terracotta.license.LicenseConstants.EHCACHE_MAX_OFFHEAP;
import static org.terracotta.license.LicenseConstants.TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP;

import org.terracotta.license.License;
import org.terracotta.license.LicenseException;
import org.terracotta.license.util.MemorySizeParser;

import java.util.HashMap;
import java.util.Map;

/**
 * Class to maintain the current state of the cluster in terms of allocated resources and resources available.
 */
public class LicenseUsageManagerImpl implements LicenseUsageManager {

  // map of <clientUUID,<FQCacheName,BigMemoryUsed>>
  private final Map<String, Map<String, Long>> l1BigMemoryUsage = new HashMap<String, Map<String, Long>>();
  // map of serverUUID,BigMemoryUsed
  private final Map<String, Long>              l2BigMemoryUsage = new HashMap<String, Long>();

  public LicenseUsageManagerImpl() {
    //
  }

  private License license;

  @Override
  public synchronized boolean allocateL1BigMemory(String clientUUID, String fullyQualifiedCacheName, long memoryInBytes) {
    verifyCapability(CAPABILITY_EHCACHE_OFFHEAP);

    String licenseLimitString = license.getRequiredProperty(EHCACHE_MAX_OFFHEAP);
    long licenseLimitBytes = MemorySizeParser.parse(licenseLimitString);

    long current = getCurrentL1OffHeapUsage(fullyQualifiedCacheName, fullyQualifiedCacheName);

    if ((current + memoryInBytes) > licenseLimitBytes) {

    throw new LicenseException("Attempt to exceed offHeap license limit of " + licenseLimitString + " by addition of "
                               + memoryInBytes + " bytes for cache [" + fullyQualifiedCacheName + "]"); }

    Map<String, Long> map = l1BigMemoryUsage.get(clientUUID);
    if (map == null) {
      map = new HashMap<String, Long>();
      l1BigMemoryUsage.put(clientUUID, map);
    }
    map.put(fullyQualifiedCacheName, memoryInBytes);
    return true;

  }

  @Override
  public void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName) {
    Map<String, Long> map = l1BigMemoryUsage.get(clientUUID);
    if (map != null) {
      map.remove(fullyQualifiedCacheName);
    }
  }

  public void allocateL2BigMemory(String serverUUID, String memory) {
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    String maxHeapSizeFromLicense = license.getRequiredProperty(TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP);
    long maxOffHeapLicensedInBytes = MemorySizeParser.parse(maxHeapSizeFromLicense);
    long maxOffHeapConfiguredInBytes = MemorySizeParser.parse(memory);

    boolean offHeapSizeAllowed = maxOffHeapConfiguredInBytes <= maxOffHeapLicensedInBytes;
    if (!offHeapSizeAllowed) { throw new LicenseException(
                                                          "Your license only allows up to "
                                                              + maxHeapSizeFromLicense
                                                              + " in offheap size. Your Terracotta server is configured with "
                                                              + memory); }
    l2BigMemoryUsage.put(serverUUID, maxOffHeapConfiguredInBytes);

  }

  @Override
  public void releaseL2BigMemory(String serverUUID) {
    l2BigMemoryUsage.remove(serverUUID);
  }

  @Override
  public void freeUpAllResources(String UUID) {
    //
  }

  @Override
  public void l1Joined(String clientUUID) {
    // TODO Auto-generated method stub

  }

  @Override
  public void l1Removed(String clientUUID) {
    // TODO Auto-generated method stub

  }

  @Override
  public boolean verifyCapability(String capability) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public boolean reloadLicense(License license) {
    // TODO Auto-generated method stub
    return false;
  }

  @Override
  public void allocateL2BigMemory(String serverUUID, long memoryInBytes) {
    // TODO Auto-generated method stub

  }

  private long getCurrentL1OffHeapUsage(String excludeClientUUID, String excludeCacheName) {
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

  @Override
  public License getLicense() {
    return license;
  }

  @Override
  public LicenseServerState getState() {
    return LicenseServerState.UNINITIALIZED;
  }

}
