package com.github.cm360.pixadv.network.endpoints;

import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;

import com.github.cm360.pixadv.config.Config;
import com.github.cm360.pixadv.config.ConfigProvider;
import com.github.cm360.pixadv.graphics.ClientWindow;
import com.github.cm360.pixadv.graphics.picasso.Picasso;
import com.github.cm360.pixadv.graphics.swing.components.GameCanvas;
import com.github.cm360.pixadv.graphics.swing.frames.ClientFrame;
import com.github.cm360.pixadv.network.Connection;
import com.github.cm360.pixadv.network.packets.authentication.LoginAttemptPacket;
import com.github.cm360.pixadv.network.packets.authentication.LoginResponsePacket;
import com.github.cm360.pixadv.network.packets.universe.UniverseInfoPacket;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.sound.beethoven.Beethoven;
import com.github.cm360.pixadv.tasks.TaskQueue;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.world.storage.universe.LocalUniverse;
import com.github.cm360.pixadv.world.storage.universe.RemoteUniverse;
import com.github.cm360.pixadv.world.storage.universe.Universe;

public class Client implements ConfigProvider {

	private final Registry registry;
	private Picasso picasso;
	private Beethoven beethoven;
	private TaskQueue taskQueue;
	private Thread tasksThread;
	
	private GraphicsEnvironment graphicsEnv;
	private ClientFrame frame;
	private Rectangle framePosition;
	private boolean fullscreen;
	private GraphicsDevice fullscreenDevice;
	
	private Universe loadedUniverse;
	private UUID playerId;
	private UUID cameraFollowedId;
	private Set<UUID> controlledIds;
	
	private boolean paused;
	
//	public Client(Registry registry) throws Exception {
//		this(registry, "");
//		this.registry = registry;
//		registry.initialize(new File("."));
//		ClientWindow clientWindow = new ClientWindow(registry);
//		clientWindow.run();
//	}
	
	public Client(Registry registry) throws Exception {
		this.registry = registry;
		picasso = new Picasso(this);
		beethoven = new Beethoven(this);
		taskQueue = new TaskQueue();
		fullscreen = false;
		// Hardware acceleration
		System.setProperty("sun.java2d.opengl", "true");
		// Create client frame
		EventQueue.invokeLater(() -> {
			graphicsEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
			GameCanvas gamePanel = new GameCanvas(this);
			frame = new ClientFrame(gamePanel);
			frame.setVisible(true);
			gamePanel.grabFocus();
		});
		// Start task manager
		taskQueue = new TaskQueue();
		tasksThread = new Thread(() -> {
			while (!Thread.interrupted())
				taskQueue.processTasks();
			Logger.logMessage(Logger.DEBUG, "Stopped task queue manager");
		}, "TaskQueue");
		tasksThread.start();
		//
		playerId = UUID.randomUUID();
		cameraFollowedId = playerId;
		controlledIds = new HashSet<UUID>();
		controlledIds.add(playerId);
		// 
		paused = false;
	}
	
	/**
	 * Loads a local universe from the specified directory
	 * @param directory The directory to load from.
	 * @return True if the universe was loaded successfully, false otherwise.
	 */
	public boolean load(File directory) {
		try {
			LocalUniverse newUniverse = new LocalUniverse(registry, directory, this::getPlayerId);
			loadedUniverse = newUniverse;
			return newUniverse.load();
		} catch (Exception e) {
			Logger.logException("Failed to load to '%s'", e, directory);
		}
		return false;
	}
	
	/**
	 * Connects to a universe at the specified address.
	 * @param directory The address to connect to.
	 * @return True if the universe was connected successfully, false otherwise.
	 */
	public boolean connect(String address, int port) {
		Logger.logMessage(Logger.INFO, "Connecting to '%s:%d'...", address, port);
		try {
			// Connect to remote server
			Connection connection = new Connection(new Socket(address, port));
			// Perform handshake
			Logger.logMessage(Logger.DEBUG, "Performing handshake...");
			connection.send(new LoginAttemptPacket("Player1", "password1"));
			LoginResponsePacket loginResponse = (LoginResponsePacket) connection.await(LoginResponsePacket.class, 10000);
			if (loginResponse == null) {
				Logger.logMessage(Logger.ERROR, "Handshake response timeout!");
			} else {
				Logger.logMessage(Logger.DEBUG, "Handshake successful!");
				//
				UniverseInfoPacket universeInfo = (UniverseInfoPacket) connection.await(UniverseInfoPacket.class, 10000);
				if (universeInfo == null) {
					Logger.logMessage(Logger.ERROR, "Universe information response timeout!");
				} else {
					loadedUniverse = new RemoteUniverse(registry, connection, universeInfo, this::getPlayerId);
					return true;
				}
			}
			//connection.send(new TestPacket("hello from client 1\0com.github.cm360.pixadv.network.packets.TestPacket;inject good"));
			//connection.send(new TestPacket("hello from client 2\0com.github.cm360.pixadv.network.packets.FakePacket;inject bad"));
		} catch (IOException e) {
			String message = String.format("Failed to connect to '%s:%d'", address, port);
			Logger.logException(message, e);
		}
		closeUniverse();
		return false;
	}
	
	public void toggleFullscreen(boolean windowed) {
	    if (!graphicsEnv.isHeadlessInstance()) {
			if (fullscreen) {
				// Exit fullscreen (but don't touch other fullscreen apps!)
				if (fullscreenDevice != null && fullscreenDevice.getFullScreenWindow() == frame) {
					fullscreenDevice.setFullScreenWindow(null);
				}
				frame.disposeNoClose();
				frame.setUndecorated(false);
				frame.setVisible(true);
				restoreFramePosition();
				fullscreen = false;
			} else {
				framePosition = new Rectangle(frame.getX(), frame.getY(), frame.getWidth(), frame.getHeight());
				frame.disposeNoClose();
				frame.setUndecorated(true);
				if (windowed) {
					frame.setExtendedState(Frame.MAXIMIZED_BOTH);
					frame.setVisible(true);
				} else {
					fullscreenDevice = getBestFullscreenDevice();
					fullscreenDevice.setFullScreenWindow(frame);
				}
				fullscreen = true;
			}
		}
	}
	
	public boolean isFrameFullscreen() {
		return fullscreen;
	}
	
	private GraphicsDevice getBestFullscreenDevice() {
		Map<GraphicsDevice, Rectangle> intersections = new HashMap<GraphicsDevice, Rectangle>();
		for (GraphicsDevice gDevice : graphicsEnv.getScreenDevices()) {
			if (gDevice.isFullScreenSupported()) {
				// Record how much of the window is visible on each graphics device
				Rectangle intersection = gDevice.getDefaultConfiguration().getBounds().intersection(framePosition);
				if (!intersection.isEmpty()) {
					intersections.put(gDevice, intersection);
				}
			}
		}
		if (intersections.isEmpty()) {
			// The frame is visible on no graphics devices (graphics environment has likely changed since fullscreen was enabled)
			return graphicsEnv.getDefaultScreenDevice();
		} else {
			int bestArea = 0;
			GraphicsDevice bestDevice = null;
			for (Entry<GraphicsDevice, Rectangle> entry : intersections.entrySet()) {
				Rectangle intersection = entry.getValue();
				int area = intersection.width * intersection.height;
				if (area > bestArea) {
					bestArea = area;
					bestDevice = entry.getKey();
				}
			}
			return bestDevice;
		}
	}
	
	private void restoreFramePosition() {
		if (framePosition != null) {
			frame.setBounds(framePosition);
		} else {
			frame.setBounds(new Rectangle(100, 100, 600, 400));
		}
	}
	
	public ClientFrame getClientFrame() {
		return frame;
	}
	
	public GameCanvas getGamePanel() {
		return frame.getGameCanvas();
	}
	
	public Registry getRegistry() {
		return registry;
	}
	
	public Config getConfig() {
		// TODO get config
		return null;
	}
	
	public Picasso getRenderingEngine() {
		return picasso;
	}
	
	public Beethoven getSoundEngine() {
		return beethoven;
	}
	
	public TaskQueue getTaskQueueManager() {
		return taskQueue;
	}
	
	public Universe getCurrentUniverse() {
		return loadedUniverse;
	}
	
	public UUID getPlayerId() {
		return playerId;
	}
	
	public UUID getCameraFollowedId() {
		return cameraFollowedId;
	}
	
	
	public Set<UUID> getControlledIds() {
		return Set.copyOf(controlledIds);
	}
	
	public void resetTaskQueue() {
		tasksThread.interrupt();
		taskQueue.clear();
	}
	
	public boolean isPaused() {
		return paused;
	}
	
	public void setPaused(boolean paused) {
		this.paused = paused;
		notifyAll();
	}
	
	public void closeUniverse() {
		picasso.clearCache();
		if (loadedUniverse != null) {
			try {
				// Close current universe
				loadedUniverse.close();
			} catch (Exception e) {
				Logger.logException("Failed to safely close universe '%s'!", e, loadedUniverse.getName());
			}
			loadedUniverse = null;
		}
		System.gc();
	}

}
