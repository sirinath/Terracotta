package org.terracotta.lassen.actions;

import java.util.Collection;

import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

import com.google.inject.Inject;
import com.opensymphony.xwork2.ActionSupport;

public class ListUsersAction extends ActionSupport {
	
	@Inject
	private UserService service;

	private Collection<User> users;
	
	public Collection<User> getUsers() {
		return users;
	}

	public String execute() {
		users = service.getAllUsers();
		return SUCCESS;
	}
}