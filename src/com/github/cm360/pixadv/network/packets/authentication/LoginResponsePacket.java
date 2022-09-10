package com.github.cm360.pixadv.network.packets.authentication;

import com.github.cm360.pixadv.network.Packet;

public class LoginResponsePacket implements Packet {

	private boolean accepted;
	private String message;
	
	public LoginResponsePacket() {
		// For deserialization
	}
	
	public LoginResponsePacket(boolean accepted, String message) {
		this.accepted = accepted;
		this.message = message;
	}
	
	@Override
	public String serialize() {
		return accepted ? message : "failed";
	}

	@Override
	public void deserialize(String data) {
		; // TODO deserialize login response
	}

}
