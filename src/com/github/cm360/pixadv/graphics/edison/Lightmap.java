package com.github.cm360.pixadv.graphics.edison;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class Lightmap {

	protected BufferedImage rawMap;
	protected BufferedImage scaledMap;
	
	protected Lightmap(int size, int scale, double[][] intensities, Color[][] colors) {
		// Create raw map
		rawMap = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
		WritableRaster raster = rawMap.getRaster();
		for (int x = 0; x < size; x++) {
			for (int y = 0; y < size; y++) {
				// Flip Y-axis
				int pixelY = size - (y + 1);
				double intensity = intensities[x][y];
				Color color = colors[x][y];
				if (intensity >= 0) {
					if (color != null) {
						// y must be inverted
						if (intensity != 0) {
							System.nanoTime();
						}
						raster.setPixel(x, pixelY, new int[] {
								(int) (color.getRed() * intensity),
								(int) (color.getGreen() * intensity),
								(int) (color.getBlue() * intensity),
								(int) (255 * (1 - intensity))});
					}
				} else {
					raster.setPixel(x, pixelY, new int[] {0, 0, 0, 0});
				}
			}
		}
		// Create scaled map
		scaledMap = new BufferedImage(size * scale, size * scale, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = scaledMap.createGraphics();
		RenderingHints renderHints = new RenderingHints(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
		renderHints.put(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
		new AffineTransformOp(AffineTransform.getScaleInstance(scale, scale),
				renderHints)
		.filter(rawMap, scaledMap);
	}
	
	public BufferedImage getRawMap() {
		return rawMap;
	}
	
	public BufferedImage getScaledMap() {
		return scaledMap;
	}

}
