package com.github.cm360.pixadv.builtin.pixadv;

import java.util.HashMap;
import java.util.Map;

import com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.terra.Dirt;
import com.github.cm360.pixadv.registry.ModuleContentProvider;
import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class BuiltinModuleContentProvider implements ModuleContentProvider {

	@Override
	public Map<String, Class<? extends Tile>> getTiles() {
		Map<String, Class<? extends Tile>> tiles = new HashMap<String, Class<? extends Tile>>();
		tiles.put("terra/dirt", Dirt.class);
		tiles.put("terra/stone", Dirt.class);
		return tiles;
	}

	@Override
	public Map<String, Class<? extends Entity>> getEntities() {
		Map<String, Class<? extends Entity>> entities = new HashMap<String, Class<? extends Entity>>();
		return entities;
	}

}
