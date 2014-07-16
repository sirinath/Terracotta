/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.resource.services;

import com.terracotta.management.resource.LicenseEntity;

import java.util.Collection;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

/**
 * A resource service for querying TSA topologies.
 * 
 * @author Hung Huynh
 */
public interface LicenseResourceService {

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Collection<LicenseEntity> getLicenseProperties(@Context
  UriInfo info);

}
