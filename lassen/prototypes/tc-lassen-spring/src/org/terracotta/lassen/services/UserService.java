package org.terracotta.lassen.services;

import java.util.Collection;

import javax.annotation.security.RolesAllowed;

import org.terracotta.lassen.models.User;
import org.terracotta.lassen.security.StandardAuthoritiesService;

public interface UserService {
  public User findById(Long id);

  public User findByEmail(String email);

  public User findByUserName(String userName);

  @RolesAllowed(StandardAuthoritiesService.ADMINISTRATOR)
  public Collection<User> getAllUsers();

  public boolean store(User user);

  @RolesAllowed(StandardAuthoritiesService.ADMINISTRATOR)
  public boolean deleteById(Long id);
}