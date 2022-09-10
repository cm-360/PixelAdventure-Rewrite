package com.github.cm360.pixadv.world.storage.universe;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.function.Supplier;

import com.github.cm360.pixadv.builtin.pixadv.java.generators.world.BasicWorldGenerator;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.FileUtil;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.world.LocalWorld;
import com.github.cm360.pixadv.world.storage.world.World;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class LocalUniverse extends Universe {

	protected File saveDirectory;
	
	public LocalUniverse(Registry registry, File directory) {
		this(registry, directory, null);
	}
	
	public LocalUniverse(Registry registry, File directory, Supplier<UUID> playerIdSupplier) {
		super(registry, playerIdSupplier);
		setDirectory(directory);
	}

	public boolean load() {
		Logger.logMessage(Logger.INFO, "Loading '%s'...", saveDirectory);
		try {
			// Check if save directory exists
			if (saveDirectory.exists()) {
				Gson gson = new Gson();
				// Get universe info
				File infoFile = new File(saveDirectory, "info.json");
				if (infoFile.exists()) {
					try (FileReader infoFileReader = new FileReader(infoFile);) {
						info = gson.fromJson(infoFileReader, TypeToken.getParameterized(HashMap.class, String.class, String.class).getType());
						// Check for registry information
						String savedResig = info.get("resig");
						if (savedResig == null) {
							// TODO prompt user what to do
							Logger.logMessage(Logger.ERROR, "The save '%s' is missing registry information!", getName());
						} else {
							// Check for registry compatibility
							if (registry.compatibleWith(savedResig)) {
								List<File> worldDirs = FileUtil.listFiles(new File(saveDirectory, "worlds"), File::isDirectory, false);
								for (File worldDir : worldDirs) {
									// Read world info
									File worldInfoFile = new File(worldDir, "info.json");
									if (worldInfoFile.exists()) {
										try (FileReader worldInfoFileReader = new FileReader(worldInfoFile);) {
											Map<String, String> worldInfo = gson.fromJson(worldInfoFileReader, TypeToken.getParameterized(HashMap.class, String.class, String.class).getType());
											worlds.put(worldDir.getName(), new LocalWorld(
													Integer.parseInt(worldInfo.get("width")),
													Integer.parseInt(worldInfo.get("height")),
													Integer.parseInt(worldInfo.get("chunkSize")),
													worldInfo, worldDir));
										}
									} else {
										Logger.logMessage(Logger.ERROR, "The world '%s' from the save '%s' is missing an info.json file!", worldDir.getName(), getName());
										return false;
									}
								}
								// Test world
//								LocalWorld genWorld = new LocalWorld(10, 1, new HashMap<String, String>(), new File(String.format("%s/worlds/GenTest", saveDirectory).replace('/', File.separatorChar)));
//								for (int x = 0; x < genWorld.getWidth() * genWorld.getChunkSize(); x++) {
//									if (!genWorld.isTileLoaded(x, 0))
//										genWorld.createChunk(x / genWorld.getChunkSize(), 0);
//									Logger.logMessage(Logger.DEBUG, "Placed tile? %s", genWorld.setTile(new Dirt(), x, (int) Math.round(genWorld.getChunkSize() * (SimplexNoise.noise(x / 50.0, 0) + 1) / 2), 0));
//								}
								World genWorld = new BasicWorldGenerator(new Random().nextLong()).generate();
								worlds.put("GENTEST", genWorld);
								return true;
							} else {
								Logger.logMessage(Logger.ERROR, "The save '%s' is incompatible with the current registry!", getName());
							}
						}
					}
				} else {
					Logger.logMessage(Logger.ERROR, "The save '%s' is missing an info.json file!", getName());
				}
			} else {
				Logger.logMessage(Logger.ERROR, "The directory '%s' does not exist!", saveDirectory);
			}
		} catch (Exception e) {
			Logger.logException("Failed to load univserse from '%s'!", e, saveDirectory);
		}
		return false;
	}
	
	public void save() {
		
	}
	
	public File getDirectory() {
		return saveDirectory;
	}
	
	public void setDirectory(File newDirectory) {
		saveDirectory = newDirectory;
	}
	
	@Override
	public String getName() {
		return info.getOrDefault("name", saveDirectory.getName());
	}
	
	@Override
	public void close() {
		super.close();
		// TODO save worlds
	}

}
