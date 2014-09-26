/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package net.sf.ehcache.management.service.impl;

import net.sf.ehcache.management.resource.CacheConfigEntity;
import net.sf.ehcache.management.resource.CacheEntity;
import net.sf.ehcache.management.resource.CacheManagerConfigEntity;
import net.sf.ehcache.management.resource.CacheManagerEntity;
import net.sf.ehcache.management.resource.CacheStatisticSampleEntity;
import net.sf.ehcache.management.resource.services.validator.impl.JmxEhcacheRequestValidator;
import net.sf.ehcache.management.service.AgentService;
import net.sf.ehcache.management.service.CacheManagerService;
import net.sf.ehcache.management.service.CacheService;
import net.sf.ehcache.management.service.EntityResourceFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.resource.AgentEntity;
import org.terracotta.management.resource.AgentMetadataEntity;
import org.terracotta.management.resource.Representable;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.UserService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.TsaManagementClientService;
import com.terracotta.management.user.UserInfo;
import com.terracotta.management.web.utils.TSAConfig;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @author Ludovic Orban
 */
public class JmxRepositoryService implements EntityResourceFactory, CacheManagerService, CacheService, AgentService {

  private static final Logger LOG = LoggerFactory.getLogger(JmxRepositoryService.class);

  private final TsaManagementClientService tsaManagementClientService;
  private final JmxEhcacheRequestValidator requestValidator;
  private final RequestTicketMonitor ticketMonitor;
  private final ContextService contextService;
  private final UserService userService;
  private final ExecutorService executorService;
  private final TimeoutService timeoutService;

  public JmxRepositoryService(TsaManagementClientService tsaManagementClientService, JmxEhcacheRequestValidator requestValidator,
                              RequestTicketMonitor ticketMonitor, ContextService contextService, UserService userService,
                              ExecutorService executorService, TimeoutService timeoutService) {
    this.tsaManagementClientService = tsaManagementClientService;
    this.requestValidator = requestValidator;
    this.ticketMonitor = ticketMonitor;
    this.contextService = contextService;
    this.userService = userService;
    this.executorService = executorService;
    this.timeoutService = timeoutService;
  }

  private static <T> T deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    if (bytes == null) {
      return null;
    }

    ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bytes));
    try {
      return (T)ois.readObject();
    } finally {
      ois.close();
    }
  }

  private static <T extends Representable> Collection<T> rewriteAgentId(Collection<T> representables, String agentId) {
    if (representables != null) {
      for (Representable r : representables) {
        r.setAgentId(agentId);
      }
    } else {
      representables = Collections.emptySet();
    }
    return representables;
  }

  private <R,P> Collection<Future<R>> fanOutCall(Collection<P> parameters, final ParameterizedCallable<R, P> callable) {
    List<Future<R>> futures = new ArrayList<Future<R>>();
    for (final P parameter : parameters) {
      try {
        Future<R> future = executorService.submit(new Callable<R>() {
          @Override
          public R call() throws Exception {
            return callable.call(parameter);
          }
        });
        futures.add(future);
      } catch (RejectedExecutionException ree) {
        try {
          LOG.warn("L1 Executor rejected callable, pausing before submitting next one...", ree);
          Thread.sleep(100L);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
        }
      }
    }
    return futures;
  }

  private static interface ParameterizedCallable<R, P> {
    R call(P param) throws Exception;
  }

  private static <T> Collection<T> collectEntitiesFromFutures(Collection<Future<Collection<T>>> futures, long timeoutInMillis, String methodName) {
    int failedRequests = 0;
    Collection<T> globalResult = new ArrayList<T>();
    long timeLeft = timeoutInMillis;
    for (Future<Collection<T>> future : futures) {
      long before = System.nanoTime();
      try {
        Collection<T> entities = future.get(Math.max(1L, timeLeft), TimeUnit.MILLISECONDS);
        globalResult.addAll(entities);
      } catch (Exception e) {
        future.cancel(true);
        failedRequests++;
        LOG.debug("Future execution error in {}", methodName, e);
      } finally {
        timeLeft -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
      }
    }
    if (failedRequests > 0) {
      LOG.warn(failedRequests + "/" + futures.size() + " L1 agent(s) failed to respond to " + methodName);
    }
    return globalResult;
  }

  @Override
  public void updateCacheManager(String cacheManagerName, CacheManagerEntity resource) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token,
        TSAConfig.getSecurityCallbackUrl(), "updateCacheManager", new Class<?>[] { String.class, CacheManagerEntity.class }, new Object[] { cacheManagerName, resource });
  }

  @Override
  public void createOrUpdateCache(String cacheManagerName, String cacheName, CacheEntity resource) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
        .getSecurityCallbackUrl(), "createOrUpdateCache", new Class<?>[] { String.class, String.class, CacheEntity.class }, new Object[] { cacheManagerName, cacheName, resource });
  }

  @Override
  public void clearCache(String cacheManagerName, String cacheName) throws ServiceExecutionException {
    String ticket = ticketMonitor.issueRequestTicket();
    String token = userService.putUserInfo(contextService.getUserInfo());
    String node = requestValidator.getSingleValidatedNode();

    tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
        .getSecurityCallbackUrl(), "clearCache", new Class<?>[] { String.class, String.class }, new Object[] { cacheManagerName, cacheName });
  }

  @Override
  public Collection<CacheManagerEntity> createCacheManagerEntities(final Set<String> cacheManagerNames, final Set<String> attributes) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheManagerEntity>>> futures = fanOutCall(nodes, new ParameterizedCallable<Collection<CacheManagerEntity>, String>() {
      @Override
      public Collection<CacheManagerEntity> call(String node) throws Exception {
        String ticket = ticketMonitor.issueRequestTicket();
        String token = userService.putUserInfo(userInfo);

        byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "createCacheManagerEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, attributes });
        return rewriteAgentId((Collection<CacheManagerEntity>)deserialize(bytes), node);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "createCacheManagerEntities");
  }

  @Override
  public Collection<CacheManagerConfigEntity> createCacheManagerConfigEntities(final Set<String> cacheManagerNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheManagerConfigEntity>>> futures = fanOutCall(nodes, new ParameterizedCallable<Collection<CacheManagerConfigEntity>, String>() {
      @Override
      public Collection<CacheManagerConfigEntity> call(String node) throws Exception {
        String ticket = ticketMonitor.issueRequestTicket();
        String token = userService.putUserInfo(userInfo);

        byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "createCacheManagerConfigEntities", new Class<?>[] { Set.class }, new Object[] { cacheManagerNames });
        return rewriteAgentId((Collection<CacheManagerConfigEntity>)deserialize(bytes), node);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "createCacheManagerConfigEntities");
  }

  @Override
  public Collection<CacheEntity> createCacheEntities(final Set<String> cacheManagerNames, final Set<String> cacheNames, final Set<String> attributes) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheEntity>>> futures = fanOutCall(nodes, new ParameterizedCallable<Collection<CacheEntity>, String>() {
      @Override
      public Collection<CacheEntity> call(String node) throws Exception {
        String token = userService.putUserInfo(userInfo);
        String ticket = ticketMonitor.issueRequestTicket();

        byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "createCacheEntities", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, attributes });
        return rewriteAgentId((Collection<CacheEntity>)deserialize(bytes), node);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "createCacheEntities");
  }

  @Override
  public Collection<CacheConfigEntity> createCacheConfigEntities(final Set<String> cacheManagerNames, final Set<String> cacheNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheConfigEntity>>> futures = fanOutCall(nodes, new ParameterizedCallable<Collection<CacheConfigEntity>, String>() {
      @Override
      public Collection<CacheConfigEntity> call(String node) throws Exception {
        String ticket = ticketMonitor.issueRequestTicket();
        String token = userService.putUserInfo(userInfo);

        byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "createCacheConfigEntities", new Class<?>[] { Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames });
        return rewriteAgentId((Collection<CacheConfigEntity>)deserialize(bytes), node);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "createCacheConfigEntities");
  }

  @Override
  public Collection<CacheStatisticSampleEntity> createCacheStatisticSampleEntity(final Set<String> cacheManagerNames, final Set<String> cacheNames, final Set<String> statNames) throws ServiceExecutionException {
    Set<String> nodes = requestValidator.getValidatedNodes();
    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<CacheStatisticSampleEntity>>> futures = fanOutCall(nodes, new ParameterizedCallable<Collection<CacheStatisticSampleEntity>, String>() {
      @Override
      public Collection<CacheStatisticSampleEntity> call(String node) throws Exception {
        String token = userService.putUserInfo(userInfo);
        String ticket = ticketMonitor.issueRequestTicket();

        byte[] bytes = tsaManagementClientService.invokeMethod(node, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "createCacheStatisticSampleEntity", new Class<?>[] { Set.class, Set.class, Set.class }, new Object[] { cacheManagerNames, cacheNames, statNames });
        return rewriteAgentId((Collection<CacheStatisticSampleEntity>)deserialize(bytes), node);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "createCacheStatisticSampleEntity");
  }

  @Override
  public Collection<AgentMetadataEntity> getAgentsMetadata(Set<String> ids) throws ServiceExecutionException {
    Set<String> nodes = tsaManagementClientService.getRemoteAgentNodeNames();
    if (ids.isEmpty()) {
      ids = new HashSet<String>(nodes);
    }

    Set<String> idsClone = new HashSet<String>(ids);
    idsClone.removeAll(nodes);
    if (!idsClone.isEmpty()) {
      throw new ServiceExecutionException("Unknown agent IDs : " + idsClone);
    }

    final UserInfo userInfo = contextService.getUserInfo();

    Collection<Future<Collection<AgentMetadataEntity>>> futures = fanOutCall(ids, new ParameterizedCallable<Collection<AgentMetadataEntity>, String>() {
      @Override
      public Collection<AgentMetadataEntity> call(String id) throws Exception {
        String ticket = ticketMonitor.issueRequestTicket();
        String token = userService.putUserInfo(userInfo);

        byte[] bytes = tsaManagementClientService.invokeMethod(id, DfltSamplerRepositoryServiceMBean.class, ticket, token, TSAConfig
            .getSecurityCallbackUrl(), "getAgentsMetadata", new Class<?>[] { Set.class }, new Object[] { Collections.emptySet() });
        return rewriteAgentId((Collection<AgentMetadataEntity>)deserialize(bytes), id);
      }
    });

    return collectEntitiesFromFutures(futures, timeoutService.getCallTimeout(), "getAgentsMetadata");
  }

  @Override
  public Collection<AgentEntity> getAgents(Set<String> idSet) throws ServiceExecutionException {
    Collection<AgentEntity> result = new ArrayList<AgentEntity>();

    Map<String, Map<String, String>> nodes = getAllRemoteAgentNodeDetails();
    if (idSet.isEmpty()) {
      idSet = nodes.keySet();
    }

    for (String id : idSet) {
      if (!nodes.keySet().contains(id)) {
        throw new ServiceExecutionException("Unknown agent ID : " + id);
      }
      Map<String, String> props = nodes.get(id);

      AgentEntity e = new AgentEntity();
      e.setAgentId(id);
      e.setAgencyOf(props.get("Agency"));
      e.setVersion(props.get("Version"));
      result.add(e);
    }

    return result;
  }

  private Map<String, Map<String, String>> getAllRemoteAgentNodeDetails() throws ServiceExecutionException {
    Set<String> remoteAgentNodeNames = tsaManagementClientService.getRemoteAgentNodeNames();

    Collection<Future<Map<String, Map<String, String>>>> futures = fanOutCall(remoteAgentNodeNames, new ParameterizedCallable<Map<String, Map<String, String>>, String>() {
      @Override
      public Map<String, Map<String, String>> call(String remoteAgentNodeName) throws Exception {
        Map<String, String> nodeDetails = tsaManagementClientService.getRemoteAgentNodeDetails(remoteAgentNodeName);
        HashMap<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
        result.put(remoteAgentNodeName, nodeDetails);
        return result;
      }
    });

    Map<String, Map<String, String>> result = new HashMap<String, Map<String, String>>();
    long timeLeft = timeoutService.getCallTimeout();
    int failedRequests = 0;
    for (Future<Map<String, Map<String, String>>> future : futures) {
      long before = System.nanoTime();
      try {
        Map<String, Map<String, String>> map = future.get(Math.max(1L, timeLeft), TimeUnit.MILLISECONDS);
        result.putAll(map);
      } catch (Exception e) {
        future.cancel(true);
        failedRequests++;
        LOG.debug("Future execution error in getAllRemoteAgentNodeDetails", e);
      } finally {
        timeLeft -= TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - before);
      }
    }
    if (failedRequests > 0) {
      LOG.warn(failedRequests + "/" + futures.size() + " L1 agent(s) failed to respond to getAllRemoteAgentNodeDetails");
    }
    return result;
  }

}
