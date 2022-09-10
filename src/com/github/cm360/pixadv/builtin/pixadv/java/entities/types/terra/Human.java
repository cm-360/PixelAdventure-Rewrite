package com.github.cm360.pixadv.builtin.pixadv.java.entities.types.terra;

import java.awt.image.BufferedImage;
import java.util.Map;

import com.github.cm360.pixadv.world.types.entities.AbstractEntity;

public class Human extends AbstractEntity {

	public Human() {
		width = 0.8;
		height = 1.8;
		mass = 70;
		collidability = 0.5;
	}
	
	@Override
	public String getID() {
		return "terra/human";
	}

	@Override
	public String getDisplayName() {
		return "Human";
	}
	
	@Override
	public BufferedImage getTexture() {
		return null;
	}
	
	@Override
	public Map<String, String> getData() {
		// TODO Auto-generated method stub
		return null;
	}

}
