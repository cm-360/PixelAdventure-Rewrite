package com.github.cm360.pixadv.registry;

import java.awt.Font;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.swing.ImageIcon;

import com.github.cm360.pixadv.registry.Asset.AssetType;
import com.github.cm360.pixadv.util.FileUtil;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.types.entities.Entity;
import com.github.cm360.pixadv.world.types.tiles.Tile;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Registry {

	private boolean initialized;
	
	private Map<String, Module> loadedModules;
	
	private Map<Identifier, BufferedImage> textures;
	private Map<Identifier, ByteBuffer> textureData;
	private Map<Identifier, ByteBuffer> sounds;
	private Map<Identifier, Font> fonts;
	
	private Map<Identifier, Class<? extends Tile>> tiles;
	private Map<Identifier, Class<? extends Entity>> entities;
	
	
	public Registry() {
		initialized = false;
	}
	
	public void initialize(File rootDirectory) throws RegistryException {
		if (initialized) {
			throw new RegistryException("Registry has already been initialized!");
		} else {
			Logger.logMessage(Logger.INFO, "Building registry...");
			long start = System.currentTimeMillis();
			// Initialize maps
			loadedModules = new HashMap<String, Module>();
			textures = new HashMap<Identifier, BufferedImage>();
			textureData = new HashMap<Identifier, ByteBuffer>();
			sounds = new HashMap<Identifier, ByteBuffer>();
			fonts = new HashMap<Identifier, Font>();
			tiles = new HashMap<Identifier, Class<? extends Tile>>();
			entities = new HashMap<Identifier, Class<? extends Entity>>();
			// Load builtin module first
			try {
				Map<String, Object> builtinModuleInfo = new Gson().fromJson(
						new BufferedReader(new InputStreamReader(getClass().getResourceAsStream("/module.json"))),
						TypeToken.getParameterized(Map.class, String.class, Object.class).getType());
				Module builtinModule = new Module(builtinModuleInfo, getClass().getProtectionDomain().getCodeSource().getLocation());
				loadModule(builtinModule);
			} catch (Exception e) {
				throw new RegistryException("Failed to load built-in module!", e);
			}
			// Find external modules
			File modulesDirectory = new File(rootDirectory, "modules");
			if (modulesDirectory.exists() && modulesDirectory.isDirectory()) {
				List<Module> discoveredModules = discoverModules(modulesDirectory);
				for (Module module : discoveredModules) {
					try {
						loadModule(module);
					} catch (Exception e) {
						throw new RegistryException(String.format("Failed to load external module '%s'!", module.getName()), e);
					}
				}
			} else {
				// Modules directory does not exist
				Logger.logMessage(Logger.WARNING, "External modules directory '%s' does not exist!", modulesDirectory.getPath());
			}
			Logger.logMessage(Logger.INFO, "Finished building registry in %dms", System.currentTimeMillis() - start);
			initialized = true;
			synchronized (this) {
				this.notifyAll();
			}
		}
	}
	
	private List<Module> discoverModules(File directory) {
		List<Module> modules = new ArrayList<Module>();
		FileUtil.listFiles(directory, file -> file.getName().endsWith(".jar"), true).stream().forEach(jarFile -> {
			try (ZipFile jarFileZip = new ZipFile(jarFile)) {
				ZipEntry moduleInfoEntry = jarFileZip.getEntry("module.json");
				if (moduleInfoEntry == null || moduleInfoEntry.isDirectory()) {
					// module.json does not exist or is a directory
					Logger.logMessage(Logger.ERROR, "Module '%s' is missing a module information file!", jarFile.getPath());
				} else {
					// Read module.json
					Map<String, Object> moduleInfo = new Gson().fromJson(
							new InputStreamReader(jarFileZip.getInputStream(moduleInfoEntry)),
							TypeToken.getParameterized(Map.class, String.class, Object.class).getType());
					Module module = new Module(moduleInfo, jarFile.toURI().toURL());
					modules.add(module);
					Logger.logMessage(Logger.INFO, "Discovered module '%s'", module.getName());
				}
			} catch (Exception e) {
				// TODO catch different exception types
				Logger.logException("'%s' is not a valid module!", e, jarFile.getPath());
			}
		});
		return modules;
	}
	
	private void loadModule(Module module) throws Exception {
		Logger.logMessage(Logger.INFO, "Loading module '%s'...", module.getName());
		String moduleId = module.getId();
		// Create a class loader for the provided module
		try (URLClassLoader ucl = new URLClassLoader(new URL[] { module.getLocation() }, ClassLoader.getSystemClassLoader())) {
			// Load content from each of this modules providers
			for (String providerClassName : module.getProviders()) {
				Class<?> providerClass = ucl.loadClass(providerClassName);
				if (ModuleContentProvider.class.isAssignableFrom(providerClass)) {
					ModuleContentProvider provider = (ModuleContentProvider) providerClass.getDeclaredConstructor().newInstance();
					// Load assets from module
					List<Asset> assets = discoverAssets(module, provider, ucl);
					Logger.logMessage(Logger.INFO, "Discovered %d assets for module '%s'", assets.size(), module.getName());
					for (Asset asset : assets)
						loadAsset(module, asset);
					// Load tiles
					importNamespaced(moduleId, provider.getTiles(), tiles);
					// Load entities
					importNamespaced(moduleId, provider.getEntities(), entities);
				} else {
					
				}
			}
		}
		// Add module to list, prevent duplicates
		if (loadedModules.containsKey(moduleId))
			throw new RegistryException(String.format("Duplicate module ID '%s'!", moduleId));
		else
			loadedModules.put(moduleId, module);
	}
	
	private <T> void importNamespaced(String namespace, Map<String, T> rawMap, Map<Identifier, T> targetMap) {
		if (rawMap != null) {
			rawMap.keySet().forEach(key -> {
				Identifier namespacedId = new Identifier(namespace, key);
				targetMap.put(namespacedId, rawMap.get(key));
				Logger.logMessage(Logger.DEBUG, "Imported '%s'", namespacedId);
			});
		}
	}
	
	private List<Asset> discoverAssets(Module module, ModuleContentProvider provider, ClassLoader loader) throws Exception {
		List<Asset> assets = new ArrayList<Asset>();
		// Check if module is being loaded from a jar or directory
		String modulePath = loader.getResource(provider.getClass().getCanonicalName().replace('.', '/') + ".class").toURI().getPath();
		File moduleFile = new File(provider.getClass().getProtectionDomain().getCodeSource().getLocation().toURI());
		if (modulePath.startsWith("/")) {
			// Loading from directory
			for (String assetsSubDirectory : module.getAssetDirectories()) {
				File assetsDirectory = new File(moduleFile, assetsSubDirectory);
				String assetsDirectoryPath = assetsDirectory.getPath();
				int assetsFileTrim = assetsDirectoryPath.length() + File.separator.length();
				Logger.logMessage(Logger.DEBUG, "Loading assets from directory '%s'...", assetsDirectoryPath);
				List<File> assetFiles = FileUtil.listFiles(assetsDirectory, file -> file.isFile(), true);
				for (File assetFile : assetFiles) {
					// Get asset identifier and type from filename
					String assetName = assetFile.getPath().substring(assetsFileTrim).replace(File.separator, "/")
							.toLowerCase();
					Identifier assetId = new Identifier(module.getId(), FileUtil.removeExtension(assetName));
					// Create and cache asset object
					Asset asset = new Asset(Asset.getTypeByExtension(FileUtil.getExtension(assetName)), assetId,
							new FileInputStream(assetFile));
					assets.add(asset);
					Logger.logMessage(Logger.DEBUG, "Discovered asset '%s'", assetId);
				} 
			}
		} else {
			// Loading from compiled JAR
			Logger.logMessage(Logger.DEBUG, "Loading assets from JAR '%s'...", moduleFile.getPath());
			ZipFile moduleZip = new ZipFile(moduleFile);
			Enumeration<? extends ZipEntry> moduleZipEntries = moduleZip.entries();
			while (moduleZipEntries.hasMoreElements()) {
				ZipEntry moduleZipEntry = moduleZipEntries.nextElement();
				// Get asset identifier and type from filename
				String assetName = moduleZipEntry.getName().toLowerCase();
				Identifier assetId = new Identifier(module.getId(), FileUtil.removeExtension(assetName));
				// Create and cache asset object
				Asset asset = new Asset(Asset.getTypeByExtension(FileUtil.getExtension(assetName)), assetId, moduleZip.getInputStream(moduleZipEntry));
				assets.add(asset);
				Logger.logMessage(Logger.DEBUG, "Discovered asset '%s'", assetId);
			}
			moduleZip.close();
		}
		return assets;
	}
	
	private void loadAsset(Module module, Asset asset) throws RegistryException {
		try {
			AssetType type = asset.getType();
			if (type == null) {
				Logger.logMessage(Logger.WARNING, "Unknown type for asset '%s'!", asset.getId());
			} else {
				byte[] assetBytes = asset.getBytes();
				// Parse bytes by file type
				switch (type) {
				case TEXTURE:
					// Texture
					ImageIcon iconImage = new ImageIcon(assetBytes);
					BufferedImage bufferedImage = GraphicsEnvironment.getLocalGraphicsEnvironment()
							.getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(
									iconImage.getIconWidth(), iconImage.getIconHeight(), Transparency.TRANSLUCENT);
					Graphics bufferedImageGraphics = bufferedImage.getGraphics();
					bufferedImageGraphics.drawImage(iconImage.getImage(), 0, 0, null);
					bufferedImageGraphics.dispose();
					textures.put(asset.getId(), bufferedImage);
					ByteBuffer data = ByteBuffer.allocateDirect(assetBytes.length);
					data.put(assetBytes);
					textureData.put(asset.getId(), data.flip());
					break;
				case SOUND:
					// Sound
					sounds.put(asset.getId(), ByteBuffer.wrap(assetBytes));
					break;
				case TRANSLATION:
					// Translations file

					break;
				case FONT:
					// Font file
					Font font = Font.createFont(Font.TRUETYPE_FONT, new ByteArrayInputStream(assetBytes));
					fonts.put(asset.getId(), font);
					break;
				}
				Logger.logMessage(Logger.DEBUG, "Loaded asset '%s'", asset.getId());
			}
		} catch (Exception e) {
			throw new RegistryException(String.format("Failed to load asset '%s'!", asset.getId()), e);
		}
	}
	
	private void loadTexture() {
		
	}
	
	public BufferedImage getTexture(Identifier id) {
		return textures.get(id);
	}
	
	public ByteBuffer getTextureBytes(Identifier id) {
		return textureData.get(id);
	}
	
	public ByteBuffer getSound(Identifier id) {
		return sounds.get(id);
	}
	
	public Font getFont(Identifier id) {
		return fonts.get(id);
	}
	
	public Tile getTile(Identifier id) {
		try {
			return tiles.get(id).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			Logger.logException("Failed to create tile %s!", e, id);
		}
		return null;
	}

	public Entity getEntity(Identifier id) {
		try {
			return entities.get(id).getDeclaredConstructor().newInstance();
		} catch (Exception e) {
			Logger.logException("Failed to create entity %s!", e, id);
		}
		return null;
	}
	
	public Map<String, Module> getModulesList() {
		return loadedModules;
	}
	
	public String getSignature() {
		return String.join(",",
				loadedModules.values().stream().map(module -> String.format("%s@%s", module.getId(), module.getVersion()))
						.collect(Collectors.toList()));
	}
	
	public boolean compatibleWith(String signature) {
		// Get modules from input signature
		String[] sigModules = signature.split(",");
		for (String sigModule : sigModules) {
			String[] sigModuleSplit = sigModule.split("@", 2);
			if (!loadedModules.containsKey(sigModuleSplit[0])) {
				// TODO check versions
				return false;
			}
		}
		return true;
	}
	
	public boolean isInitialized() {
		return initialized;
	}
	
	@Override
	public String toString() {
		return getSignature();
	}

}
