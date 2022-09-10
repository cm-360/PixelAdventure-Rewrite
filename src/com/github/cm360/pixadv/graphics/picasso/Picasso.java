package com.github.cm360.pixadv.graphics.picasso;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.cm360.pixadv.graphics.edison.Edison;
import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.util.Stopwatch;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class Picasso {

	private final Client client;

	private int tileTextureSize = 16;
	private double tileTextureScale = 4;
	
	private int chunkCacheTarget = 200;
	private int chunkCacheMax = 300;
	private Map<HashablePoint, ChunkImage> chunkCache;
	
	
	// Constructor
	public Picasso(Client client) {
		this.client = client;
		chunkCache = new HashMap<HashablePoint, ChunkImage>();
	}
	
	// Main method
	public RenderStats paint(Graphics g, World world, Precompute precomp, Point mouseLocation, Stopwatch renderTimes) {
		RenderStats stats = new RenderStats();
		if (world == null) {
			// Draw loading message
			int shade = 64 + (int) (64 * (Math.sin(System.currentTimeMillis() / 500.0) + 1));
			g.setColor(new Color(shade, shade, shade));
			g.setFont(client.getGamePanel().getDefaultFont().deriveFont(40f));
			FontMetrics gfm = g.getFontMetrics();
			String loadingMessage = "Loading world...";
			g.drawString(loadingMessage,
					(precomp.getGBounds().width - gfm.stringWidth(loadingMessage)) / 2,
					(precomp.getGBounds().height + gfm.getAscent()) / 2);
			renderTimes.mark("ui-loading");
		} else {
			precomp.update(world, this);
			// Save entity coordinates before rendering
			Map<UUID, Point2D.Double> entityPositions = world.getEntities().entrySet().stream()
					.collect(Collectors.toMap(entry -> {
						return entry.getKey();
					}, entry -> {
						Entity entity = entry.getValue();
						return new Point2D.Double(entity.getX(), entity.getY());
					}));
			//
			Point2D.Double cameraFollowedEntityPos = entityPositions.get(client.getCameraFollowedId());
			if (cameraFollowedEntityPos != null) {
				world.setCameraX(cameraFollowedEntityPos.x);
				world.setCameraY(cameraFollowedEntityPos.y);
			}
			renderTimes.mark("entity-positions");
			// Draw sky
			paintSky(g, world, precomp);
			renderTimes.mark("sky");
			// Draw parallax background
//			paintBackgroundImages(g, world, precomp);
			renderTimes.mark("background");
			// Draw chunkmap
			paintChunkmap(g, world, precomp);
			renderTimes.mark("chunkmap");
			// Draw lightmap
			// paintLightmap(g, world, precomp);
			renderTimes.mark("lightmap");
			// Draw entities
			paintEntities(g, world, precomp, stats, entityPositions);
			renderTimes.mark("entities");
			// Draw tile hover info
			if (client.getGamePanel().showUI && mouseLocation != null) {
				Point mouseTile = getMouseTile(world, precomp, mouseLocation);
				paintMouseHover(g, world, precomp, mouseLocation, mouseTile);
				renderTimes.mark("mouse");
			}
		}
		return stats;
	}
	
	protected void paintSky(Graphics g, World world, Precompute precomp) {
		int[] horizonColor = {182, 206, 245};
		int[] zenithColor = {85, 127, 187};
		Rectangle gBounds = precomp.getGBounds();
		for (int h = 0; h < gBounds.height; h++) {
			float elevation = ((float) h) / gBounds.height;
			g.setColor(new Color(
					(int) Math.round((horizonColor[0] * elevation) + (zenithColor[0] * (1 - elevation))),
					(int) Math.round((horizonColor[1] * elevation) + (zenithColor[1] * (1 - elevation))),
					(int) Math.round((horizonColor[2] * elevation) + (zenithColor[2] * (1 - elevation)))));
			g.drawLine(
					gBounds.x,
					gBounds.y + h,
					gBounds.x + gBounds.width,
					gBounds.y + h);
		}
	}
	
	protected void paintBackgroundImages(Graphics g, World world, Precompute precomp) {
		int[] powersOf2 = {1, 2, 4, 8, 16, 32, 64, 128};
		Identifier backgroundImageId = Identifier.parse("pixadv:textures/tiles/missing");
		for (int i = 3; i >= 1; i--) {
			g.drawImage(client.getRegistry().getTexture(backgroundImageId),
					(int) Math.round(precomp.getGBounds().x - (world.getCameraX() * precomp.getScaledTileTextureSize()) / powersOf2[i]),
					(int) Math.round(precomp.getGBounds().y + (((world.getCameraY() * precomp.getScaledTileTextureSize()) / powersOf2[i]) - (world.getHeight() * world.getChunkSize()))),
					precomp.getGBounds().width,
					precomp.getGBounds().height, null);
		}
	}
	
	protected void paintChunkmap(Graphics g, World world, Precompute precomp) {
		// Avoid wasting memory with old cached images
		if (getCacheSize() > chunkCacheMax) {
			Point cameraPoint = world.correctCoord(
					(int) Math.round(world.getCameraX()),
					(int) Math.round(world.getCameraY()));
			Point cameraChunk = world.getChunkOf(cameraPoint.x, cameraPoint.y);
			client.getTaskQueueManager().addGenericTask(() -> trimCache(cameraChunk));
		}
		// Draw the different chunk images
		for (int cx = (int) Math.round((precomp.getMinX() + 0.5) / world.getChunkSize() - 0.5); cx <= (int) Math.round((precomp.getMaxX() - 0.5) / world.getChunkSize() - 0.5); cx++) {
			for (int cy = (int) Math.round((precomp.getMinY() + 0.5) / world.getChunkSize() - 0.5); cy <= (int) Math.round((precomp.getMaxY() - 0.5) / world.getChunkSize() - 0.5); cy++) {
				if (cy >= 0 && cy < world.getHeight()) {
					// Get actual chunk coordinates
					HashablePoint correctedChunk = new HashablePoint(world.correctChunkCoord(cx, cy));
					if (world.isChunkLoaded(correctedChunk.x, correctedChunk.y)) {
						ChunkImage chunkImage = chunkCache.get(correctedChunk);
						if (chunkImage == null || world.getChunkUpdates().contains(correctedChunk)) {
							client.getTaskQueueManager().repaintChunk(client, world, correctedChunk.x, correctedChunk.y);
						}
						if (chunkImage != null) {
							// Draw entire chunk
							g.drawImage(chunkImage.getImage(),
									precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize()
											* (cx * world.getChunkSize() - world.getCameraX())),
									precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize()
											* ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)),
									(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()),
									(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()), null);
						}
					} else {
						// Request that this chunk be loaded
						client.getTaskQueueManager().requestChunk(client.getRegistry(), world, correctedChunk.x, correctedChunk.y);
					}
					// Draw chunk-specific debug info
					if (client.getGamePanel().showUI && client.getGamePanel().showDebugMenu) {
						g.setColor(Color.WHITE);
						g.setFont(new Font(null, 0, 12));
						g.drawRect(
								precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize() * (cx * world.getChunkSize() - world.getCameraX())),
								precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize() * ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)),
								(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()),
								(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()));
						g.drawString(String.format("%d,%d (%d,%d)", correctedChunk.x, correctedChunk.y, cx, cy),
								precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize() * (cx * world.getChunkSize() - world.getCameraX())) + 5,
								precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize() * ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)) + 15);
					}
				}
			}
		}
	}
	
	protected void paintLightmap(Graphics g, World world, Precompute precomp) {
		Edison edison = world.getLightingEngine();
		// Draw the different chunk images
		for (int cx = (int) Math.round((precomp.getMinX() + 0.5) / world.getChunkSize() - 0.5); cx <= (int) Math.round((precomp.getMaxX() - 0.5) / world.getChunkSize() - 0.5); cx++) {
			for (int cy = (int) Math.round((precomp.getMinY() + 0.5) / world.getChunkSize() - 0.5); cy <= (int) Math.round((precomp.getMaxY() - 0.5) / world.getChunkSize() - 0.5); cy++) {
				if (cy >= 0 && cy < world.getHeight()) {
					// Get actual chunk coordinates
					HashablePoint correctedChunk = new HashablePoint(world.correctChunkCoord(cx, cy));
					if (world.isChunkLoaded(correctedChunk.x, correctedChunk.y)) {
						if (edison.isLit(correctedChunk)) {
							BufferedImage lightmap = edison.getLightmap(correctedChunk).getScaledMap();
							// Draw entire chunk
							g.drawImage(lightmap,
									precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize()
											* (cx * world.getChunkSize() - world.getCameraX())),
									precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize()
											* ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)),
									(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()),
									(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()), null);
						} else {
							edison.relight(correctedChunk);
						}
					}
					// Draw chunk-specific debug info
					if (client.getGamePanel().showUI && client.getGamePanel().showDebugMenu) {
						g.setColor(Color.WHITE);
						g.setFont(new Font(null, 0, 12));
						g.drawRect(
								precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize() * (cx * world.getChunkSize() - world.getCameraX())),
								precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize() * ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)),
								(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()),
								(int) (precomp.getScaledTileTextureSize() * world.getChunkSize()));
						g.drawString(String.format("%d,%d (%d,%d)", correctedChunk.x, correctedChunk.y, cx, cy),
								precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize() * (cx * world.getChunkSize() - world.getCameraX())) + 5,
								precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize() * ((cy + 1) * world.getChunkSize() - world.getCameraY() - 1)) + 15);
					}
				}
			}
		}
	}

	protected void paintMouseHover(Graphics g, World world, Precompute precomp, Point mouseLocation, Point mouseTile) {
		if (mouseTile.y >= 0 && mouseTile.y < world.getHeight() * world.getChunkSize()) {
			// Tile outline
			int shade = 64 + (int) (64 * (Math.sin(System.currentTimeMillis() / 250.0) + 1));
			g.setColor(new Color(255, 255, 255, shade));
			g.drawRect(
					precomp.getCenterX() + (int) Math.round(precomp.getScaledTileTextureSize() * (mouseTile.x - world.getCameraX())),
					precomp.getCenterY() - (int) Math.round(precomp.getScaledTileTextureSize() * (mouseTile.y - world.getCameraY())),
					(int) precomp.getScaledTileTextureSize() - 1,
					(int) precomp.getScaledTileTextureSize() - 1);
			// Tile position
			if (client.getGamePanel().showDebugMenu) {
				g.setColor(Color.WHITE);
				g.fillRect(mouseLocation.x, mouseLocation.y - 30, 150, 25);
				g.setColor(Color.BLACK);
				g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
				g.drawString(String.format("(x%d,y%d)", mouseTile.x, mouseTile.y), mouseLocation.x + 3, mouseLocation.y - 20);
				Point correctedTileCoords = world.correctCoord(mouseTile.x, mouseTile.y);
				Tile tile0 = world.getTile(correctedTileCoords.x, correctedTileCoords.y, 0);
				Tile tile1 = world.getTile(correctedTileCoords.x, correctedTileCoords.y, 1);
				Tile tile2 = world.getTile(correctedTileCoords.x, correctedTileCoords.y, 2);
				g.drawString(String.format("%s, %s, %s",
						((tile0 == null) ? "air" : tile0.getID()),
						((tile1 == null) ? "air" : tile1.getID()),
						((tile2 == null) ? "air" : tile2.getID())),
						mouseLocation.x + 3, mouseLocation.y - 8);
			}
		}
	}
	
	protected void paintEntities(Graphics g, World world, Precompute precomp, RenderStats stats, Map<UUID, Point2D.Double> entityPositions) {
		int renderCount = 0;
		Map<UUID, Entity> entities = world.getEntities();
		stats.setTotalEntities(entities.size());
		// Draw entities
		for (UUID uuid : entities.keySet()) {
			Entity entity = entities.get(uuid);
			try {
//				HashMap<String, String> data = new Gson().fromJson(entity.getData(), TypeToken.getParameterized(HashMap.class, String.class, String.class).getType());
				// Grab entity location info
				double x = entity.getX();
				double y = entity.getY();
				Point2D.Double entityPos = entityPositions.get(uuid);
				if (entityPos != null) {
					x = entityPos.x;
					y = entityPos.y;
				}
				
				int pixelsWidth = (int) Math.round(precomp.getScaledTileTextureSize() * entity.getWidth());
				int pixelsHeight = (int) Math.round(precomp.getScaledTileTextureSize() * entity.getHeight());
				// Check if entity is visible
				if ((x >= precomp.getMinX() && x < precomp.getMaxX()) && (y >= precomp.getMinY() && y < precomp.getMaxY())) {
					// Draw to panel
					g.drawImage(entity.getTexture(),
							(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2) - pixelsWidth / 2) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
							(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2) - pixelsHeight / 2) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())),
							pixelsWidth, pixelsHeight, null);
					if (client.getGamePanel().showUI && client.getGamePanel().showDebugMenu) {
						// Draw bounding box
						if (client.getControlledIds().contains(uuid))
							g.setColor(Color.CYAN);
						else
							g.setColor(Color.WHITE);
						g.drawRect(
								(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2) - pixelsWidth / 2) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
								(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2) - pixelsHeight / 2) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())),
								pixelsWidth - 1, pixelsHeight - 1);
						// Draw velocity vector
						double xVel = entity.getXVel();
						double yVel = entity.getYVel();
						g.setColor(Color.GREEN);
						g.drawLine(
								(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2)) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
								(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2)) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())),
								(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2)) + (int) Math.round(precomp.getScaledTileTextureSize() * ((x + xVel) - world.getCameraX())),
								(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2)) - (int) Math.round(precomp.getScaledTileTextureSize() * ((y + yVel) - world.getCameraY())));
						// Draw acceleration vector
						double xAccel = entity.getXAccel();
						double yAccel = entity.getYAccel();
						g.setColor(Color.ORANGE);
						g.drawLine(
								(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2)) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
								(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2)) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())),
								(precomp.getCamBounds().x + (precomp.getCamBounds().width / 2)) + (int) Math.round(precomp.getScaledTileTextureSize() * ((x + xAccel) - world.getCameraX())),
								(precomp.getCamBounds().y + (precomp.getCamBounds().height / 2)) - (int) Math.round(precomp.getScaledTileTextureSize() * ((y + yAccel) - world.getCameraY())));
						// Draw coordinates label
						if (pixelsWidth > 50) {
							g.setColor(Color.WHITE);
							g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 10));
							g.drawString("X: %.2f".formatted(x),
									4 + (precomp.getCamBounds().x + (precomp.getCamBounds().width / 2) - pixelsWidth / 2) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
									pixelsHeight + 12 + (precomp.getCamBounds().y + (precomp.getCamBounds().height / 2) - pixelsHeight / 2) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())));
							g.drawString("Y: %.2f".formatted(y),
									4 + (precomp.getCamBounds().x + (precomp.getCamBounds().width / 2) - pixelsWidth / 2) + (int) Math.round(precomp.getScaledTileTextureSize() * (x - world.getCameraX())),
									pixelsHeight + 24 + (precomp.getCamBounds().y + (precomp.getCamBounds().height / 2) - pixelsHeight / 2) - (int) Math.round(precomp.getScaledTileTextureSize() * (y - world.getCameraY())));
						}
					}
					renderCount++;
				}
			} catch (Exception e) {
				// TODO rendering exception
				String message = "Exception while rendering entity!  UUID: %s  Type: %s".formatted(entity.getID(), uuid);
				Logger.logException(message, e);
				client.getGamePanel().setLastExceptionInfo("Exception while rendering entity Entity ID '%s'   %s: %s".formatted(
						entity.getID(),
						e.getClass().getName(),
						e.getMessage()), System.currentTimeMillis());
			}
		}
		stats.setUniqueEntities(renderCount);
	}
	
	// Utility methods
	public Point getMouseTile(World world, Precompute precomp, Point mouseLocation) {
		// Calculations
		int mouseTileX = (int) Math.round((double) (mouseLocation.x - (precomp.getCamBounds().x + precomp.getCamBounds().width / 2)) / precomp.getScaledTileTextureSize() + world.getCameraX());
		int mouseTileY = (int) Math.round((double) ((precomp.getCamBounds().y + precomp.getCamBounds().height / 2) - mouseLocation.y) / precomp.getScaledTileTextureSize() + world.getCameraY());
		// Return point
		return new Point(mouseTileX, mouseTileY);
	}
	
	public void cacheChunkImage(Point location, ChunkImage chunkImage) {
		chunkCache.put(new HashablePoint(location), chunkImage);
	}

	public int getCacheSize() {
		return chunkCache.size();
	}
	
	public void trimCache(Point centerChunk) {
		chunkCache.keySet().retainAll(chunkCache.keySet().stream().sorted((point1, point2) -> {
			return (int) Math.round(point1.distance(centerChunk) - point2.distance(centerChunk));
		}).limit(chunkCacheTarget).toList());
	}
	
	public void clearCache() {
		chunkCache.clear();
	}
	
	// Info methods
	public int getTileTextureSize() {
		return tileTextureSize;
	}
	
	public void setTileScale(double scale) {
		tileTextureScale = scale;
	}

	public double getTileScale() {
		return tileTextureScale;
	}
	
}
