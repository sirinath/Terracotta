/*
 * Copyright Terracotta, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.terracotta.management.web.shiro;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.RequestTicketMonitor;
import com.terracotta.management.security.SecurityContextService;
import com.terracotta.management.security.UserService;
import com.terracotta.management.security.impl.DfltSecurityContextService;
import com.terracotta.management.security.impl.NullContextService;
import com.terracotta.management.security.impl.NullRequestTicketMonitor;
import com.terracotta.management.security.impl.NullUserService;
import com.terracotta.management.service.TimeoutService;
import com.terracotta.management.service.impl.util.LocalManagementSource;
import com.terracotta.management.service.impl.util.RemoteManagementSource;

import java.util.Collections;
import java.util.List;

/**
 * @author Ludovic Orban
 */
public class SecuritySetup {

  public ContextService getContextService() {
    return new NullContextService();
  }

  public RemoteManagementSource buildRemoteManagementSource(LocalManagementSource localManagementSource, TimeoutService timeoutService) {
    return new RemoteManagementSource(localManagementSource, timeoutService);
  }

  public RequestTicketMonitor getRequestTicketMonitor() {
    return new NullRequestTicketMonitor();
  }

  public UserService getUserService() {
    return new NullUserService();
  }

  public SecurityContextService getSecurityContextService() {
    return new DfltSecurityContextService();
  }

  public List<String> performSecurityChecks() {
    return Collections.emptyList();
  }
}
