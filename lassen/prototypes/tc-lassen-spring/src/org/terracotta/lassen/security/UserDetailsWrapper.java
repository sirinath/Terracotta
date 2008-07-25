package org.terracotta.lassen.security;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;
import org.terracotta.lassen.models.User;

public class UserDetailsWrapper implements UserDetails {
  private final User delegate;
  
  public UserDetailsWrapper(final User delegate) {
    this.delegate = delegate;
  }
  
  public GrantedAuthority[] getAuthorities() {
    // todo : properly adapt this once roles are fully supported
    return StandardAuthorityGroups.DEFAULT;
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
