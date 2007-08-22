/*
 @COPYRIGHT@
 */
package demo.sharededitor.ui;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;

import javax.swing.JComponent;

import demo.sharededitor.events.ListListener;
import demo.sharededitor.models.BaseObject;
import demo.sharededitor.models.ObjectManager;

public final class Renderer extends JComponent implements ListListener {
	public static final long serialVersionUID = 0;

	public Renderer() {
		objmgr = null;
	}

	private ObjectManager objmgr;

	public void changed(Object source, Object obj) {
		this.objmgr = (ObjectManager) source;
		this.repaint();
	}

	public void paint(Graphics g) {
		Graphics2D g2 = (Graphics2D) g;
		g2.setBackground(Color.WHITE);
		g2.clearRect(0, 0, getSize().width, getSize().height);

		if (objmgr == null) {
			return;
		}

		BaseObject[] objects = objmgr.list();
		for (int i = 0; i < objects.length; i++) {
			BaseObject obj = objects[i];
			obj.draw(g2, objmgr.isGrabbed(obj));
		}

		g2.setColor(Color.DARK_GRAY);
		g2.drawRect(0, 0, getSize().width - 1, getSize().height - 1);
	}
}
