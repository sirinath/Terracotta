package com.tc.net.core.security;

import java.security.Principal;
import java.util.Set;

/**
 * @author Alex Snaps
 */
public interface TCPrincipal extends Principal {

  public Set<?> getRoles();
}
