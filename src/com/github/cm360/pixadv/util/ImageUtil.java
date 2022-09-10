package com.github.cm360.pixadv.util;

import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;

public class ImageUtil {

	public static BufferedImage applyBrightness(BufferedImage input, double percent) {
		BufferedImage output = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice()
				.getDefaultConfiguration()
				.createCompatibleImage(input.getWidth(), input.getHeight(), Transparency.TRANSLUCENT);
		WritableRaster inputRaster = input.getRaster(), outputRaster = output.getRaster();
		for (int y = 0; y < input.getHeight(); y++)
			for (int x = 0; x < input.getWidth(); x++) {
				int[] pixel = inputRaster.getPixel(x, y, new int[4]);
				for (int channel = 0; channel < 3; channel++) {
					pixel[channel] = (int) Math.round(pixel[channel] * percent);
					if (pixel[channel] < 0)
						pixel[channel] = 0;
					if (pixel[channel] > 255)
						pixel[channel] = 255;
				}
				outputRaster.setPixel(x, y, pixel);
			}
		return output;
	}

}
