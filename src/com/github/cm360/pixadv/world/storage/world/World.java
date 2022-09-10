package com.github.cm360.pixadv.world.storage.world;

import java.awt.Point;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.github.cm360.pixadv.graphics.edison.Edison;
import com.github.cm360.pixadv.graphics.picasso.HashablePoint;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.newton.Newton;
import com.github.cm360.pixadv.world.storage.Chunk;
import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public abstract class World {
	
	protected Map<String, String> info;
	
	protected int width;
	protected int height;
	protected int chunkSize;
	protected double gravity = 9.8;
	protected Chunk[][] chunks;
	protected Queue<HashablePoint> chunkUpdates;
	
	protected Newton newton;
	protected Thread physicsThread;
	protected int physicsTickTimesQueueSize = 200;
	protected Queue<Long> physicsTickTimes;
	protected Map<UUID, Entity> entities;
	
	protected Edison edison;
	
	protected double cameraX = 0;
	protected double cameraY = 0;
	
	protected boolean paused;
	
	public World(int width, int height, int chunkSize, Map<String, String> info) {
		this.width = width;
		this.height = height;
		this.chunkSize = chunkSize;
		this.info = info;
		chunks = new Chunk[width][height];
		entities = new HashMap<UUID, Entity>();
		chunkUpdates = new ConcurrentLinkedQueue<HashablePoint>();
		paused = false;
		// Start physics engine
		newton = new Newton(this);
		physicsTickTimes = new ArrayDeque<Long>(physicsTickTimesQueueSize);
		physicsThread = new Thread(() -> {
			while (!Thread.interrupted()) {
				// Pause if needed
				if (paused) {
					try {
						wait();
					} catch (InterruptedException e) {
						break;
					}
				}
				// Tick physics
				long tickTime = newton.tick();
				synchronized (physicsTickTimes) {
					while (physicsTickTimes.size() >= physicsTickTimesQueueSize)
						physicsTickTimes.remove();
					physicsTickTimes.add(tickTime);
				}
			}
			Logger.logMessage(Logger.DEBUG, "Stopped physics engine");
		}, "Physics-%s".formatted(getName()));
		physicsThread.start();
		// Start lighting engine
		edison = new Edison(this);
	}
	
	public Tile getTile(int x, int y, int layer) {
		Point chunk = getChunkOf(x, y);
		Point coord = getChunkCoordOf(x, y);
		// TODO request unloaded chunk
		if (isChunkLoaded(chunk.x, chunk.y))
			return chunks[chunk.x][chunk.y].getTile(coord.x, coord.y, layer);
		else
			return null;
	}
	
	public boolean setTile(Tile tile, int x, int y, int layer) {
		HashablePoint chunk = new HashablePoint(getChunkOf(x, y));
		Point coord = getChunkCoordOf(x, y);
		// TODO request unloaded chunk
		if (isChunkLoaded(chunk.x, chunk.y)) {
			chunks[chunk.x][chunk.y].setTile(tile, coord.x, coord.y, layer);
			if (!chunkUpdates.contains(chunk))
				chunkUpdates.add(chunk);
			return true;
		} else {
			return false;
		}
	}
	
	public Point correctCoord(int x, int y) {
		int xNew = x;
		int worldWidthTiles = getWidth() * chunkSize;
		if (x < 0)
			while (xNew < 0)
				xNew += worldWidthTiles;
		else if (x >= worldWidthTiles)
			while (xNew >= worldWidthTiles)
				xNew -= worldWidthTiles;
		return new Point(xNew, y);
	}
	
	public Point correctChunkCoord(int cx, int cy) {
		int cxNew = cx;
		if (cx < 0)
			while (cxNew < 0)
				cxNew += getWidth();
		else if (cx >= getWidth())
			while (cxNew >= getWidth())
				cxNew -= getWidth();
		return new Point(cxNew, cy);
	}
	
	public void createChunk(int cx, int cy) {
		chunks[cx][cy] = new Chunk(chunkSize);
	}
	
	public abstract boolean loadChunk(Registry registry, int cx, int cy);
	
	public abstract boolean unloadChunk(int cx, int cy);
	
	public Chunk getChunk(int cx, int cy) {
		return chunks[cx][cy];
	}
	
	public Point getChunkOf(int x, int y) {
		return new Point(x / chunkSize, y / chunkSize);
	}
	
	public Point getChunkCoordOf(int x, int y) {
		return new Point(x % chunkSize, y % chunkSize);
	}
	
	public boolean isTileLoaded(int x, int y) {
		Point tileCoords = getChunkOf(x, y);
		return isChunkLoaded(tileCoords.x, tileCoords.y);
	}
	
	public boolean isChunkLoaded(int cx, int cy) {
		return chunks[cx][cy] != null;
	}
	
	public Queue<HashablePoint> getChunkUpdates() {
		return chunkUpdates;
	}
	
	public Map<UUID, Entity> getEntities() {
		return entities;
	}
	
	public Entity getEntity(UUID uuid) {
		return entities.get(uuid);
	}
	
	public void addEntity(UUID uuid, Entity entity) {
		entity.setY(getHeight() / 3.0 * getChunkSize());
		entities.put(uuid, entity);
	}
	
	public Entity removeEntity(UUID uuid) {
		return entities.remove(uuid);
	}
	
	public String getInfo(String key) {
		return info.get(key);
	}
	
	public String getName() {
		return info.getOrDefault("name", "Unnamed World");
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public int getChunkSize() {
		return chunkSize;
	}
	
	public double getCameraX() {
		return cameraX;
	}
	
	public double getCameraY() {
		return cameraY;
	}
	
	public void setCameraX(double x) {
		cameraX = x;
	}
	
	public void setCameraY(double y) {
		cameraY = y;
	}
	
	public double getGravity() {
		return gravity;
	}
	
	public Newton getPhysicsEngine() {
		return newton;
	}
	
	public Long[] getPhysicsTickTimes() {
		synchronized (physicsTickTimes) {
			return physicsTickTimes.toArray(size -> new Long[size]);
		}
	}
	
	public Edison getLightingEngine() {
		return edison;
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public void setPaused(boolean paused) {
		this.paused = paused;
		notifyAll();
	}
	
	public void close() {
		physicsThread.interrupt();
	}

}
