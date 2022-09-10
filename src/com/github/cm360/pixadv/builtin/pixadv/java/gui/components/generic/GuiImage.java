package com.github.cm360.pixadv.builtin.pixadv.java.gui.components.generic;

import java.awt.Graphics;
import java.util.stream.Stream;

import com.github.cm360.pixadv.graphics.gui.BoundsMutator;
import com.github.cm360.pixadv.graphics.gui.GuiComponent;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.registry.Registry;

public class GuiImage extends GuiComponent {

	protected Identifier[] textures;
	
	public GuiImage(GuiComponent parent) {
		super(parent);
	}
	
	public GuiImage(GuiComponent parent, BoundsMutator boundsMutator) {
		super(parent, boundsMutator);
	}
	
	public void setTextures(Identifier[] newTextures) {
		textures = newTextures;
	}
	
	@Override
	protected void paintSelf(Graphics g, Registry registry) {
		Stream.of(textures).forEach(id -> drawTexture(g, registry, id));
	}
	
	protected void drawTexture(Graphics g, Registry registry, Identifier textureId) {
		g.drawImage(registry.getTexture(textureId),
				(int) Math.round(bounds.getX()),
				(int) Math.round(bounds.getY()),
				(int) Math.round(bounds.getWidth()),
				(int) Math.round(bounds.getHeight()),
				null);
	}

}
