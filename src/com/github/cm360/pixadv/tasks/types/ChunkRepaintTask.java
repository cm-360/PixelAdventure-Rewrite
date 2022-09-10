package com.github.cm360.pixadv.tasks.types;

import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Transparency;
import java.awt.image.BufferedImage;

import com.github.cm360.pixadv.graphics.picasso.ChunkImage;
import com.github.cm360.pixadv.graphics.picasso.Picasso;
import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.util.ImageUtil;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class ChunkRepaintTask implements Task {

	private Client client;
	private World world;
	private int cx, cy;
	
	public ChunkRepaintTask(Client client, World world, int cx, int cy) {
		this.client = client;
		this.world = world;
		this.cx = cx;
		this.cy = cy;
	}
	
	@Override
	public void process() {
		Picasso picasso = client.getRenderingEngine();
		Point chunkPos = new Point(cx, cy);
		String chunkName = chunkPos.toString();
		// Create a new image for the chunk cache
		BufferedImage chunkImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice()
				.getDefaultConfiguration()
				.createCompatibleImage(
						picasso.getTileTextureSize() * world.getChunkSize(),
						picasso.getTileTextureSize() * world.getChunkSize(),
						Transparency.TRANSLUCENT);
		// Draw tiles to chunk image
		Graphics2D cg = chunkImage.createGraphics();
		for (int xc = 0; xc < world.getChunkSize(); xc++)
			for (int yc = 0; yc < world.getChunkSize(); yc++) {
				try {
					int x = cx * world.getChunkSize() + xc;
					int y = cy * world.getChunkSize() + yc;
					// Draw tile layers
					for (int l = 0; l < 3; l++) {
						Tile tile = world.getTile(x, y, l);
						if (tile != null) {
							for (Identifier textureId : tile.getTextures()) {
								BufferedImage texture = client.getRegistry().getTexture(textureId);
								if (l == 0)
									texture = ImageUtil.applyBrightness(texture, 0.5);
								// Texture coordinates in chunk image
								int tx = picasso.getTileTextureSize() * xc;
								int ty = picasso.getTileTextureSize() * (world.getChunkSize() - (yc + 1));
								// Draw with clip to prevent texture overlapping
								cg.setClip(tx, ty, picasso.getTileTextureSize(), picasso.getTileTextureSize());
								cg.drawImage(texture, tx, ty, null);
							}
						}
					}
				} catch (Exception e) {
					// Save exception info
					client.getGamePanel().setLastExceptionInfo(
							"Exception: Chunk %s   %s: %s".formatted(
									chunkName,
									e.getClass().getName(),
									e.getMessage()),
							System.currentTimeMillis());
					Logger.logException("Exception while drawing chunk %s", e, chunkName);
				}
			}
		// Finalization
		cg.dispose();
		picasso.cacheChunkImage(chunkPos, new ChunkImage(chunkImage, System.nanoTime()));
		world.getChunkUpdates().remove(chunkPos);
	}

}
