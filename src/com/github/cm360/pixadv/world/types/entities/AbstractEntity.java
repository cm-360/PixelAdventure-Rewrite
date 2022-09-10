package com.github.cm360.pixadv.world.types.entities;

public abstract class AbstractEntity implements Entity {

	protected double width = 1;
	protected double height = 1;
	protected double mass;
	protected double collidability = 0.5;
	protected double x;
	protected double y;
	protected double xVel;
	protected double yVel;
	protected double xAccel;
	protected double yAccel;
	protected boolean noClip;
	protected boolean gravityAffected = true;
	protected boolean onGround = false;
	protected double lastGroundTime;

	@Override
	public double getWidth() {
		return width;
	}

	@Override
	public double getHeight() {
		return height;
	}
	
	@Override
	public double getMass() {
		return mass;
	}
	
	@Override
	public double getCollidability() {
		return collidability;
	}

	@Override
	public double getX() {
		return x;
	}

	@Override
	public void setX(double x) {
		this.x = x;
	}

	@Override
	public double getY() {
		return y;
	}

	@Override
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public double getXVel() {
		return xVel;
	}

	@Override
	public void setXVel(double xVel) {
		this.xVel = xVel;
	}

	@Override
	public double getYVel() {
		return yVel;
	}

	@Override
	public void setYVel(double yVel) {
		this.yVel = yVel;
	}
	
	@Override
	public double getXAccel() {
		return xAccel;
	}

	@Override
	public void setXAccel(double xAccel) {
		this.xAccel = xAccel;
	}

	@Override
	public double getYAccel() {
		return yAccel;
	}

	@Override
	public void setYAccel(double yAccel) {
		this.yAccel = yAccel;
	}
	
	@Override
	public boolean canNoClip() {
		return noClip;
	}
	
	@Override
	public void setNoClip(boolean noClip) {
		this.noClip = noClip;
	}
	
	@Override
	public boolean isGravityAffected() {
		return gravityAffected;
	}
	
	@Override
	public void setOnGround(boolean onGround) {
		this.onGround = onGround;
		if (onGround)
			lastGroundTime = System.nanoTime();
	}
	
	@Override
	public boolean isOnGround() {
		return onGround;
	}

}
