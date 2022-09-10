package com.github.cm360.pixadv.graphics.picasso;

import java.awt.Point;

public class HashablePoint extends Point {

	private static final long serialVersionUID = -1761668980804904928L;

	public HashablePoint(Point p) {
		super(p);
	}
	
	public HashablePoint(int x, int y) {
		super(x, y);
	}
	
	@Override
	public int hashCode() {
		return toString().hashCode();
	}

}
