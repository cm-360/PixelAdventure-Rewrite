package com.github.cm360.pixadv.network.packets.authentication;

import com.github.cm360.pixadv.network.Packet;
import com.github.cm360.pixadv.util.HashUtil;

public class LoginAttemptPacket implements Packet {

	private String username;
	private String token;
	private String salt;
	
	public LoginAttemptPacket() {
		// For deserialization
	}
	
	public LoginAttemptPacket(String username, String password) {
		String salt = HashUtil.getSHA256Hash(Long.toUnsignedString(System.currentTimeMillis(), 16));
		// Set packet fields
		this.username = username;
		this.token = HashUtil.getSHA256Hash(HashUtil.getSHA256Hash(password) + salt);
		this.salt = salt;
	}
	
	public String getUsername() {
		return username;
	}

	public String getToken() {
		return token;
	}
	
	public String getSalt() {
		return salt;
	}
	
	@Override
	public String serialize() {
		return String.format("%s:%s:%s", username, token, salt);
	}

	@Override
	public void deserialize(String data) {
		String[] split = data.split(":");
		if (split.length == 3) {
			username = split[0];
			token = split[1];
			salt = split[2];
		} else {
			username = "Invalid";
			token = "Invalid";
			salt = "Invalid";
		}
	}

}
