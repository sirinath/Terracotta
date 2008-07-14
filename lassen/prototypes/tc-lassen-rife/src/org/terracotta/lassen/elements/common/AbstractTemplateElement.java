package org.terracotta.lassen.elements.common;

import com.uwyn.rife.engine.Element;
import com.uwyn.rife.template.Template;

public abstract class AbstractTemplateElement extends Element {
	protected Template template;
	public void setTemplate(Template template) { this.template = template; }
}