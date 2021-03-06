/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terracotta.management.ServiceExecutionException;
import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;
import org.terracotta.management.resource.services.validator.RequestValidator;

import com.terracotta.management.resource.StatisticsEntityV2;
import com.terracotta.management.resource.services.utils.UriInfoUtils;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.MonitoringServiceV2;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA runtime statistics.
 * 
 * @author Ludovic Orban
 */
@Path("/v2/agents/statistics")
public class MonitoringResourceServiceImplV2 {

  private static final Logger LOG = LoggerFactory.getLogger(MonitoringResourceServiceImplV2.class);

  private final MonitoringServiceV2 monitoringService;
  private final RequestValidator requestValidator;

  public MonitoringResourceServiceImplV2() {
    this.monitoringService = ServiceLocator.locate(MonitoringServiceV2.class);
    this.requestValidator = ServiceLocator.locate(RequestValidator.class);
  }

  public final static String ATTR_QUERY_KEY = "show";

  /**
   * Get a {@code Collection} of {@link StatisticsEntityV2} objects representing the server(s) statistics provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntityV2} objects.
   */
  @GET
  @Path("/v2/servers")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<StatisticsEntityV2> getServerStatistics(@Context UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getServerStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      return monitoringService.getServerStatistics(serverNames, tsaAttrs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link StatisticsEntityV2} objects representing the DGC statistics of each iteration
   * provided by the associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntityV2} objects.
   */
  @GET
  @Path("/v2/dgc")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<StatisticsEntityV2> getDgcStatistics(@Context UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getDgcStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(2).getMatrixParameters().getFirst("serverNames");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      return monitoringService.getDgcStatistics(serverNames);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

  /**
   * Get a {@code Collection} of {@link StatisticsEntityV2} objects representing the client(s) statistics provided by the
   * associated monitorable entity's agent given the request path.
   *
   * @return a a collection of {@link StatisticsEntityV2} objects.
   */
  @GET
  @Path("/v2/clients")
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<StatisticsEntityV2> getClientStatistics(@Context UriInfo info) {
    LOG.debug(String.format("Invoking MonitoringResourceServiceImpl.getClientStatistics: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String ids = info.getPathSegments().get(2).getMatrixParameters().getFirst("ids");
      Set<String> clientIds = ids == null ? null : new HashSet<String>(Arrays.asList(ids.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      List<String> attrs = qParams.get(ATTR_QUERY_KEY);
      Set<String> tsaAttrs = attrs == null || attrs.isEmpty() ? null : new HashSet<String>(attrs);

      Set<String> productIDs = UriInfoUtils.extractProductIds(info);

      return monitoringService.getClientStatistics(clientIds, tsaAttrs, productIDs);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA statistics", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
