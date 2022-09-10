package com.github.cm360.pixadv.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class TextUtil {

	public static String read(File f) {
		StringBuilder su = new StringBuilder();
		BufferedReader br = null;
		try {
			String text;
			br = new BufferedReader(new FileReader(f));
			while ((text = br.readLine()) != null)
				su.append(text + "\n");
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return su.toString();
	}
	
	public static boolean write(File f, String s) {
		BufferedWriter bw = null;
		try {
			bw = new BufferedWriter(new FileWriter(f));
			bw.write(s);
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		} finally {
			try {
				bw.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return true;
	}

}
