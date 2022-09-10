package com.github.cm360.pixadv.graphics.gui.input;

import java.util.Set;

public class KeyCombo {

	private int mouseButton;
	private Set<Integer> keys;
	
	// Constructor
	public KeyCombo(int mouseButton) {
		this(mouseButton, Set.of());
	}
	
	public KeyCombo(int mouseButton, Set<Integer> pressedKeys) {
		this.mouseButton = mouseButton;
		this.keys = pressedKeys;
	}

	// Access methods
	public int getMouse() {
		return mouseButton;
	}
	
	public Integer[] getKeys() {
		return keys.toArray(new Integer[keys.size()]);
	}
	
	// Comparison methods
	public boolean equals(KeyCombo otherCombo) {
		return keys.size() == otherCombo.keys.size() && containsAll(otherCombo);
	}
	
	public boolean containsAll(KeyCombo otherCombo) {
		return mouseButton == otherCombo.mouseButton && keys.containsAll(otherCombo.keys);
	}

}
