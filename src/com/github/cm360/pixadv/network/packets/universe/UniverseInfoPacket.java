package com.github.cm360.pixadv.network.packets.universe;

import com.github.cm360.pixadv.network.Packet;

public class UniverseInfoPacket implements Packet {

	private String resig;
	private String name;
	private String[] worlds;
	
	public UniverseInfoPacket() {
		// For deserialization
	}
	
	public UniverseInfoPacket(String resig, String name, String[] worlds) {
		// Set packet fields
		this.resig = resig;
		this.name = name;
		this.worlds = worlds;
	}
	
	public String getRegistrySignature() {
		return resig;
	}

	public String getName() {
		return name;
	}
	
	public String[] getWorlds() {
		return worlds;
	}
	
	@Override
	public String serialize() {
		return String.format("%s:%s:%s", resig, name, String.join(",", worlds));
	}

	@Override
	public void deserialize(String data) {
		String[] split = data.split(":");
		if (split.length == 3) {
			resig = split[0];
			name = split[1];
			worlds = split[2].split(",");
		} else {
			resig = "Invalid";
			name = "Invalid";
			worlds = new String[] {};
		}
	}

}
