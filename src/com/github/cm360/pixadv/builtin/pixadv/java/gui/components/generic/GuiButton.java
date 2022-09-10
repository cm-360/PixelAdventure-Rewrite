package com.github.cm360.pixadv.builtin.pixadv.java.gui.components.generic;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;

import com.github.cm360.pixadv.graphics.gui.BoundsMutator;
import com.github.cm360.pixadv.graphics.gui.GuiComponent;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiLayer;
import com.github.cm360.pixadv.registry.Registry;

public class GuiButton extends GuiImage {

	protected String buttonText;
	protected Font buttonFont;
	
	public GuiButton(GuiComponent parent, BoundsMutator boundsMutator, String text, Font font) {
		super(parent, boundsMutator);
		focusable = true;
		buttonText = text;
		buttonFont = font;
	}
	
	@Override
	protected void paintSelf(Graphics g, Registry registry) {
		// Paint image
		if (hovered) {
			super.drawTexture(g, registry, textures[1]);
		} else {
			super.drawTexture(g, registry, textures[0]);
		}
		// Paint text
		if (!buttonText.isEmpty()) {
			g.setFont(buttonFont.deriveFont((float) (buttonFont.getSize() * GuiLayer.scale)));
			FontMetrics fontMetrics = g.getFontMetrics();
			Point textBasePoint = new Point(
					(int) (bounds.getX() + (bounds.getWidth() - fontMetrics.stringWidth(buttonText)) * 0.5),
					(int) (bounds.getY() + (bounds.getHeight() + fontMetrics.getHeight() * 0.5) * 0.5)
				);
			g.setColor(Color.DARK_GRAY);
			int offset = (int) Math.ceil(1.5 * GuiLayer.scale);
			g.drawString(buttonText, textBasePoint.x + offset, textBasePoint.y + offset);
			g.setColor(Color.WHITE);
			g.drawString(buttonText, textBasePoint.x, textBasePoint.y);
		}
	}

}
