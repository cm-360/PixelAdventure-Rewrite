package com.github.cm360.pixadv.graphics.edison;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import com.github.cm360.pixadv.builtin.pixadv.java.tiles.capabilities.LightEmitter;
import com.github.cm360.pixadv.graphics.picasso.HashablePoint;
import com.github.cm360.pixadv.world.storage.Chunk;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class Edison {

	protected World world;
	protected Map<HashablePoint, Lightmap> lightmaps;
	
	public Edison(World world) {
		this.world = world;
		lightmaps = new HashMap<HashablePoint, Lightmap>();
	}
	
	public void relight(HashablePoint chunkPos) {
		int chunkSize = world.getChunkSize();
		Chunk chunk = world.getChunk(chunkPos.x, chunkPos.y);
		double[][] intensities = new double[chunkSize][chunkSize];
		Color[][] colors = new Color[chunkSize][chunkSize];
		for (int x = 0; x < chunkSize; x++) {
			for (int y = 0; y < chunkSize; y++) {
				double intensity = -1;
				// Check each layer
				for (int l = 0; l < 3; l++) {
					Tile tile = chunk.getTile(x, y, l);
					if (tile != null) {
						if (tile instanceof LightEmitter) {
							LightEmitter tileEmitter = (LightEmitter) tile;
							intensity = Math.max(intensity, tileEmitter.getLightIntensity());
							// TODO process color
						} else {
							intensity = 0;
						}
					}
				}
				intensities[x][y] = intensity;
				colors[x][y] = Color.WHITE;
			}
		}
		lightmaps.put(chunkPos, new Lightmap(chunkSize, 16, intensities, colors));
	}
	
	
	
	public boolean isLit(HashablePoint chunkPos) {
		return lightmaps.containsKey(chunkPos);
	}
	
	public Lightmap getLightmap(HashablePoint chunkPos) {
		return lightmaps.get(chunkPos);
	}
	
// transmittance, how much light a block can pass
}
