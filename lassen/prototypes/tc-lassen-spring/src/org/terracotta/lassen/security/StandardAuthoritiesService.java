package org.terracotta.lassen.security;

import org.springframework.security.GrantedAuthority;

public interface StandardAuthoritiesService {
  public final static String STUDENT_LITERAL = "ROLE_STUDENT";
  public final static String ADMINISTRATOR_LITERAL = "ROLE_ADMINISTRATOR";
  
  public GrantedAuthority getNameBasedAuthority(String name);
}