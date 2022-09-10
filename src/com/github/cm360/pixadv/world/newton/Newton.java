package com.github.cm360.pixadv.world.newton;

import java.awt.Point;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.ControllableEntity;
import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.RideableEntity;
import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.ControllableEntity.Input;
import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.FlyingEntity;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;

/**
 * The Class Newton, an engine to handle physics for each world.
 */
public class Newton {

	/**
	 * Represents the different sides tile collisions can happen from.
	 */
	public enum CollisionSide {
		TOP, BOTTOM, LEFT, RIGHT
	};
	
	/** The world this engine handles physics for. */
	protected World world;
	
	/** When this engine's last tick occurred, in nano time. */
	protected long lastTick;
	
	/** The UUIDs of all currently controlled entities. */
	protected Set<UUID> controlledIds;
	
	/** The movement directions currently input by the user. */
	protected Set<Input> inputDirections;
	
	/** The mouse's last position on screen, in screen coordinates. */
	protected Point mousePos;
	
	/**
	 * Instantiates a new Newton physics engine.
	 *
	 * @param world The world to process.
	 */
	public Newton(World world) {
		this.world = world;
		this.controlledIds = Set.of();
		this.inputDirections = Set.of();
		this.mousePos = new Point();
	}
	
	/**
	 * Tick the physics engine.
	 *
	 * @return The duration of this tick in nanoseconds.
	 */
	public long tick() {
		long currentTick = System.nanoTime();
		long tickDuration = currentTick - lastTick;
		if (lastTick > 0) {
			// Time elapsed since last tick in seconds
			double elapsedSeconds = tickDuration / 1000000000.0;
			world.getEntities().forEach((uuid, entity) -> {
				synchronized (entity) {
					// Update inputs for controllable entities
					if (controlledIds.contains(uuid) && entity instanceof ControllableEntity) {
						updateControllable((ControllableEntity) entity);
					}
					// Update motion values
					updatePosition(entity, elapsedSeconds);
					updateVelocity(entity, elapsedSeconds);
					updateAcceleration(entity);
					// Apply collision forces caused by other entities
					processEntityCollisions(entity);
				}
			});
		}
		this.lastTick = currentTick;
		// TODO return correct tick time, not time since last tick
		return tickDuration;
	}
	
	/**
	 * Update directional inputs for a controllable entity.
	 *
	 * @param entity The entity to update.
	 */
	private void updateControllable(ControllableEntity entity) {
		entity.setInputs(inputDirections);
		// Update passengers positions if rideable
		if (entity instanceof RideableEntity) {
			RideableEntity rideableEntity = (RideableEntity) entity;
			for (Entity passenger : rideableEntity.getPassengers()) {
				Point2D.Double ridePoint = rideableEntity.getRidePoint(passenger);
				passenger.setX(ridePoint.x);
				passenger.setY(ridePoint.y);
			}
		}
	}
	
	/**
	 * Update an entity's position. This calculates delta values using kinematics,
	 * steps it's position to check for tile collisions, finally clipping the
	 * entity's position to stay inside any solid world boundaries.
	 *
	 * @param entity The entity to update.
	 * @param time   The time which has passed since the last update, in seconds.
	 */
	private void updatePosition(Entity entity, double time) {
		// Calculate expected delta values
		double deltaX = (entity.getXVel() * time) + (0.5 * entity.getXAccel() * (time * time));
		double deltaY = (entity.getYVel() * time) + (0.5 * entity.getYAccel() * (time * time));
		// Calculate tile collisions if needed
		if (!entity.canNoClip()) {
			stepPosition(entity, deltaX, deltaY);
		} else {
			entity.setX(entity.getX() + deltaX);
			entity.setY(entity.getY() + deltaY);
		}
		// Force player inside world boundaries
		double bottomY = entity.getY() - (entity.getHeight() / 2);
		if (bottomY < -0.5) {
			entity.setY((entity.getHeight() / 2) - 0.5);
			entity.setYVel(0);
			if (world.getGravity() > 0) {
				entity.setOnGround(true);
			}
		}
		int worldHeight = world.getHeight() * world.getChunkSize();
		double topY = entity.getY() + (entity.getHeight() / 2);
		if (topY > (worldHeight - 0.5)) {
			entity.setY((worldHeight - 0.5) - (entity.getHeight() / 2));
			entity.setYVel(0);
			if (world.getGravity() < 0) {
				entity.setOnGround(true);
			}
		}
	}
	
	/**
	 * Steps an entity's position and checks tile collisions at each step.
	 *
	 * @param entity The entity to update.
	 * @param deltaX The desired X-axis displacement, in tiles.
	 * @param deltaY The desired Y-axis displacement, in tiles.
	 */
	private void stepPosition(Entity entity, double deltaX, double deltaY) {
		double stepSize = 0.5;
		Point2D.Double entityPos = new Point2D.Double(entity.getX(), entity.getY());
		// Calculate movement distance and direction
		double distance = Math.sqrt((deltaX * deltaX) + (deltaY * deltaY));
		double angle = Math.atan2(deltaY, deltaX);
		// Calculate size and number of movement steps
		int steps = (int) Math.floor(distance / stepSize);
		double remainder = distance % stepSize;
		Point2D.Double step = new Point2D.Double(stepSize * Math.cos(angle), stepSize * Math.sin(angle));
		// Check collisions at each step
		for (int i = 1; i <= steps; i++) {
			Point2D.Double stepPos = new Point2D.Double(entityPos.x + step.x, entityPos.y + step.y);
			applyTileCollisions(entity, entityPos, stepPos, deltaX, deltaY, step);
			// Exit loop if no more steps should be made
			if (step.x == 0 && step.y == 0) {
				break;
			}
		}
		// Handle remainder
		Point2D.Double remainderPos = new Point2D.Double(entityPos.x + (remainder * Math.cos(angle)), entityPos.y + (remainder * Math.sin(angle)));
		applyTileCollisions(entity, entityPos, remainderPos, deltaX, deltaY, step);
		// Update position
		entity.setX(entityPos.x);
		entity.setY(entityPos.y);
	}

	/**
	 * Applies tile collisions to an entity. More specifically, checks for new tile
	 * collisions at unsafePos, and updates the coordinates of safePos to represent
	 * the new collision-less position for this entity.
	 *
	 * @param entity    The entity to update.
	 * @param safePos   A point representing the farthest currently known safe tile
	 *                  position this entity can be stepped to. (Will be modified if
	 *                  no collisions are found!)
	 * @param unsafePos A point representing the tile position this entity should
	 *                  attempt to step to if no collisions are found.
	 * @param deltaX    The desired X-axis direction, only sign matters.
	 * @param deltaY    The desired Y-axis direction, only sign matters.
	 * @param step      A point containing the X and Y step sizes. (Will be modified
	 *                  if needed!)
	 */
	private void applyTileCollisions(Entity entity, Point2D.Double safePos, Point2D.Double unsafePos, double deltaX, double deltaY, Point2D.Double step) {
		Set<Point> collisions = findTileCollisions(entity, unsafePos.x, unsafePos.y);
		Set<CollisionSide> sides = calculateCollisionSides(entity, unsafePos.x, unsafePos.y, deltaX, deltaY, collisions);
		// Update velocities and step sizes
		if (sides.contains(CollisionSide.TOP) || sides.contains(CollisionSide.BOTTOM)) {
			entity.setYVel(0);
			step.y = 0;
		} else {
			safePos.y = unsafePos.y;
		}
		if (sides.contains(CollisionSide.LEFT) || sides.contains(CollisionSide.RIGHT)) {
			entity.setXVel(0);
			step.x = 0;
		} else {
			safePos.x = unsafePos.x;
		}
		// Update onGround state
		entity.setOnGround(false);
		entity.setOnGround((world.getGravity() > 0 && sides.contains(CollisionSide.BOTTOM))
				|| (world.getGravity() < 0 && sides.contains(CollisionSide.TOP)));
	}
	
	/**
	 * Find tile collisions.
	 *
	 * @param entity The entity to check for.
	 * @param x      The X coordinate to check for tile collisions with this entity
	 *               at, in tiles.
	 * @param y      The Y coordinate to check for tile collisions with this entity
	 *               at, in tiles.
	 * @return A set containing points for each tile this entity would collide with
	 *         at the new coordinates.
	 */
	private Set<Point> findTileCollisions(Entity entity, double x, double y) {
		// TODO ignore tiles that are already intersected
		int worldTileHeight = world.getHeight() * world.getChunkSize();
		//
		double halfWidth = entity.getWidth() / 2;
		int leftX = (int) Math.round(x - halfWidth);
		int rightX = (int) Math.round(x + halfWidth);
		//
		double halfHeight = entity.getHeight() / 2;
		int bottomY = (int) Math.round(y - halfHeight);
		int topY = (int) Math.round(y + halfHeight);
		//
		Set<Point> collisions = new HashSet<Point>();
		for (int tx = leftX; tx <= rightX; tx++) {
			for (int ty = bottomY; ty <= topY; ty++) {
				Point corrected = world.correctCoord(tx, ty);
				if (corrected.y >= 0 && corrected.y < worldTileHeight) {
					if (checkCollidableTile(corrected.x, corrected.y)) {
						collisions.add(new Point(tx, ty));
					}
				}
			}
		}
		return collisions;
	}
	
	/**
	 * Check for collidable tile at the specified coordinates.
	 *
	 * @param x The corrected X value to check for a tile at.
	 * @param y The corrected Y value to check for a tile at.
	 * @return true, if a tile is found.
	 */
	private boolean checkCollidableTile(int x, int y) {
		Tile tile2 = world.getTile(x, y, 2);
		return tile2 != null;
	}
	
	/**
	 * Calculate collision sides.
	 *
	 * @param entity the entity
	 * @param x the x
	 * @param y the y
	 * @param deltaX the delta X
	 * @param deltaY the delta Y
	 * @param collisions the collisions
	 * @return the sets the
	 */
	private Set<CollisionSide> calculateCollisionSides(Entity entity, double x, double y, double deltaX, double deltaY, Set<Point> collisions) {
		//
		double halfWidth = entity.getWidth() / 2;
		double leftX = x - halfWidth;
		double rightX = x + halfWidth;
		//
		double halfHeight = entity.getHeight() / 2;
		double bottomY = y - halfHeight;
		double topY = y + halfHeight;
		// Entity bounding rectangle
		Rectangle2D.Double entityBounds = new Rectangle2D.Double(x - halfWidth, y - halfHeight, entity.getWidth(), entity.getHeight());
		// Check side collisions
		Set<CollisionSide> allSides = new HashSet<CollisionSide>();
		collisions.stream().forEach(point -> {
			// Calculate tile intersection
			Rectangle2D.Double tileBounds = new Rectangle2D.Double(point.x - 0.5, point.y - 0.5, 1, 1);
			Rectangle2D intersection = tileBounds.createIntersection(entityBounds);
			// Check side collisions
			Set<CollisionSide> sides = new HashSet<CollisionSide>();
			// Y axis collisions
			if (intersection.getWidth() >= intersection.getHeight()) {
				if ((deltaY > 0) && (point.y > topY && point.y - 0.5 < topY))
					sides.add(CollisionSide.TOP);
				if ((deltaY < 0) && (point.y < bottomY && point.y + 0.5 > bottomY))
					sides.add(CollisionSide.BOTTOM);
			}
			// X axis collisions
			if (intersection.getHeight() >= intersection.getWidth()) {
				if ((deltaX < 0) && (point.x < leftX && point.x + 0.5 > leftX))
					sides.add(CollisionSide.LEFT);
				if ((deltaX > 0) && (point.x > rightX && point.x - 0.5 < rightX))
					sides.add(CollisionSide.RIGHT);
			}
			allSides.addAll(sides);
		});
		return allSides;
	}
	
	/**
	 * Updates the velocity on an entity.
	 *
	 * @param entity The entity to update.
	 * @param time The time which has passed since the last update, in seconds.
	 */
	private void updateVelocity(Entity entity, double time) {
		double xVel = entity.getXVel();
		double yVel = entity.getYVel();
		// Decay velocity for flying entities
		if (entity instanceof FlyingEntity) {
			FlyingEntity flyingEntity = (FlyingEntity) entity;
			if (flyingEntity.isFlying()) {
				xVel *= Math.pow(flyingEntity.getFlightDecayX(), time);
				yVel *= Math.pow(flyingEntity.getFlightDecayY(), time);
			}
		}
		// Apply acceleration
		xVel += entity.getXAccel() * time;
		yVel += entity.getYAccel() * time;
		// C
		if (Math.abs(xVel) < 0.001)
			xVel = 0;
		if (Math.abs(yVel) < 0.001)
			yVel = 0;
		// Update velocities
		entity.setXVel(xVel);
		entity.setYVel(yVel);
	}
	
	/**
	 * Updates the acceleration on an entity by first resetting it, then applying
	 * gravity.
	 *
	 * @param entity The entity to update.
	 */
	private void updateAcceleration(Entity entity) {
		entity.setXAccel(0);
		entity.setYAccel(entity.isGravityAffected() ? (-world.getGravity() * 6) : 0);
	}
	
	/**
	 * Process entity collisions.
	 *
	 * @param entity the entity
	 */
	private void processEntityCollisions(Entity entity) {
		
	}
	
	
	/**
	 * Update control input.
	 *
	 * @param controlledIds the controlled ids
	 * @param inputs The input directions
	 * @param mousePos The mouse's position as normal world coordinates
	 */
	public void updateControlInputs(Set<UUID> controlledIds, Set<Input> inputs, Point mousePos) {
		this.controlledIds = controlledIds;
		this.inputDirections = inputs;
		this.mousePos = mousePos;
	}

}
