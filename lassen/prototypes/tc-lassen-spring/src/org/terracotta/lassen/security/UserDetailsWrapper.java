package org.terracotta.lassen.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.terracotta.lassen.models.User;

public class UserDetailsWrapper implements UserDetails {
  private final transient StandardAuthoritiesService standardAuthoritiesService;
  
  private final User delegate;
  
  public UserDetailsWrapper(final StandardAuthoritiesService service, final User delegate) {
    this.standardAuthoritiesService = service;
    this.delegate = delegate;
  }
  
  public GrantedAuthority[] getAuthorities() {
    final GrantedAuthority[] userAuthorities = new GrantedAuthority[delegate.getRoles().size()];
    int i = 0;
    for (final String role : delegate.getRoles()) {
      userAuthorities[i++] = standardAuthoritiesService.getNameBasedAuthority(role);
    }
    return userAuthorities;
  }

  public String getPassword() {
    return delegate.getPassword();
  }

  public String getUsername() {
    return delegate.getUserName();
  }

  public boolean isAccountNonExpired() {
    return true;
  }

  public boolean isAccountNonLocked() {
    return true;
  }

  public boolean isCredentialsNonExpired() {
    return true;
  }

  public boolean isEnabled() {
    return delegate.isConfirmed();
  }
}
