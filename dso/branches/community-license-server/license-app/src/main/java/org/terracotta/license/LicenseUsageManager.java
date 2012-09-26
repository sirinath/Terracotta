package org.terracotta.license;

public interface LicenseUsageManager {

//	public void allocateL1BigMemory(String clientUUID, String fullyQualifiedCacheName, Long memory, MemoryUnit unit);

	public void releaseL1BigMemory(String clientUUID, String fullyQualifiedCacheName);

//	public void allocateL2BigMemory(String serverUUID, Long memory, MemoryUnit unit);

	public void releaseL2BigMemory(String serverUUID);

	public void freeUpAllResources(String UUID);

	public void l1Joined(String clientUUID);

	public void l1Removed(String clientUUID);

	public boolean verifyCapability(String capability);

//	public boolean reloadLicense(License license);
}
