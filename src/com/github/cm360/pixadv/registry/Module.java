package com.github.cm360.pixadv.registry;

import java.net.URL;
import java.util.List;
import java.util.Map;

public class Module {

	private URL location;
	
	private List<String> providers;
	private List<String> assetDirs;
	
	private String name;
	private String id;
	private String version;
	private String author;
	private List<String> description;
	
	@SuppressWarnings("unchecked")
	public Module(Map<?, ?> info, URL moduleLocation) {
		// Retrieve module loading data
		location = moduleLocation;
		providers = (List<String>) info.get("providers");
		assetDirs = (List<String>) info.get("assets");
		// Retrieve metadata
		name = info.get("name").toString();
		id = info.get("id").toString();
		version = info.get("version").toString();
		author = info.get("author").toString();
		description = (List<String>) info.get("description");
	}
	
	public URL getLocation() {
		return location;
	}
	
	public List<String> getProviders() {
		return providers;
	}
	
	public List<String> getAssetDirectories() {
		return assetDirs;
	}
	
	public String getName() {
		return name;
	}

	public String getId() {
		return id;
	}

	public String getVersion() {
		return version;
	}

	public String getAuthor() {
		return author;
	}

	public List<String> getDescription() {
		return description;
	}

}
