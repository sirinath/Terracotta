package org.terracotta.lassen.elements;

import org.terracotta.lassen.elements.common.AbstractUserElement;
import org.terracotta.lassen.models.User;

import com.uwyn.rife.engine.annotations.AutolinkExitField;
import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.OutputProperty;

@Elem
public class ListUsers extends AbstractUserElement {
	@AutolinkExitField(destClass=EditUser.class)
	public final static String EDIT_EXIT = "edit";
	
	private int id;
	@OutputProperty
	public int getId() { return id; }
	
	public void processElement() {
		for (User user : service.getAllUsers()) {
			id = user.getId();
			setExitQuery(template, EDIT_EXIT);
			template.setBean(user);
			template.appendBlock("users", "user");
		}
		print(template);
	}
}