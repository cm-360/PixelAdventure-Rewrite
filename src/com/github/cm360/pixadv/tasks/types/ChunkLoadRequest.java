package com.github.cm360.pixadv.tasks.types;

import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.world.storage.world.LocalWorld;
import com.github.cm360.pixadv.world.storage.world.RemoteWorld;
import com.github.cm360.pixadv.world.storage.world.World;

public class ChunkLoadRequest implements Task {

	private Registry registry;
	private World world;
	private int cx, cy;
	
	public ChunkLoadRequest(Registry registry, World world, int cx, int cy) {
		this.registry = registry;
		this.world = world;
		this.cx = cx;
		this.cy = cy;
	}
	
	@Override
	public void process() {
		if (world instanceof LocalWorld) {
			((LocalWorld) world).loadChunk(registry, cx, cy);
		} else if (world instanceof RemoteWorld) {
			// Send chunk request packet
		} else {
			
		}
	}

}
