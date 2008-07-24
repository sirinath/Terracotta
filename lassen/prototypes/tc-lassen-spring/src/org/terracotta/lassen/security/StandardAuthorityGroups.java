package org.terracotta.lassen.security;

import org.springframework.security.GrantedAuthority;

public abstract class StandardAuthorityGroups {
  public final static GrantedAuthority[] DEFAULT = new GrantedAuthority[] { StandardAuthorities.STUDENT };
  public final static GrantedAuthority[] ADMINISTRATOR = new GrantedAuthority[] { StandardAuthorities.STUDENT, StandardAuthorities.ADMINISTRATOR };
}