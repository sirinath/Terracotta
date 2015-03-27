/* 
 * The contents of this file are subject to the Terracotta Public License Version
 * 2.0 (the "License"); You may not use this file except in compliance with the
 * License. You may obtain a copy of the License at 
 *
 *      http://terracotta.org/legal/terracotta-public-license.
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License for
 * the specific language governing rights and limitations under the License.
 *
 * The Covered Software is Terracotta Platform.
 *
 * The Initial Developer of the Covered Software is 
 *      Terracotta, Inc., a Software AG company
 */
package com.terracotta.management.security.impl;

import com.terracotta.management.security.ContextService;
import com.terracotta.management.security.IACredentials;
import com.terracotta.management.security.InvalidIAInteractionException;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.SSLContextFactory;
import com.terracotta.management.security.SecurityServiceDirectory;
import com.terracotta.management.user.UserInfo;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * @author Ludovic Orban
 */
public class RelayingJerseyIdentityAssertionServiceClient extends JerseyIdentityAssertionServiceClient {
  private final ContextService contextService;

  public RelayingJerseyIdentityAssertionServiceClient(KeyChainAccessor keyChainAccessor, SSLContextFactory sslCtxtFactory,
                                                      SecurityServiceDirectory securityServiceDirectory,
                                                      ContextService contextService) throws URISyntaxException, MalformedURLException {
    super(keyChainAccessor, sslCtxtFactory, securityServiceDirectory);
    this.contextService = contextService;
  }

  @Override
  public UserInfo retreiveUserDetail(IACredentials credentials) throws InvalidIAInteractionException {
    UserInfo userInfo = super.retreiveUserDetail(credentials);

    contextService.putUserInfo(userInfo);
    return userInfo;
  }
}
