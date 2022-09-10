package com.github.cm360.pixadv.world.storage.world;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;

import com.github.cm360.pixadv.graphics.picasso.HashablePoint;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.io.ChunkReader;
import com.github.cm360.pixadv.world.storage.Chunk;

public class LocalWorld extends World {

	protected File saveDirectory;
	
	public LocalWorld(int width, int height, int chunkSize, Map<String, String> info, File directory) {
		super(width, height, chunkSize, info);
		setDirectory(directory);
	}
	
	@Override
	public boolean loadChunk(Registry registry, int cx, int cy) {
		try {
			// Load chunk
			File chunkFile = new File(String.format("%s/chunks/%d_%d.pachunk", saveDirectory, cx, cy).replace('/', File.separatorChar));
			chunks[cx][cy] = new ChunkReader(registry, chunkSize).read(chunkFile);
			chunkUpdates.add(new HashablePoint(cx, cy));
			return true;
		} catch (Exception e) {
			if (e.getClass().equals(FileNotFoundException.class)) {
				Logger.logMessage(Logger.WARNING, "Creating empty chunk at %d,%d", cx, cy);
				chunks[cx][cy] = new Chunk(chunkSize);
				chunkUpdates.add(new HashablePoint(cx, cy));
				return true;
			} else {
				Logger.logException("Failed to load chunk %d,%d!", e, cx, cy);
			}
		}
		return false;
	}
	
	@Override
	public boolean unloadChunk(int cx, int cy) {
		// Save chunk
		return true;
	}
	
	public File getDirectory() {
		return saveDirectory;
	}
	
	public void setDirectory(File newDirectory) {
		saveDirectory = newDirectory;
	}

}
