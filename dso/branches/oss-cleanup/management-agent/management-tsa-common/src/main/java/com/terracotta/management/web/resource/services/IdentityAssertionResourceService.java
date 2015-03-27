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
package com.terracotta.management.web.resource.services;

import org.terracotta.management.ServiceLocator;
import org.terracotta.management.resource.exceptions.ResourceRuntimeException;

import com.terracotta.management.security.RequestIdentityAsserter;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * A security resource service that provides identity assertion for downstream consumers.
 *
 * @author brandony
 */
@Path("/assertIdentity")
public final class IdentityAssertionResourceService {
  private final RequestIdentityAsserter idAsserter;

  public IdentityAssertionResourceService() {
    this.idAsserter = ServiceLocator.locate(RequestIdentityAsserter.class);
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getIdentity(@Context HttpServletRequest request,
                              @Context HttpServletResponse response) {
    try {
      return Response.ok(idAsserter.assertIdentity(request, response)).build();
    } catch (Exception e) {
      throw new ResourceRuntimeException("Identity assertion failure", e, Response.Status.UNAUTHORIZED.getStatusCode());
    }
  }
}
