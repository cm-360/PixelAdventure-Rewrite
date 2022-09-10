package com.github.cm360.pixadv.world.storage.universe;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.cm360.pixadv.commands.CommandProcessor;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.world.storage.world.World;

public abstract class Universe {

	protected Registry registry;
	private CommandProcessor commandProcessor;
	
	protected Map<String, String> info;
	protected Map<String, World> worlds;
	protected Supplier<UUID> playerIdSupplier;
	protected String currentWorld;
	
	protected Universe(Registry registry) {
		this(registry, null);
	}
	
	protected Universe(Registry registry, Supplier<UUID> playerIdSupplier) {
		this.registry = registry;
		this.playerIdSupplier = playerIdSupplier;
		this.commandProcessor = new CommandProcessor(this);
		this.info = new HashMap<String, String>();
		this.worlds = new HashMap<String, World>();
		this.currentWorld = "GENTEST";
	}
	
	public World getWorld(String name) {
		return worlds.get(name);
	}
	
	public World getCurrentWorld() {
		return getWorld(currentWorld);
	}
	
	public String[] getWorldNames() {
		return worlds.keySet().stream().toArray(size -> new String[size]);
	}
	
	public UUID getPlayerId() {
		if (playerIdSupplier != null)
			return playerIdSupplier.get();
		else
			return null;
	}
	
	public String getInfo(String key) {
		return info.get(key);
	}
	
	public String getName() {
		return info.getOrDefault("name", "Unnamed Universe");
	}
	
	public CommandProcessor getCommandProcessor() {
		return commandProcessor;
	}
	
	public void close() {
		worlds.values().forEach(World::close);
	}

}
