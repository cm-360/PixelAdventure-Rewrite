package com.github.cm360.pixadv.util;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

	public static String extensionSeparator = ".";
	
	public static List<File> listFiles(File dir, FileFilter filter, boolean recursive) {
		List<File> results = new ArrayList<File>();
		File[] found = dir.listFiles();
		if (found != null)
			for (File f : found) {
				if (f.isDirectory() && recursive)
					results.addAll(listFiles(f, filter, true));
				else if (filter.accept(f))
					results.add(f);
			}
		return results;
	}
	
	public static String getExtension(String filename) {
		int index = filename.lastIndexOf(extensionSeparator);
		if (index == -1)
			return "";
		else
			return filename.substring(index + extensionSeparator.length());
	}
	
	public static String removeExtension(String filename) {
		int index = filename.lastIndexOf(extensionSeparator);
		if (index == -1)
			return filename;
		else
			return filename.substring(0, index);
	}
	
}
