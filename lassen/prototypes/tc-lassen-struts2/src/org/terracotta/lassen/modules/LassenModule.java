package org.terracotta.lassen.modules;

import org.terracotta.lassen.services.UserService;
import org.terracotta.lassen.services.impl.DefaultUserService;

import com.google.inject.AbstractModule;

public class LassenModule extends AbstractModule {
	public void configure() {
		bind(UserService.class).to(DefaultUserService.class);
	}
}