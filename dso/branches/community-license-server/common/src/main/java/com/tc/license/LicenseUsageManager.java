package com.tc.license;

import org.terracotta.license.License;

public interface LicenseUsageManager {

  public enum LicenseServerState {
    UNINITIALIZED, ACTIVE, PASSIVE
  }

  public boolean allocateL1BigMemory(String clientUUID, String fullyQualifiedCacheName, long memoryInBytes);

  public void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName);

  public void allocateL2BigMemory(String serverUUID, long memoryInBytes);

  public void releaseL2BigMemory(String serverUUID);

  public void freeUpAllResources(String UUID);

  public void l1Joined(String clientUUID);

  public void l1Removed(String clientUUID);

  public boolean verifyCapability(String capability);

  public boolean reloadLicense(String license);

  public License getLicense();

  public LicenseServerState getState();
}
