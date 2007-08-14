/*
@COPYRIGHT@
 */
package demo.sharededitor.models;

import java.awt.Graphics2D;
import java.awt.Rectangle;

final class Image extends Square {

	public void draw(Graphics2D g, boolean showAnchors) {
		Rectangle bounds = getShape().getBounds();
		java.awt.Image img = (java.awt.Image) getTexture();
		if (img != null) {
			g.drawImage(img, bounds.x, bounds.y, bounds.width, bounds.height,
					null);
		}
		super.draw(g, showAnchors);
	}

}
