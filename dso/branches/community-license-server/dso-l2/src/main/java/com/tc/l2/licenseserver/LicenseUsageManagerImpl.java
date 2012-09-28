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
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to maintain the current state of the cluster in terms of allocated resources and resources available.
 */
public class LicenseUsageManagerImpl implements LicenseUsageManager, StateChangeListener, GroupMessageListener {

  private LicenseServerState                                          state     = LicenseServerState.UNINITIALIZED;
  private LicenseUsageState                                           licenseUsageState;
  private final CopyOnWriteArrayList<LicenseUsageStateChangeListener> listeners = new CopyOnWriteArrayList<LicenseUsageStateChangeListener>();

  public LicenseUsageManagerImpl(License license) {
    this.licenseUsageState = new LicenseUsageState();
    licenseUsageState.setLicense(license);
  }

  @Override
  public synchronized void registerNode(String jvmId, String machineName, String checksum) throws LicenseException {
    if (licenseUsageState.isVMRegistered(jvmId)) { throw new LicenseException("(JVM id =" + jvmId + ", JVM name ="
                                                                              + machineName + ") is already registered"); }
    // TODO: Compare jvmId+license with checksum
    licenseUsageState.registerVM(jvmId, machineName);
    licenseUsageStateChanged();
  }

  @Override
  public synchronized void unregisterNode(String jvmId) {
    if (!licenseUsageState.isVMRegistered(jvmId)) { throw new LicenseException("(JVM id =" + jvmId
                                                                               + ") was already registered"); }
    freeUpAllResources(jvmId);
    licenseUsageStateChanged();
  }

  @Override
  public synchronized boolean allocateL1BigMemory(String clientUUID, String fullyQualifiedCacheName, long memoryInBytes)
      throws LicenseException {
    verifyCapability(CAPABILITY_EHCACHE_OFFHEAP);

    String licenseLimitString = licenseUsageState.getLicense().getRequiredProperty(EHCACHE_MAX_OFFHEAP);
    long licenseLimitBytes = MemorySizeParser.parse(licenseLimitString);

    long current = licenseUsageState.getCurrentL1OffHeapUsage(fullyQualifiedCacheName, fullyQualifiedCacheName);

    if ((current + memoryInBytes) > licenseLimitBytes) { throw new LicenseException(
                                                                                    "Attempt to exceed offHeap license limit of "
                                                                                        + licenseLimitString
                                                                                        + " by addition of "
                                                                                        + memoryInBytes
                                                                                        + " bytes for cache ["
                                                                                        + fullyQualifiedCacheName + "]");
    //
    }

    licenseUsageState.allocateL1BM(clientUUID, fullyQualifiedCacheName, memoryInBytes);
    licenseUsageStateChanged();
    return true;
  }

  @Override
  public synchronized void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName) {
    licenseUsageState.releaseL1BigMemory(clientUUID, fullyQualifiedCacheName);
    licenseUsageStateChanged();
  }

  @Override
  public synchronized void allocateL2BigMemory(String serverUUID, long memory) {
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    String maxHeapSizeFromLicense = licenseUsageState.getLicense()
        .getRequiredProperty(TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP);
    long maxOffHeapLicensedInBytes = MemorySizeParser.parse(maxHeapSizeFromLicense);

    boolean offHeapSizeAllowed = memory + licenseUsageState.getCurrentL2OffHeapUsage(serverUUID) <= maxOffHeapLicensedInBytes;
    if (!offHeapSizeAllowed) { throw new LicenseException(
                                                          "Your license only allows up to "
                                                              + maxHeapSizeFromLicense
                                                              + " in offheap size. Your Terracotta server is configured with "
                                                              + memory); }
    licenseUsageState.allocateL2BigMemory(serverUUID, memory);
    licenseUsageStateChanged();

  }

  @Override
  public synchronized void releaseL2BigMemory(String serverUUID) {
    licenseUsageState.releaseL2BigMemory(serverUUID);
    licenseUsageStateChanged();
  }

  public synchronized void freeUpAllResources(String jvmId) {
    licenseUsageState.unregisterVM(jvmId); // Remove the jvmId from deallocate all available licenses
    licenseUsageStateChanged();
  }

  @Override
  public synchronized boolean verifyCapability(String capability) {
    return this.licenseUsageState.getLicense().capabilities().contains(capability);
  }

  @Override
  public synchronized boolean reloadLicense(String licenseParam) {
    AbstractLicenseResolverFactory factory = new EnterpriseLicenseResolverFactory();
    License licenseResolved = factory.resolveLicense(new ByteArrayInputStream(licenseParam.getBytes()));
    licenseUsageState.setLicense(licenseResolved);
    licenseUsageStateChanged();
    return true;
  }

  @Override
  public synchronized License getLicense() {
    return licenseUsageState.getLicense();
  }

  @Override
  public synchronized LicenseServerState getState() {
    return state;
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
    if (msg instanceof LicenseStateMessage) {
      LicenseStateMessage licenseStateMessage = (LicenseStateMessage) msg;
      this.licenseUsageState = licenseStateMessage.getLicenseUsageState();
    } else {
      throw new AssertionError("Message of Unknown type received :" + msg.getClass().getName());
    }
  }

  private void licenseUsageStateChanged() {
    for (LicenseUsageStateChangeListener listener : listeners) {
      listener.licenseStateChanged(licenseUsageState);
    }
  }

  public void registerLicenseStateChangeListener(LicenseUsageStateChangeListener listener) {
    listeners.add(listener);
  }

  public void unregisterLicenseStateChangeListener(LicenseUsageStateChangeListener listener) {
    listeners.remove(listener);
  }

  @Override
  public Map getLicenseUsageInfo() {
    return licenseUsageState.getLicenseUsageInfo();
  }

}
