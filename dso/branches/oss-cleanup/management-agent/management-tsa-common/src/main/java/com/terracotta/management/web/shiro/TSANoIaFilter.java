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
package com.terracotta.management.web.shiro;

import org.terracotta.management.embedded.NoIaFilter;

import com.terracotta.management.web.utils.TSAConfig;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * A servlet filter that prevents secure requests from being processed by unsecure agents. On secure agents,
 * it's a no-op.
 *
 * @author Ludovic Orban
 */
public class TSANoIaFilter extends NoIaFilter {

  public static final boolean SSL_ENABLED = TSAConfig.isSslEnabled();

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    if (!SSL_ENABLED) {
      super.doFilter(request, response, chain);
    } else {
      chain.doFilter(request, response);
    }
  }

}
