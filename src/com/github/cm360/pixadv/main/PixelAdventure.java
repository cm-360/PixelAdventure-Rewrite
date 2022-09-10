package com.github.cm360.pixadv.main;

import java.io.File;

import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.network.endpoints.Server;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.Logger;

public class PixelAdventure {

	private static File workingDirectory = new File("data");
	
	public static void main(String[] args) {
		try {
			// Create registry
			Registry registry = new Registry();
			// Start game
			if (args.length > 0 && args[0].equals("server")) {
				new Server(registry, new File(workingDirectory, "saves/Universe Zero"), "", 43234);
			} else {
				new Client(registry);
			}
			// Build registry after client
			registry.initialize(workingDirectory);
		} catch (Exception e) {
			Logger.logException("Uncaught exception!", e);
		}
	}
	
	public static File getWorkingDirectory() {
		return workingDirectory;
	}

}
