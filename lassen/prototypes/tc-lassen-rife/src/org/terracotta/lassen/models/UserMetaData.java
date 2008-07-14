package org.terracotta.lassen.models;

import com.uwyn.rife.site.ConstrainedBean;
import com.uwyn.rife.site.ConstrainedProperty;
import com.uwyn.rife.site.MetaData;

public class UserMetaData extends MetaData<ConstrainedBean, ConstrainedProperty> {
	@Override
	public void activateMetaData() {
		addConstraint(new ConstrainedProperty("id")
			.identifier(true)
			.editable(false));
		addConstraint(new ConstrainedProperty("username")
			.notNull(true)
			.minLength(5)
			.maxLength(8));
		addConstraint(new ConstrainedProperty("password")
			.notNull(true)
			.minLength(5)
			.maxLength(8));
		addConstraint(new ConstrainedProperty("email")
			.notNull(true)
			.email(true));
	}
}