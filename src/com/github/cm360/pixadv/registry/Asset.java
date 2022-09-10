package com.github.cm360.pixadv.registry;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.stream.IntStream;

public class Asset {

	public enum AssetType { TEXTURE, SOUND, TRANSLATION, FONT };
	private static String[] extensions = {"png", "wav", "lang", "ttf"};
	private static final HashMap<String, AssetType> extensionMap = new HashMap<String, AssetType>();
	
	private byte[] bytes;
	private AssetType type;
	private Identifier id;
	
	public Asset(AssetType type, Identifier id, InputStream stream) throws IOException {
		this.type = type;
		this.id = id;
		// Copy asset contents to local byte array
		ByteArrayOutputStream bufferStream = new ByteArrayOutputStream();
		byte[] buffer = new byte[8192];
		int length;
		while ((length = stream.read(buffer)) > 0)
			bufferStream.write(buffer, 0, length);
		bytes = bufferStream.toByteArray();
	}

	public byte[] getBytes() {
		return bytes;
	}

	public AssetType getType() {
		return type;
	}

	public Identifier getId() {
		return id;
	}
	
	public static AssetType getTypeByExtension(String extension) {
		return extensionMap.get(extension.toLowerCase());
	}
	
	static {
		AssetType[] types = AssetType.values();
		IntStream.range(0, types.length).forEach(i -> extensionMap.put(extensions[i], types[i]));
	}

}
