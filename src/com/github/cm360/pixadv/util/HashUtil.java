package com.github.cm360.pixadv.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashUtil {

	public static String getSHA256Hash(String input) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.reset();
			md.update(input.getBytes(StandardCharsets.UTF_8));
			return bytesToHex(md.digest());
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	// Modified from https://www.baeldung.com/sha-256-hashing-java
	public static String bytesToHex(byte[] input) {
		StringBuilder hexString = new StringBuilder(2 * input.length);
		for (int i = 0; i < input.length; i++) {
			String hex = Integer.toHexString(0xff & input[i]);
			if (hex.length() == 1)
				hexString.append('0');
			hexString.append(hex);
		}
		return hexString.toString();
	}

}
