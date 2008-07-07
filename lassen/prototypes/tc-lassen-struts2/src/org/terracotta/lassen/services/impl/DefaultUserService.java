package org.terracotta.lassen.services.impl;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

import com.google.inject.Singleton;

@Singleton
public class DefaultUserService implements UserService {
	private AtomicInteger ids = new AtomicInteger();
	private Map<Integer, User> users = new ConcurrentHashMap<Integer, User>();
	
	public User findById(int id) {
		return users.get(id);
	}

	public Collection<User> getAllUsers() {
		return users.values();
	}

	public boolean deleteById(int id) {
		return users.remove(id) != null;
	}

	public boolean store(User user) {
		if (null == user) {
			return false;
		}
		
		if (0 == user.getId()) {
			user.setId(ids.incrementAndGet());
		}
		users.put(user.getId(), user);
		
		return true;
	}
}