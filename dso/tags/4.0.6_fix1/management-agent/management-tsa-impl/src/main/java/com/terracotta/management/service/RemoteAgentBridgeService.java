/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service;

import net.sf.ehcache.management.service.impl.DfltSamplerRepositoryServiceMBean;
import org.terracotta.management.ServiceExecutionException;

import java.util.Map;
import java.util.Set;

/**
 * An interface for service implementations providing an abstraction of remote agents bridging.
 *
 * @author Ludovic Orban
 */
public interface RemoteAgentBridgeService {

  /**
   * Get the connected remote agents node names. This call does not go over the network.
   *
   * @return a set of connected remote agents node names.
   * @throws ServiceExecutionException
   */
  Set<String> getRemoteAgentNodeNames() throws ServiceExecutionException;

  /**
   * Get the connected remote agent node names and details. This goes over the network to fetch details.
   *
   * @param remoteAgentNodeName the name of the remote agent node to fetch details from.
   * @return a map filled with the remote agent's attributes.
   * @throws ServiceExecutionException
   */
  Map<String, String> getRemoteAgentNodeDetails(String remoteAgentNodeName) throws ServiceExecutionException;

  /**
   * Invoke an method on the remote agent.
   *
   * @param nodeName the remote agent node name.
   * @param clazz the interface of the remote agent MBean bridging calls.
   * @param ticket the security ticket, can be null when no security is required.
   * @param token the security token, can be null when no security is required.
   * @param securityCallbackUrl the security IA service URL the remote agent must call, can be null when no security is required.
   * @param methodName the name of the method to call.
   * @param paramClasses the classes of the called method's parameters.
   * @param params the called method's parameters.
   * @return the serialized response.
   * @throws ServiceExecutionException
   */
  byte[] invokeMethod(String nodeName, Class<DfltSamplerRepositoryServiceMBean> clazz, String ticket, String token,
                      String securityCallbackUrl, String methodName, Class<?>[] paramClasses, Object[] params) throws ServiceExecutionException;

}
