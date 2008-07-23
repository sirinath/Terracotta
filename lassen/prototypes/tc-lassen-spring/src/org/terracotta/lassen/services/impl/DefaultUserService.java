package org.terracotta.lassen.services.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Service;
import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

@Service
public class DefaultUserService implements UserService {
  private final AtomicLong      ids   = new AtomicLong();
  private final Map<Long, User> users = new ConcurrentHashMap<Long, User>();

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

    for (User user : users.values()) {
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

    if (null == user.getId()) {
      user.setId(ids.incrementAndGet());
    }
    users.put(user.getId(), user);

    return true;
  }
}