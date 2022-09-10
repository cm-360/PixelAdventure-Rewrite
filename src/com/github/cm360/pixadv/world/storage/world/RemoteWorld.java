package com.github.cm360.pixadv.world.storage.world;

import java.util.HashMap;

import com.github.cm360.pixadv.registry.Registry;

public class RemoteWorld extends World {

	public RemoteWorld(int width, int height, int chunkSize,HashMap<String, String> info) {
		super(width, height, chunkSize, info);
	}

	@Override
	public boolean loadChunk(Registry registry, int cx, int cy) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean unloadChunk(int cx, int cy) {
		// TODO Auto-generated method stub
		return false;
	}

}
