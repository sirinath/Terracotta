package org.terracotta.lassen.services.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.dao.DataAccessException;
import org.springframework.security.userdetails.UserDetails;
import org.springframework.security.userdetails.UserDetailsService;
import org.springframework.security.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.security.StandardAuthoritiesService;
import org.terracotta.lassen.security.UserDetailsWrapper;
import org.terracotta.lassen.services.UserService;

@Service
public class DefaultUserService implements UserService, UserDetailsService {
  private final StandardAuthoritiesService standardAuthoritiesService;
  private final AtomicLong                 ids   = new AtomicLong();
  private final Map<Long, User>            users = new ConcurrentHashMap<Long, User>();

  public DefaultUserService(final StandardAuthoritiesService standardAuthoritiesService) {
    this.standardAuthoritiesService = standardAuthoritiesService;
  }

  public User findById(final Long id) {
    if (null == id) { return null; }
    return users.get(id);
  }

  public User findByEmail(final String email) {
    if (null == email) { return null; }

    for (User user : users.values()) {
      if (email.equals(user.getEmail())) { return user; }
    }

    return null;
  }

  public User findByUserName(final String userName) {
    if (null == userName) { return null; }

    for (final User user : users.values()) {
      if (userName.equals(user.getUserName())) { return user; }
    }

    return null;
  }

  public Collection<User> getAllUsers() {
    return users.values();
  }

  public boolean deleteById(final Long id) {
    return users.remove(id) != null;
  }

  public boolean store(final User user) {
    if (null == user) { return false; }

    // new user
    if (null == user.getId()) {
      user.setId(ids.incrementAndGet());

      // every new user always has the student role
      user.addRole(StandardAuthoritiesService.STUDENT);

      // if this is the first user, give him also administrator rights
      if (0 == users.size()) {
        user.addRole(StandardAuthoritiesService.ADMINISTRATOR);
      }
    }
    users.put(user.getId(), user);

    return true;
  }

  public UserDetails loadUserByUsername(final String userName) throws UsernameNotFoundException, DataAccessException {
    final User user = findByUserName(userName);
    if (null == user) { throw new UsernameNotFoundException(userName); }
    return new UserDetailsWrapper(standardAuthoritiesService, user);
  }
}