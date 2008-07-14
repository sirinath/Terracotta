package org.terracotta.lassen.elements.common;

import org.terracotta.lassen.services.UserService;

public abstract class AbstractUserElement extends AbstractTemplateElement {
	protected UserService service;
	public void setUserService(UserService service) { this.service = service; }
}