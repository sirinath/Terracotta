/*
 * All content copyright (c) 2003-2007 Terracotta, Inc., except as may otherwise be noted in a separate copyright
 * notice. All rights reserved.
 */
package org.terracotta.ui.util;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.LayoutConstants;
import org.eclipse.jface.util.Geometry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Shell;

public final class SWTUtil {

  private SWTUtil() {
    // cannot instantiate
  }

  public static void applyDefaultButtonSize(Button button) {
    Point preferredSize = button.computeSize(SWT.DEFAULT, SWT.DEFAULT, false);
    Point hint = Geometry.max(LayoutConstants.getMinButtonSize(), preferredSize);
    GridDataFactory.fillDefaults().align(SWT.FILL, SWT.CENTER).hint(hint).applyTo(button);
  }
  
  public static void placeDialogInCenter(Shell parent, Shell shell){
    Rectangle parentSize = parent.getBounds();
    Rectangle mySize = shell.getBounds();
    int locationX, locationY;
    locationX = (parentSize.width - mySize.width)/2+parentSize.x;
    locationY = (parentSize.height - mySize.height)/2+parentSize.y;
    shell.setLocation(new Point(locationX, locationY));
  }
}
