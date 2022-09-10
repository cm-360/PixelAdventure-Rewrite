package com.github.cm360.pixadv.graphics.gui;

import java.awt.geom.Rectangle2D;

@FunctionalInterface
public interface BoundsMutator {

	public Rectangle2D mutate(Rectangle2D parentBounds);

}
