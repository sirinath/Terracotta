package org.terracotta.lassen.elements;

import org.terracotta.lassen.elements.common.AbstractUserElement;
import org.terracotta.lassen.models.User;

import com.uwyn.rife.engine.annotations.Elem;
import com.uwyn.rife.engine.annotations.SubmissionBean;
import com.uwyn.rife.engine.annotations.SubmissionHandler;
import com.uwyn.rife.site.Validated;

@Elem
public class RegisterUser extends AbstractUserElement {
	public void processElement() {
		print(template);
	}
	
	@SubmissionHandler(beans = {@SubmissionBean(beanclass=User.class)})
	public void doRegister() {
		User user = getSubmissionBean(User.class);
		if (((Validated)user).validate()) {
			service.store(user);
			template.setBlock("content", "success");
		} else {
			generateForm(template, user);
		}
		print(template);
	}
}