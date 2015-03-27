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
package com.terracotta.management.web.utils;

import com.tc.security.PwProviderUtil;
import com.terracotta.management.keychain.KeyName;
import com.terracotta.management.keychain.URIKeyName;
import com.terracotta.management.security.KeyChainAccessor;
import com.terracotta.management.security.SecretUtils;

import java.net.URI;

/**
 * @author Ludovic Orban
 */
public class L2KeyChainAccessor implements KeyChainAccessor {

  @Override
  public byte[] retrieveSecret(KeyName alias) {
    URIKeyName uriKeyName = (URIKeyName)alias;
    URI uri = uriKeyName.getURI();
    try {
      char[] passwordTo = PwProviderUtil.getPasswordTo(uri);
      return SecretUtils.toBytesAndWipe(passwordTo);
    } catch (NullPointerException npe) {
      // PwProviderUtil.getPasswordTo throws NPE when there is no such secret
      return null;
    }
  }

}
