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

import com.terracotta.management.security.InvalidIAInteractionException;
import com.terracotta.management.security.RequestIdentityAsserter;
import com.terracotta.management.user.UserInfo;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @author Ludovic Orban
 */
public final class NullIdentityAsserter implements RequestIdentityAsserter {

  @Override
  public UserInfo assertIdentity(HttpServletRequest request,
                                 HttpServletResponse response) throws InvalidIAInteractionException {
      throw new InvalidIAInteractionException(
          String.format("IA request received from host '%s' while security is disabled.", request.getRemoteAddr()));
  }

}
