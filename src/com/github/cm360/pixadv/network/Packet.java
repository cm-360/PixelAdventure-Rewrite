package com.github.cm360.pixadv.network;

public interface Packet {

	public String serialize();
	
	public void deserialize(String data);

}
