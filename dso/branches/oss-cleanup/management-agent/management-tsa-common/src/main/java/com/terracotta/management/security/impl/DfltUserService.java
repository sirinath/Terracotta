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

import com.terracotta.management.security.UserService;
import com.terracotta.management.user.UserInfo;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Ludovic Orban
 */
public class DfltUserService implements UserService {

  private final Map<String, UserInfo> userInfoMap = new ConcurrentHashMap<String, UserInfo>();

  @Override
  public UserInfo getUserInfo(String token) {
    return userInfoMap.remove(token);
  }

  @Override
  public String putUserInfo(UserInfo userInfo) {
    String token = UUID.randomUUID().toString();
    userInfoMap.put(token, userInfo);
    return token;
  }

}
