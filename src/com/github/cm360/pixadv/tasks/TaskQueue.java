package com.github.cm360.pixadv.tasks;

import java.util.HashMap;
import java.util.Map;

import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.tasks.types.ChunkLoadRequest;
import com.github.cm360.pixadv.tasks.types.ChunkRepaintTask;
import com.github.cm360.pixadv.tasks.types.Task;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.world.World;

public class TaskQueue {

	protected Map<String, Task> requests;
	
	public TaskQueue() {
		this.requests = new HashMap<String, Task>();
	}
	
	public synchronized void processTasks() {
		for (String requestId : requests.keySet()) {
			requests.get(requestId).process();
			Logger.logMessage(Logger.DEBUG, "Finished task %s", requestId);
		}
		requests.clear();
	}
	
	public synchronized void repaintChunk(Client client, World world, int cx, int cy) {
		String requestId = String.format("repaintChunk_%s_%s-%s", world, cx, cy);
		Logger.logMessage(Logger.DEBUG, requestId);
		if (!requests.containsKey(requestId))
			requests.put(requestId, new ChunkRepaintTask(client, world, cx, cy));
	}
	
	public synchronized void requestChunk(Registry registry, World world, int cx, int cy) {
		String requestId = String.format("requestChunk_%d_%s-%s", world.hashCode(), cx, cy);
		Logger.logMessage(Logger.DEBUG, requestId);
		if (!requests.containsKey(requestId))
			requests.put(requestId, new ChunkLoadRequest(registry, world, cx, cy));
	}
	
	public synchronized void addGenericTask(Task task) {
		String requestId = String.format("genericTask_%d", task.hashCode());
		Logger.logMessage(Logger.DEBUG, requestId);
		if (!requests.containsKey(requestId))
			requests.put(requestId, task);
	}
	
	public synchronized void clear() {
		requests.clear();
	}

}
