package org.terracotta.lassen.elements;

import org.terracotta.lassen.elements.common.AbstractTemplateElement;

import com.uwyn.rife.engine.annotations.Elem;

@Elem
public class Welcome extends AbstractTemplateElement {
	public void processElement() {
		print(template);
	}
}