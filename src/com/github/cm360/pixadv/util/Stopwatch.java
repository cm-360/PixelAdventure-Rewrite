package com.github.cm360.pixadv.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

public class Stopwatch {

	private LinkedHashMap<String, Long> times;
	private long startTime;
	private long lastTime;
	private long totalDuration;
	
	public Stopwatch() {
		times = new LinkedHashMap<String, Long>();
		startTime = System.nanoTime();
		lastTime = startTime;
	}
	
	public synchronized void mark(String name) {
		long currentTime = System.nanoTime();
		times.put(name, currentTime - lastTime);
		lastTime = currentTime;
		totalDuration = lastTime - startTime;
	}
	
	public Set<Entry<String, Long>> getTimes() {
		return times.entrySet();
	}
	
	public long getTotalDuration() {
		return totalDuration;
	}

}
