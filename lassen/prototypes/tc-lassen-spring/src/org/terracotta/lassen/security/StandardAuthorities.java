package org.terracotta.lassen.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.GrantedAuthorityImpl;

public abstract class StandardAuthorities {
  public final static String STUDENT_LITERAL = "STUDENT";
  public final static GrantedAuthority STUDENT = new GrantedAuthorityImpl(STUDENT_LITERAL);
  public final static String ADMINISTRATOR_LITERAL = "ADMINISTRATOR";
  public final static GrantedAuthority ADMINISTRATOR = new GrantedAuthorityImpl(ADMINISTRATOR_LITERAL);
}