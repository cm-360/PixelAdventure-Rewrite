package com.github.cm360.pixadv.registry;

import java.util.Map;

import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public interface ModuleContentProvider {

	public Map<String, Class<? extends Tile>> getTiles();

	public Map<String, Class<? extends Entity>> getEntities();

}
