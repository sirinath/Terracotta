package org.terracotta.lassen.elements;

import org.terracotta.lassen.elements.common.AbstractUserElement;
import org.terracotta.lassen.models.User;

import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.InputProperty;
import com.uwyn.rife.engine.annotations.SubmissionBean;
import com.uwyn.rife.engine.annotations.SubmissionHandler;
import com.uwyn.rife.site.Validated;

@Elem
public class EditUser extends AbstractUserElement {
	private int id;
	@InputProperty
	public void setId(int id) { this.id = id; };
	
	public void processElement() {
		User user = service.findById(id);
		if (null == user) {
			exit("index");
		}
		generateForm(template, user);
		print(template);
	}
	
	@SubmissionHandler(beans = {@SubmissionBean(beanclass=User.class)})
	public void doEdit() {
		User user = getSubmissionBean(User.class);
		user.setId(id);
		if (((Validated)user).validate()) {
			service.store(user);
			template.setBlock("content", "success");
		} else {
			generateForm(template, user);
		}
		print(template);
	}
}