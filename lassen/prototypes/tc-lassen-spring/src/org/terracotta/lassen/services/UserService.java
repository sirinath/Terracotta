package org.terracotta.lassen.services;

import java.util.Collection;

import org.terracotta.lassen.models.User;

public interface UserService {
  public User findById(Long id);

  public User findByEmail(String email);

  public User findByUserName(String userName);

  public Collection<User> getAllUsers();

  public boolean store(User user);

  public boolean deleteById(Long id);
}