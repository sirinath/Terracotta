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
import com.tc.logging.CustomerLogging;
import com.tc.logging.TCLogger;
import com.tc.logging.TCLogging;
import com.tc.net.NodeID;
import com.tc.net.groups.GroupMessage;
import com.tc.net.groups.GroupMessageListener;

import java.io.ByteArrayInputStream;
import java.util.Date;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Class to maintain the current state of the cluster in terms of allocated resources and resources available.
 */
public class LicenseUsageManagerImpl implements LicenseUsageManager, StateChangeListener, GroupMessageListener {

  private LicenseServerState                                          state           = LicenseServerState.UNINITIALIZED;
  private LicenseUsageState                                           licenseUsageState;
  private final CopyOnWriteArrayList<LicenseUsageStateChangeListener> listeners       = new CopyOnWriteArrayList<LicenseUsageStateChangeListener>();
  private static final TCLogger                                       logger          = TCLogging
                                                                                          .getLogger(LicenseUsageManagerImpl.class);
  private static final TCLogger                                       consoleLogger   = CustomerLogging
                                                                                          .getConsoleLogger();
  private final LicenseValidationCallback                             currentServerValidationCallback;
  private TimerTask                                                   expirationTimerTask;
  private final Timer                                                 expirationTimer = new Timer();

  public LicenseUsageManagerImpl(License license, LicenseValidationCallback licenseValidationCallback) {
    this.licenseUsageState = new LicenseUsageState();
    licenseUsageState.setLicense(license);
    currentServerValidationCallback = licenseValidationCallback;
  }

  @Override
  public synchronized void registerNode(String jvmId, String machineName, String checksum) throws LicenseException {
    if (licenseUsageState.isVMRegistered(jvmId)) {
      audit("Tried to Register an already Registered Jvm" + "(JVM id =" + jvmId + ", JVM name =" + machineName
            + ") is already registered, DENIED!!");
      throw new LicenseException("(JVM id =" + jvmId + ", JVM name =" + machineName + ") is already registered");
    }
    // TODO: Compare jvmId+license with checksum
    licenseUsageState.registerVM(jvmId, machineName);
    licenseUsageStateChanged();
    audit("SuccessFully Registered (JVM id = " + jvmId + " , JVM Name =" + machineName);
  }

  @Override
  public synchronized void unregisterNode(String jvmId) {
    if (!licenseUsageState.isVMRegistered(jvmId)) {
      audit("Tried to UnRegister a Jvm which was not found registered --> (JVM id =" + jvmId + " Jvm Name="
            + licenseUsageState.getNameForUUID(jvmId) + ", Action can't be performed");
      throw new LicenseException("Trying to UnRegister a Jvm which was not found registered --> (JVM id =" + jvmId
                                 + "JVM Name = " + licenseUsageState.getNameForUUID(jvmId));
    }
    freeUpAllResources(jvmId);
    licenseUsageStateChanged();
    audit("JvmId  = " + jvmId + "name = " + licenseUsageState.getNameForUUID(jvmId) + ",Unregistered Successfully");
  }

  @Override
  public synchronized boolean allocateL1BigMemory(String clientUUID, String fullyQualifiedCacheName, long memoryInBytes)
      throws LicenseException {
    verifyCapability(CAPABILITY_EHCACHE_OFFHEAP);

    String licenseLimitString = licenseUsageState.getLicense().getRequiredProperty(EHCACHE_MAX_OFFHEAP);
    long licenseLimitBytes = MemorySizeParser.parse(licenseLimitString);

    long current = licenseUsageState.getCurrentL1OffHeapUsage(fullyQualifiedCacheName, fullyQualifiedCacheName);

    if ((current + memoryInBytes) > licenseLimitBytes) {
      audit("Attempt to exceed offHeap license limit of " + licenseLimitString + " by addition of " + memoryInBytes
            + " bytes for cache [" + fullyQualifiedCacheName + "]" + "By Client Name = "
            + licenseUsageState.getNameForUUID(clientUUID) + " ClientUUID = " + clientUUID + " ,DENIED!!");

      throw new LicenseException("Attempt to exceed offHeap license limit of " + licenseLimitString
                                 + " by addition of " + memoryInBytes + " bytes for cache [" + fullyQualifiedCacheName
                                 + "]" + "By Client Name = " + licenseUsageState.getNameForUUID(clientUUID)
                                 + " ClientUUID = " + clientUUID);
    }

    licenseUsageState.allocateL1BM(clientUUID, fullyQualifiedCacheName, memoryInBytes);
    licenseUsageStateChanged();
    audit("Allocated " + memoryInBytes + "Bytes to clientUUid = " + clientUUID + "Cache Name = "
          + fullyQualifiedCacheName + " On L1" + "to Client Name = " + licenseUsageState.getNameForUUID(clientUUID)
          + " ClientUUID = " + clientUUID);
    return true;
  }

  @Override
  public synchronized void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName) {
    licenseUsageState.releaseL1BigMemory(clientUUID, fullyQualifiedCacheName);
    licenseUsageStateChanged();
    audit("Released BM from " + fullyQualifiedCacheName + "from Client = " + clientUUID + "L1 Client Name ="
          + licenseUsageState.getNameForUUID(clientUUID));
  }

  @Override
  public synchronized void allocateL2BigMemory(String serverUUID, String memory) {
    verifyCapability(CAPABILITY_TERRACOTTA_SERVER_ARRAY_OFFHEAP);
    String maxHeapSizeFromLicense = licenseUsageState.getLicense()
        .getRequiredProperty(TERRACOTTA_SERVER_ARRAY_MAX_OFFHEAP);
    long maxOffHeapLicensedInBytes = MemorySizeParser.parse(maxHeapSizeFromLicense);
    long memoryRequired = MemorySizeParser.parse(memory);

    boolean offHeapSizeAllowed = memoryRequired + licenseUsageState.getCurrentL2OffHeapUsage(serverUUID) <= maxOffHeapLicensedInBytes;
    if (!offHeapSizeAllowed) {
      audit("Your license only allows up to " + maxHeapSizeFromLicense
            + " in offheap size. Your Terracotta server is configured with " + memory + " ServerName = "
            + licenseUsageState.getNameForUUID(serverUUID) + " serverUUid = " + serverUUID + " asked for " + memory
            + " bytes, request can not be FulFilled");
      throw new LicenseException("Your license only allows up to " + maxHeapSizeFromLicense
                                 + " in offheap size. Your Terracotta server is configured with " + memory
                                 + " ServerName = " + licenseUsageState.getNameForUUID(serverUUID) + " serverUUid = "
                                 + serverUUID + " asked for " + memory + " bytes, request can not be FulFilled");
    }
    licenseUsageState.allocateL2BigMemory(serverUUID, memoryRequired);
    licenseUsageStateChanged();
    audit("Allocated " + memory + "Bytes of BM to L2 with ServerUUid = " + serverUUID + " ServerName = "
          + licenseUsageState.getNameForUUID(serverUUID));

  }

  @Override
  public synchronized void releaseL2BigMemory(String serverUUID) {
    licenseUsageState.releaseL2BigMemory(serverUUID);
    licenseUsageStateChanged();
    audit("Released BM from Server = " + serverUUID + "server Name = " + licenseUsageState.getNameForUUID(serverUUID));

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
    audit("License Reloaded SuccessFully");
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
  public void l2StateChanged(StateChangedEvent sce) {
    synchronized (this) {
      if (sce.movedToActive()) {
        this.state = LicenseServerState.ACTIVE;
        scheduleLeaseExpiryTimer();

      } else if (sce.getCurrentState() == StateManager.PASSIVE_STANDBY) {
        this.state = LicenseServerState.PASSIVE;
        verifyAndConsumeLicenseForThisServer();
        cancelAlreadyScheduledExpirationTask();
      } else {
        this.state = LicenseServerState.UNINITIALIZED;
      }
    }
    verifyAndConsumeLicenseForThisServer();
  }

  private void verifyAndConsumeLicenseForThisServer() {
    try {
      currentServerValidationCallback.verifyAndConsumeLicense();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void scheduleLeaseExpiryTimer() {

    cancelAlreadyScheduledExpirationTask();
    expirationTimerTask = new TimerTask() {
      @Override
      public void run() {
        LicenseUsageManagerImpl.this.removeAllExpiredLease();
      }
    };
    expirationTimer.schedule(expirationTimerTask, new Date(licenseUsageState.getNextLeaseExpiryTime()));
  }

  private void cancelAlreadyScheduledExpirationTask() {
    if (expirationTimerTask != null) {
      expirationTimerTask.cancel();
    }
  }

  private void removeAllExpiredLease() {
    licenseUsageState.removeAllExpiredLease();
    scheduleLeaseExpiryTimer();
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

  public void audit(String message) {
    logger.warn("LicenseAudit:" + message);
    consoleLogger.warn("LicenseAudit" + message);
  }

  @Override
  public long renewLease(String vmId) {
    return licenseUsageState.renewLease(vmId);
  }

}
