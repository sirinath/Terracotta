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

import com.terracotta.management.resource.MBeanEntity;
import com.terracotta.management.resource.services.validator.TSARequestValidator;
import com.terracotta.management.service.JmxService;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

/**
 * @author Ludovic Orban
 */
@Path("/agents/jmx")
public class JmxResourceServiceImpl implements JmxResourceService {

  private static final Logger LOG = LoggerFactory.getLogger(JmxResourceServiceImpl.class);

  private final JmxService jmxService;
  private final RequestValidator requestValidator;

  public JmxResourceServiceImpl() {
    this.jmxService = ServiceLocator.locate(JmxService.class);
    this.requestValidator = ServiceLocator.locate(TSARequestValidator.class);
  }

  @Override
  public Collection<MBeanEntity> queryMBeans(UriInfo info) {
    LOG.debug(String.format("Invoking JmxResourceServiceImpl.queryMBeans: %s", info.getRequestUri()));

    requestValidator.validateSafe(info);

    try {
      String names = info.getPathSegments().get(1).getMatrixParameters().getFirst("names");
      Set<String> serverNames = names == null ? null : new HashSet<String>(Arrays.asList(names.split(",")));

      MultivaluedMap<String, String> qParams = info.getQueryParameters();
      String query = qParams.getFirst(ATTR_QUERY);

      return jmxService.queryMBeans(serverNames, query);
    } catch (ServiceExecutionException see) {
      throw new ResourceRuntimeException("Failed to get TSA MBeans", see, Response.Status.BAD_REQUEST.getStatusCode());
    }
  }

}
