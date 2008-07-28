package org.terracotta.lassen.security.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.stereotype.Service;
import org.terracotta.lassen.security.StandardAuthoritiesService;

@Service
public class DefaultStandardAuthoritiesService implements StandardAuthoritiesService {

  private final static Map<String, GrantedAuthority> STANDARD;
  
  static {
    STANDARD = new ConcurrentHashMap<String, GrantedAuthority>();
    
    createStandardAuthority(STUDENT_LITERAL);
    createStandardAuthority(ADMINISTRATOR_LITERAL);
  }
  
  private static GrantedAuthority createStandardAuthority(final String name) {
    if (null == name) {
      return null;
    }
    
    assert !STANDARD.containsKey(name);
    
    final GrantedAuthority auth = new GrantedAuthorityImpl(name);
    STANDARD.put(name, auth);
    return auth;
  }
  
  public GrantedAuthority getNameBasedAuthority(final String name) {
    final GrantedAuthority auth = STANDARD.get(name);
    if (null == auth) {
      return new GrantedAuthorityImpl(name);
    } else {
      return auth;
    }
  }
}