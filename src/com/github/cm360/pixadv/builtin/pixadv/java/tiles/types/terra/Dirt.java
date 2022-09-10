package com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.terra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.world.types.tiles.Tile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Dirt implements Tile {

	public boolean mud, grass, snow;
	
	@Override
	public String getID() {
		return "terra/dirt";
	}
	
	@Override
	public void setData(String data) {
		try {
			HashMap<String, String> dataMap = new Gson().fromJson(data, TypeToken.getParameterized(HashMap.class, String.class, String.class).getType());
			mud = Boolean.parseBoolean(dataMap.getOrDefault("mud", "false"));
			grass = Boolean.parseBoolean(dataMap.getOrDefault("grass", "false"));
			snow = Boolean.parseBoolean(dataMap.getOrDefault("snow", "false"));
		} catch (Exception e) {
			// Do nothing
		}
	}
	
	@Override
	public String getData() {
		HashMap<String, String> dataMap = new HashMap<String, String>();
		dataMap.put("mud", Boolean.toString(mud));
		dataMap.put("grass", Boolean.toString(grass));
		dataMap.put("snow", Boolean.toString(snow));
		return new Gson().toJson(dataMap);
	}
	
	@Override
	public List<Identifier> getTextures() {
		List<Identifier> textures = new ArrayList<Identifier>();
		textures.add(Identifier.parse("pixadv:textures/tiles/terra/dirt/" + (mud ? "mud" : "dirt") + "/basic"));
		if (grass)
			textures.add(Identifier.parse("pixadv:textures/tiles/terra/dirt/" + (mud ? "mud" : "dirt") + "/grass"));
		if (snow)
			textures.add(Identifier.parse("pixadv:textures/tiles/terra/dirt/" + (mud ? "mud" : "dirt") + "/snow"));
		return textures;
	}
	
	@Override
	public double getFriction() {
		return 0.5;
	}

}
