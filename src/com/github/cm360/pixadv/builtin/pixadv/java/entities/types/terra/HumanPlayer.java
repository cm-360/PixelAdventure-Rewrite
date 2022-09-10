package com.github.cm360.pixadv.builtin.pixadv.java.entities.types.terra;

import java.awt.image.BufferedImage;
import java.util.Set;

import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.FlyingEntity;
import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.PlayerEntity;

public class HumanPlayer extends Human implements PlayerEntity, FlyingEntity {

	protected Set<Input> inputs;
	protected BufferedImage skin;
	protected double walkAccel;
	protected double walkSpeed;
	protected double jumpAccel;
	protected boolean flying;
	protected double flightAccel;
	protected double flightSpeed;
	
	public HumanPlayer(BufferedImage skin) {
		inputs = Set.of();
		this.skin = skin;
		flying = true;
		// Movement stats
		walkAccel = 10;
		walkSpeed = 5;
		jumpAccel = 200;
		flightAccel = 30;
		flightSpeed = 10;
	}
	
	@Override
	public BufferedImage getTexture() {
		return skin;
	}
	
	@Override
	public double getXAccel() {
		double accel = 0;
		if (flying) {
			if (inputs.contains(Input.LEFT))
				accel -= getBoundedAccel(Math.max(0, -xVel));
			if (inputs.contains(Input.RIGHT))
				accel += getBoundedAccel(Math.max(0, xVel));
		} else {
			if (inputs.contains(Input.LEFT))
				accel -= walkAccel;
			if (inputs.contains(Input.RIGHT))
				accel += walkAccel;
		}
		return xAccel + accel;
	}
	
	@Override
	public double getYAccel() {
		double accel = 0;
		if (flying) {
			if (inputs.contains(Input.UP))
				accel += getBoundedAccel(Math.max(0, yVel));
			if (inputs.contains(Input.DOWN))
				accel -= getBoundedAccel(Math.max(0, -yVel));
		} else {
			if (inputs.contains(Input.JUMP) && (onGround || (System.nanoTime() - lastGroundTime) < 120000000))
				accel += jumpAccel;
		}
		return yAccel + accel;
	}
	
	protected double getBoundedAccel(double vel) {
		double accel = flightAccel * (2 - (vel / flightSpeed));
		if (accel < 0.1)
			return 0;
		else
			return accel;
	}
	
	@Override
	public void setInputs(Set<Input> inputs) {
		this.inputs = inputs;
	}
	
	@Override
	public boolean isGravityAffected() {
		return !flying;
	}

	@Override
	public void setFlying(boolean flying) {
		this.flying = flying;
	}

	@Override
	public boolean isFlying() {
		return flying;
	}
	
	@Override
	public void setFlightSpeed(double flightSpeed) {
		this.flightSpeed = flightSpeed;
	}
	
	@Override
	public double getFlightSpeed() {
		return flightSpeed;
	}
	
	@Override
	public double getFlightDecayX() {
		return (inputs.contains(Input.LEFT) || inputs.contains(Input.RIGHT)) ? 1 : 0.025;
	}
	
	@Override
	public double getFlightDecayY() {
		return (inputs.contains(Input.UP) || inputs.contains(Input.DOWN)) ? 1 : 0.025;
	}

}
