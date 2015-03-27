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

import org.apache.shiro.web.env.IniWebEnvironment;

import com.terracotta.management.web.utils.TSAConfig;

/**
 * @author Ludovic Orban
 */
public class TSAIniWebEnvironment extends IniWebEnvironment {

  private static final String NOIA_SECURE_INI_RESOURCE_PATH = "classpath:shiro-ssl-noIA.ini";
  private final static String SECURE_INI_RESOURCE_PATH = "classpath:shiro-ssl.ini";
  private final static String UNSECURE_INI_RESOURCE_PATH = "classpath:shiro.ini";

  @Override
  protected String[] getDefaultConfigLocations() {
    if (Boolean.getBoolean("com.terracotta.management.debug.noIA") && TSAConfig.isSslEnabled()) {
      return new String[] { NOIA_SECURE_INI_RESOURCE_PATH };
    } else if (TSAConfig.isSslEnabled()) {
      return new String[] { SECURE_INI_RESOURCE_PATH };
    } else {
      return new String[] { UNSECURE_INI_RESOURCE_PATH, };
    }
  }

}
