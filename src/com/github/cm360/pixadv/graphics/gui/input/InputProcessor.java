package com.github.cm360.pixadv.graphics.gui.input;

import java.awt.Point;

import com.github.cm360.pixadv.graphics.gui.GuiComponent;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiLayer;

public class InputProcessor {

	private GuiLayer parent;
	private GuiComponent focusedComponent;
	
	private long clickOriginTime = -1;
	private Point clickOriginPoint = new Point();
	
	private boolean dragging = false;
	private boolean pressed = false;
	
	// Constructor
	public InputProcessor(GuiLayer parentLayout) {
		parent = parentLayout;
	}
	
	// Mouse methods
	public void mousePressed(Point mousePos, KeyCombo keys) {
		// Save current mouse info
		pressed = true;
		clickOriginTime = System.currentTimeMillis();
		clickOriginPoint = mousePos;
		// Attempt to focus children components
		GuiComponent result = parent.attemptFocus(mousePos);
		if (result != null) {
			// Save name of now focused child
			focusedComponent = result;
		} else {
			// Nothing was focused, unfocus all
			focusedComponent = null;
		}
	}
	
	public void mouseReleased(Point mousePos, KeyCombo keys) {
		if (focusedComponent != null) {
			// Check for clicks
			long difference = clickOriginTime - System.currentTimeMillis();
			// TODO separate long and short clicks
			if (difference > 0 && difference < 1000) {
				// Normal click
				focusedComponent.interactClick(mousePos, difference, keys);
			} else {
				// Long click
				focusedComponent.interactClick(mousePos, difference, keys);
			}
			// Reset mouse state
			dragging = false;
			pressed = false;
		}
	}
	
	public void mouseMoved(Point mousePos, KeyCombo keys) {
		if (dragging || pressed) {
			// Dragging focused component
			dragging = true;
			
		}
	}
	
	// TODO Keyboard methods
	
	// Access methods
	public GuiComponent getFocusedComponent() {
		return focusedComponent;
	}

}
