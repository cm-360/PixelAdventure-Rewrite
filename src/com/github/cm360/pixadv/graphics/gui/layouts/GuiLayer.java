package com.github.cm360.pixadv.graphics.gui.layouts;

import java.awt.Graphics;

import com.github.cm360.pixadv.graphics.gui.GuiComponent;
import com.github.cm360.pixadv.graphics.gui.input.InputProcessor;
import com.github.cm360.pixadv.registry.Registry;

public abstract class GuiLayer extends GuiComponent {

	public static double scale = 2.0;
	
	private InputProcessor inputProcessor;
	
	public GuiLayer() {
		super(null);
		inputProcessor = new InputProcessor(this);
	}
	
	@Override
	public void paint(Graphics g, Registry registry) {
		paintSelf(g, registry);
		for (GuiComponent child : children)
			child.paint(g, registry);
	}
	
	public InputProcessor getInputProcessor() {
		return inputProcessor;
	}

}
