package com.github.cm360.pixadv.graphics.gui;

import java.awt.Graphics;
import java.awt.Point;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.HashMap;

import com.github.cm360.pixadv.graphics.gui.input.KeyCombo;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.sound.beethoven.Beethoven;

public abstract class GuiComponent {
	
	protected Rectangle2D bounds;
	protected BoundsMutator boundsMutator;
	protected boolean focusable;
	protected boolean hovered;
	protected GuiComponent parent;
	protected ArrayList<GuiComponent> children;
	
	protected HashMap<KeyCombo, Runnable> events;
	
	protected Beethoven soundManager;
	protected Identifier hoverSound;
	protected Identifier selectSound;
	
	public GuiComponent(GuiComponent parent) {
		this(parent, dummy -> dummy);
	}
	
	public GuiComponent(GuiComponent parent, BoundsMutator boundsMutator) {
		this.bounds = new Rectangle2D.Double();
		this.boundsMutator = boundsMutator;
		this.focusable = false;
		this.hovered = false;
		this.parent = parent;
		this.children = new ArrayList<GuiComponent>();
		this.events = new HashMap<KeyCombo, Runnable>();
		this.hoverSound = Identifier.parse("pixadv:sounds/ui/select/hover");
		this.selectSound = Identifier.parse("pixadv:sounds/ui/select/select");
	}
	
	// Rendering methods
	public void paint(Graphics g, Registry registry) {
		updateBounds(parent.getBounds());
		paintSelf(g, registry);
		for (GuiComponent child : children)
			child.paint(g, registry);
	}
	
	protected void paintSelf(Graphics g, Registry registry) {
		// Do nothing by default
	}
	
	public void updateBounds(Rectangle2D parentBounds) {
		bounds = boundsMutator.mutate(parentBounds);
	}
	
	public Rectangle2D getBounds() {
		return bounds;
	}
	
	public void setHovered(boolean hovered) {
		this.hovered = hovered;
	}
	
	public boolean isHovered() {
		return hovered;
	}
	
	// Interaction methods
	public void registerEvent(KeyCombo trigger, Runnable action) {
		events.put(trigger, action);
	}
	
	public boolean removeEvent(KeyCombo trigger) {
		return events.remove(trigger) != null;
	}
	
	public void interactClick(Point mousePos, long clickDuration, KeyCombo keys) {
		for (KeyCombo combo : events.keySet())
			if (keys.containsAll(combo))
				events.get(combo).run();
//		soundManager.playSound(selectSound);
	}

	public void interactHover(Point mousePos, KeyCombo keys) {

	}

	public void interactKey(KeyCombo keys) {

	}
	
	public GuiComponent attemptFocus(Point mousePos) {
		for (GuiComponent child : children) {
			GuiComponent result = child.attemptFocus(mousePos);
			if (result != null)
				return result;
		}
		return (focusable && getBounds().contains(mousePos)) ? this : null;
	}
	
	// Access methods
	public Identifier getHoverSoundId() {
		return hoverSound;
	}
	
	public Identifier getSelectSoundId() {
		return selectSound;
	}
	
	// Hierarchy methods
	public GuiComponent getParent() {
		return parent;
	}
	
	public ArrayList<GuiComponent> getChildren() {
		return children;
	}

}
