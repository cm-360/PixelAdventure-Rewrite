package com.github.cm360.pixadv.graphics.picasso;

import java.awt.Graphics;
import java.awt.Rectangle;

import com.github.cm360.pixadv.world.storage.world.World;

public class Precompute {

	private Rectangle gBounds;
	private Rectangle camBounds;
	private int centerX;
	private int centerY;
	private int minX;
	private int minY;
	private int maxX;
	private int maxY;
	private double scaledTileTextureSize;
	
	public Precompute(Graphics g) {
		gBounds = g.getClipBounds();
		camBounds = gBounds;
	}
	
	public void update(World world, Picasso picasso) {
		scaledTileTextureSize = picasso.getTileTextureSize() * picasso.getTileScale();
		centerX = (int) (camBounds.x + (camBounds.width / 2) - scaledTileTextureSize / 2);
		centerY = (int) (camBounds.y + (camBounds.height / 2) - scaledTileTextureSize / 2);
		minX = (int) Math.round(((world.getCameraX() * scaledTileTextureSize - camBounds.width / 2)) / scaledTileTextureSize - 0.05);
		minY = (int) Math.round(((world.getCameraY() * scaledTileTextureSize - camBounds.height / 2)) / scaledTileTextureSize - 0.05);
		maxX = (int) Math.round(((world.getCameraX() * scaledTileTextureSize + camBounds.width / 2)) / scaledTileTextureSize + 1.05);
		maxY = (int) Math.round(((world.getCameraY() * scaledTileTextureSize + camBounds.height / 2)) / scaledTileTextureSize + 1.05);
	}
	
	public void updateCamBounds(int border) {
		if (border > 0) {
			camBounds = new Rectangle(
					gBounds.x + border,
					gBounds.y + border,
					gBounds.width - 2 * border,
					gBounds.height - 2 * border);
		} else {
			camBounds = gBounds;
		}
	}
	
	public Rectangle getGBounds() {
		return gBounds;
	}

	public Rectangle getCamBounds() {
		return camBounds;
	}

	public int getCenterX() {
		return centerX;
	}

	public int getCenterY() {
		return centerY;
	}

	public int getMinX() {
		return minX;
	}

	public int getMinY() {
		return minY;
	}

	public int getMaxX() {
		return maxX;
	}

	public int getMaxY() {
		return maxY;
	}

	public double getScaledTileTextureSize() {
		return scaledTileTextureSize;
	}
	
}
