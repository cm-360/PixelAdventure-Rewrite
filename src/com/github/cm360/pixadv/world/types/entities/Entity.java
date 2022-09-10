package com.github.cm360.pixadv.world.types.entities;

import java.awt.image.BufferedImage;
import java.util.Map;

public interface Entity {

	public String getID();
	
	public String getDisplayName();
	
	public BufferedImage getTexture();
	
	public Map<String, String> getData();
	
	public double getWidth();
	
	public double getHeight();
	
	public double getMass();
	
	public double getCollidability();
	
	public double getX();
	
	public void setX(double x);
	
	public double getY();
	
	public void setY(double y);
	
	public double getXVel();
	
	public void setXVel(double xVel);
	
	public double getYVel();
	
	public void setYVel(double yVel);
	
	public double getXAccel();
	
	public void setXAccel(double xAccel);
	
	public double getYAccel();
	
	public void setYAccel(double yAccel);
	
	public boolean canNoClip();
	
	public void setNoClip(boolean noClip);
	
	public boolean isGravityAffected();

	public void setOnGround(boolean onGround);
	
	public boolean isOnGround();

}
