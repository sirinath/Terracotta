/*
 * All content copyright Terracotta, Inc., unless otherwise indicated. All rights reserved.
 */
package com.terracotta.management.service.impl;

import org.terracotta.management.ServiceExecutionException;

import com.terracotta.management.resource.LicenseEntity;
import com.terracotta.management.service.LicenseService;
import com.terracotta.management.service.TsaManagementClientService;

import java.util.Collection;
import java.util.Set;

/**
 * @author Hung Huynh
 */
public class LicenseServiceImpl implements LicenseService {

  private final TsaManagementClientService tsaManagementClientService;

  public LicenseServiceImpl(TsaManagementClientService tsaManagementClientService) {
    this.tsaManagementClientService = tsaManagementClientService;
  }


  @Override
  public Collection<LicenseEntity> getLicenseProperties(Set<String> serverNames) throws ServiceExecutionException {
    return tsaManagementClientService.getLicenseProperties(serverNames);
  }

}
