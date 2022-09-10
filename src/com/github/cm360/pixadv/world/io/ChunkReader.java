package com.github.cm360.pixadv.world.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.Chunk;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class ChunkReader {

	private Registry registry;
	private int chunkSize;
	private Chunk builder;
	
	public ChunkReader(Registry registry, int chunkSize) {
		this.registry = registry;
		this.chunkSize = chunkSize;
		this.builder = new Chunk(chunkSize);
	}
	
	public Chunk read(File chunkFile) throws Exception {
		Logger.logMessage(Logger.DEBUG, "Loading chunk from '%s'", chunkFile);
		BufferedReader br = new BufferedReader(new FileReader(chunkFile));
		// Parse lines of file
		String line;
		int index = 0;
		while ((line = br.readLine()) != null) {
			// Read tile data
			String[] lineSplit = line.split(":", 3);
			String tileId = lineSplit[1];
			if (!tileId.equals("pixadv/air")) {
				Tile tile = registry.getTile(Identifier.parse(tileId.replaceFirst("\\/", ":")));
				tile.setData(lineSplit[2]);
				// Set tile at correct coordinates
				int x = index / (chunkSize * 3), y = (index / 3) % chunkSize, z = index % 3;
				builder.setTile(tile, x, y, z);
			}
			index++;
		}
		br.close();
		return builder;
	}

}
