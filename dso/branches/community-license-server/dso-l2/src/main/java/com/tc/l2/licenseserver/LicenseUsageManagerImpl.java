package com.tc.l2.licenseserver;

import static org.terracotta.license.LicenseConstants.CAPABILITY_EHCACHE_OFFHEAP;
import static org.terracotta.license.LicenseConstants.CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP;
import static org.terracotta.license.LicenseConstants.EHCACHE_MAX_OFFHEAP;
import static org.terracotta.license.LicenseConstants.TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP;

import org.terracotta.license.AbstractLicenseResolverFactory;
import org.terracotta.license.EnterpriseLicenseResolverFactory;
import org.terracotta.license.License;
import org.terracotta.license.LicenseException;
import org.terracotta.license.util.MemorySizeParser;

import com.tc.l2.context.StateChangedEvent;
import com.tc.l2.state.StateChangeListener;
import com.tc.l2.state.StateManager;
import com.tc.license.LicenseUsageManager;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to maintain the current state of the cluster in terms of allocated resources and resources available.
 */
public class LicenseUsageManagerImpl implements LicenseUsageManager, StateChangeListener, GroupMessageListener {

  private LicenseServerState                   state            = LicenseServerState.UNINITIALIZED;

  // map of <clientUUID,<FQCacheName,BigMemoryUsed>>
  private final Map<String, Map<String, Long>> l1BigMemoryUsage = new HashMap<String, Map<String, Long>>();
  // map of serverUUID,BigMemoryUsed
  private final Map<String, Long>              l2BigMemoryUsage = new HashMap<String, Long>();

  private License                              license;

  public LicenseUsageManagerImpl(License license) {
    validateLicense(license);
    this.license = license;
  }

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
  public synchronized void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName) {
    Map<String, Long> map = l1BigMemoryUsage.get(clientUUID);
    if (map != null) {
      map.remove(fullyQualifiedCacheName);
    }
  }

  public synchronized void allocateL2BigMemory(String serverUUID, String memory) {
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
  public synchronized void releaseL2BigMemory(String serverUUID) {
    l2BigMemoryUsage.remove(serverUUID);
  }

  @Override
  public synchronized void freeUpAllResources(String UUID) {
    //
  }

  @Override
  public synchronized void l1Joined(String clientUUID) {
    // TODO Auto-generated method stub

  }

  @Override
  public synchronized void l1Removed(String clientUUID) {
    // TODO Auto-generated method stub

  }

  @Override
  public synchronized boolean verifyCapability(String capability) {
    return this.license.capabilities().contains(capability);
  }

  @Override
  public synchronized boolean reloadLicense(String licenseParam) {
    AbstractLicenseResolverFactory factory = new EnterpriseLicenseResolverFactory();
    License licenseResolved = factory.resolveLicense(new ByteArrayInputStream(licenseParam.getBytes()));
    validateLicense(licenseResolved);
    this.license = licenseResolved;
    return true;
  }

  @Override
  public synchronized void allocateL2BigMemory(String serverUUID, long memoryInBytes) {
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
  public synchronized License getLicense() {
    return license;
  }

  @Override
  public synchronized LicenseServerState getState() {
    return state;
  }

  private void validateLicense(License licenseParam) {

    if (licenseParam == null) {
      throw new LicenseException("Your Terracotta license is null ");
    } else {
      if (licenseParam.isExpired()) { throw new LicenseException("Your Terracotta license has expired "
                                                                 + license.expirationDate()); }
    }
  }

  @Override
  public synchronized void l2StateChanged(StateChangedEvent sce) {
    if (sce.movedToActive()) {
      this.state = LicenseServerState.ACTIVE;
    } else if (sce.getCurrentState() == StateManager.PASSIVE_STANDBY) {
      this.state = LicenseServerState.PASSIVE;
    } else {
      this.state = LicenseServerState.UNINITIALIZED;
    }
  }

  @Override
  public void messageReceived(NodeID fromNode, GroupMessage msg) {
    //
  }

}
