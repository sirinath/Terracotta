package org.terracotta.lassen.security.impl;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;
import org.springframework.stereotype.Service;
import org.terracotta.lassen.security.StandardAuthoritiesService;

@Service
public class DefaultStandardAuthoritiesService implements StandardAuthoritiesService {

  private final Map<String, GrantedAuthority> standard = new ConcurrentHashMap<String, GrantedAuthority>();
  
  public DefaultStandardAuthoritiesService() {
    createStandardAuthority(STUDENT);
    createStandardAuthority(ADMINISTRATOR);
  }
  
  private GrantedAuthority createStandardAuthority(final String name) {
    if (null == name) {
      return null;
    }
    
    assert !standard.containsKey(name);
    
    final GrantedAuthority auth = new GrantedAuthorityImpl(name);
    standard.put(name, auth);
    return auth;
  }
  
  public GrantedAuthority getNameBasedAuthority(final String name) {
    final GrantedAuthority auth = standard.get(name);
    if (null == auth) {
      return new GrantedAuthorityImpl(name);
    } else {
      return auth;
    }
  }
}