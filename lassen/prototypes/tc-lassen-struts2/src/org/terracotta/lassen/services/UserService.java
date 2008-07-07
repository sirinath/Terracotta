package org.terracotta.lassen.services;

import java.util.Collection;

import org.terracotta.lassen.models.User;

public interface UserService {
	public User findById(int id);
	public Collection<User> getAllUsers();
	public boolean store(User user);
	public boolean deleteById(int id);
}