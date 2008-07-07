package org.terracotta.lassen.actions;

import org.terracotta.lassen.models.User;
import org.terracotta.lassen.services.UserService;

import com.google.inject.Inject;
import com.opensymphony.xwork2.ActionSupport;
import com.opensymphony.xwork2.ModelDriven;
import com.opensymphony.xwork2.Preparable;

public class EditUserAction extends ActionSupport implements ModelDriven<User>, Preparable {
	
	@Inject
	private UserService service;

	private int id;
	private User user;
	
	public void setId(int id) {
		this.id = id;
	}

	public User getModel() {
		return user;
	}

	public void prepare() throws Exception {
		user = service.findById(id);
	}
	
	public String execute() {
		if (service.store(user)) {
			return SUCCESS;
		} else {
			return INPUT;
		}
	}
}