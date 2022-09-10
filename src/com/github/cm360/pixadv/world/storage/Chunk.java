package com.github.cm360.pixadv.world.storage;

import java.io.File;

import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.TextUtil;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class Chunk {

	private int size = 20;
	private Tile[][][] tiles;
	
	public Chunk(int size) {
		this.size = size;
		this.tiles = new Tile[size][size][3];
	}
	
	public Tile getTile(int x, int y, int layer) {
		return tiles[x][y][layer];
	}
	
	public void setTile(Tile tile, int x, int y, int layer) {
		tiles[x][y][layer] = tile;
	}
	
	public void setSize(int newSize) {
		size = newSize;
	}
	
	public int getSize() {
		return size;
	}
	
	// IO methods
	public static Chunk load(Registry registry, String chunkData, World parent) {
		Chunk chunk = new Chunk(parent.getChunkSize());
		// Parse each line of chunk data
		for (String line : chunkData.split("[\\n\\r]+")) {
			try {
				// Parse line into position and tile info
				String[] lineSplit = line.split(":", 3);
				String[] pos = lineSplit[0].split("_", 3);
				Tile tile = registry.getTile(Identifier.parse(lineSplit[1]));
				tile.setData(lineSplit[2]);
				chunk.setTile(tile, Integer.parseInt(pos[0]), Integer.parseInt(pos[1]), Integer.parseInt(pos[2]));
			} catch (Exception e) {
				// Improperly formatted line
				System.out.println("  Corrupt entry found while loading chunk!");
				System.out.println("    " + line);
				System.out.printf("    %s: %s\n", e.getClass().getName(), e.getMessage());
			}
		}
		return chunk;
	}

	public void saveTo(File chunkFile, int chunkSize) {
		StringBuilder output = new StringBuilder();
		for (int x = 0; x < chunkSize; x++)
			for (int y = 0; y < chunkSize; y++)
				for (int l = 0; l < 3; l++) {
					output.append(String.format("%s_%s_%s:%s\n", x, y, l, getTile(x, y, l)));
				}
		TextUtil.write(chunkFile, output.toString());
	}

}
