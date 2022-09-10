package com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities;

import java.awt.geom.Point2D;
import java.util.Set;

import com.github.cm360.pixadv.world.types.entities.Entity;

public interface RideableEntity extends Entity {

	public boolean mount(Entity entity);
	
	public boolean dismount(Entity entity);
	
	public Set<Entity> getPassengers();
	
	public Point2D.Double getRidePoint(Entity passenger);

}
