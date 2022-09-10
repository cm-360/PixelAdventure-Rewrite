package com.github.cm360.pixadv.world.newton;

import com.github.cm360.pixadv.world.newton.Newton.CollisionSide;

public class CollisionResult {

	private CollisionSide side;
	private double collidability;
	private double frictionConstant;

	/**
	 * @param side
	 * @param collidability
	 * @param frictionConstant
	 */
	public CollisionResult(CollisionSide side, double collidability, double frictionConstant) {
		this.side = side;
		this.collidability = collidability;
		this.frictionConstant = frictionConstant;
	}

	public CollisionSide getSide() {
		return side;
	}

	public double getCollidability() {
		return collidability;
	}

	public double getFrictionConstant() {
		return frictionConstant;
	}

}
