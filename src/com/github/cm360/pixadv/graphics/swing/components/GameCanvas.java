package com.github.cm360.pixadv.graphics.swing.components;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.Timer;

import com.github.cm360.pixadv.builtin.pixadv.java.entities.capabilities.ControllableEntity.Input;
import com.github.cm360.pixadv.builtin.pixadv.java.gui.huds.ChatHud;
import com.github.cm360.pixadv.builtin.pixadv.java.gui.menus.StartMenu;
import com.github.cm360.pixadv.builtin.pixadv.java.tiles.types.terra.Dirt;
import com.github.cm360.pixadv.graphics.gui.GuiComponent;
import com.github.cm360.pixadv.graphics.gui.input.InputProcessor;
import com.github.cm360.pixadv.graphics.gui.input.KeyCombo;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiLayer;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiMenu;
import com.github.cm360.pixadv.graphics.picasso.Picasso;
import com.github.cm360.pixadv.graphics.picasso.Precompute;
import com.github.cm360.pixadv.graphics.picasso.RenderStats;
import com.github.cm360.pixadv.main.PixelAdventure;
import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.util.Logger;
import com.github.cm360.pixadv.util.Stopwatch;
import com.github.cm360.pixadv.world.storage.universe.Universe;
import com.github.cm360.pixadv.world.storage.world.World;
import com.github.cm360.pixadv.world.types.tiles.Tile;

public class GameCanvas extends JComponent {

	private static final long serialVersionUID = 1L;
	
	private final Client client;
	
	private File screenshotsDir;
	private DateTimeFormatter screenshotNameFormatter;
	
	private Font defaultFont;
	
	private Point mouseLocation;
	private Point mouseClickOrigin;
	private Set<Integer> pressedKeys;
	private Map<Integer, Input> inputMappings;
	
	private double cameraXOld, cameraYOld;
	
	private Stack<GuiMenu> menuStack;
	private List<GuiLayer> guiLayers;
	private Precompute lastPrecomp;
	
	public boolean showUI = true;
	public boolean showFps = true;
	public boolean showDebugMenu = true;
	private Map<String, Color> debugPieColors;
	
	private int fps;
	private int frames;
	private long framesResetTime;
	private int frameCap = 0;
	
	private String lastExceptionText = "";
	private long lastExceptionTime = -1;
	
	private List<String> chatMessageHistory;
	private List<String> chatSentHistory;

	public GameCanvas(Client client) {
		this.client = client;
		//
		defaultFont = new Font(Font.SANS_SERIF, Font.BOLD, 20);
		screenshotsDir = new File(PixelAdventure.getWorkingDirectory(), "screenshots");
		screenshotNameFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS");
		//
		debugPieColors = new HashMap<String, Color>();
		debugPieColors.put("precompute", Color.YELLOW);
		debugPieColors.put("clear", Color.RED);
		debugPieColors.put("entity-positions", new Color(0, 127, 127));
		debugPieColors.put("background", new Color(64, 64, 255));
		debugPieColors.put("chunkmap", new Color(0, 192, 0));
		debugPieColors.put("entities", Color.ORANGE);
		debugPieColors.put("mouse", Color.GRAY);
		debugPieColors.put("ui-layers", Color.PINK);
		debugPieColors.put("ui-menu", Color.MAGENTA);
		debugPieColors.put("overlay", Color.CYAN);
		//
		menuStack = new Stack<GuiMenu>();
		guiLayers = new LinkedList<GuiLayer>();
		// Chat
		chatMessageHistory = new ArrayList<String>();
		chatSentHistory = new ArrayList<String>();
		
		pressedKeys = new LinkedHashSet<Integer>();
		inputMappings = new HashMap<Integer, Input>();
		inputMappings.put(KeyEvent.VK_W, Input.UP);
		inputMappings.put(KeyEvent.VK_S, Input.DOWN);
		inputMappings.put(KeyEvent.VK_A, Input.LEFT);
		inputMappings.put(KeyEvent.VK_D, Input.RIGHT);
		inputMappings.put(KeyEvent.VK_SPACE, Input.JUMP);
		// Repaint loop
		new Timer(0, event -> repaint()).start();
		// New thread to wait for registry to be built
		new Thread(() -> {
			try {
				Registry registry = client.getRegistry();
				synchronized (registry) {
					if (!registry.isInitialized())
						registry.wait();
					// Set default font
					Font font = client.getRegistry().getFont(Identifier.parse("pixadv:fonts/Style-7/PixelFont7"));
					if (font != null)
						defaultFont = font;
					// Finish loading game panel
					EventQueue.invokeLater(() -> {
						postRegistryInit();
					});
				}
			} catch (InterruptedException e) {
				Logger.logException("Interrupted!", e);
			}
		}, "GamePanel-PostRegistry").start();
	}
	
	private void postRegistryInit() {
		// For access inside lambda expressions
		GameCanvas self = this;
		// Open start menu
		menuStack.clear();
		menuStack.push(new StartMenu(client));
		// Mouse events
		addMouseListener(new MouseAdapter() {
			@Override
			public void mousePressed(MouseEvent arg0) {
				// Save current mouse info
				mouseClickOrigin = arg0.getPoint();
				mouseLocation = mouseClickOrigin;
				Universe universe = client.getCurrentUniverse();
				if (universe != null) {
					World world = universe.getCurrentWorld();
					if (world != null) {
						cameraXOld = world.getCameraX();
						cameraYOld = world.getCameraY();
					}
				}
				// Process gui interaction
				GuiLayer gui = getTopGui();
				if (gui == null) {
					if (arg0.getButton() == 3)
						interact();
				} else {
					// Attempt to focus component
					InputProcessor inputProcessor = gui.getInputProcessor();
					inputProcessor.mousePressed(mouseLocation, new KeyCombo(arg0.getButton(), getPressedKeys()));
					GuiComponent focused = inputProcessor.getFocusedComponent();
					if (focused == null && arg0.getButton() == 3)
						interact();
				}
			}
			@Override
			public void mouseReleased(MouseEvent arg0) {
				// Save current mouse info
				mouseLocation = arg0.getPoint();
				// Process gui interaction
				GuiLayer gui = getTopGui();
				if (gui != null)
					gui.getInputProcessor().mouseReleased(mouseLocation, new KeyCombo(arg0.getButton(), getPressedKeys()));
			}
		});
		addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent arg0) {
				// Save current mouse info
				mouseLocation = arg0.getPoint();
				// Process gui interaction
				GuiLayer gui = getTopGui();
				if (gui != null)
					gui.getInputProcessor().mouseMoved(mouseLocation, new KeyCombo(-1, getPressedKeys()));
				if (getCurrentMenu() == null) {
					updateControlledInputs();
				}
			}
			@Override
			public void mouseDragged(MouseEvent arg0) {
				// Save current mouse info
				mouseLocation = arg0.getPoint();
				// Process gui interaction
				GuiLayer gui = getTopGui();
				if (gui != null)
					gui.getInputProcessor().mouseMoved(mouseLocation, new KeyCombo(arg0.getButton(), getPressedKeys()));
				// Scroll camera
				Universe universe = client.getCurrentUniverse();
				if (universe != null) {
					World world = universe.getCurrentWorld();
					if (world != null) {
						Picasso picasso = client.getRenderingEngine();
						world.setCameraX(cameraXOld + ((mouseClickOrigin.getX() - mouseLocation.getX()) / (picasso.getTileTextureSize() * picasso.getTileScale())));
						world.setCameraY(cameraYOld + ((mouseLocation.getY() - mouseClickOrigin.getY()) / (picasso.getTileTextureSize() * picasso.getTileScale())));
					}
				}
			}
		});
		addMouseWheelListener(new MouseWheelListener() {
			public void mouseWheelMoved(MouseWheelEvent arg0) {
				Picasso picasso = client.getRenderingEngine();
				double scaledTextureSize = picasso.getTileTextureSize() * picasso.getTileScale();
				if (arg0.getWheelRotation() > 0 && scaledTextureSize > 16) {
					picasso.setTileScale(picasso.getTileScale() - 0.25);
				} else if (arg0.getWheelRotation() < 0 && scaledTextureSize < 32 * 5) {
					picasso.setTileScale(picasso.getTileScale() + 0.25);
				}
			}
		});
		// Key events
		setFocusable(true);
		addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent arg0) {
				synchronized (pressedKeys) {
					GuiMenu menu = getCurrentMenu();
					switch (arg0.getKeyCode()) {
					// Open pause menu
					case KeyEvent.VK_ESCAPE:
						if (menu != null) {
							if (!(client.getCurrentUniverse() == null && menu instanceof StartMenu && menuStack.size() == 1))
								closeMenu();
						} else {
							client.closeUniverse();
							openMenu(new StartMenu(client));
						}
						break;
					// Toggle UI
					case KeyEvent.VK_F1:
						showUI = !showUI;
						break;
					// Screenshot
					case KeyEvent.VK_F2:
						client.getTaskQueueManager().addGenericTask(() -> takeScreenshot(mouseLocation));
						break;
					// Toggle fullscreen
					case KeyEvent.VK_F11:
						client.toggleFullscreen(true);
						break;
					// Toggle debug menu
					case KeyEvent.VK_F12:
						showDebugMenu = !showDebugMenu;
						break;
					// Normal key press
					default:
						pressedKeys.add(arg0.getKeyCode());
						if (menu == null) {
							updateControlledInputs();
							// Handle additional special keypresses
							switch (arg0.getKeyCode()) {
							// Open chat UI
							case KeyEvent.VK_T:
								if (!(getCurrentMenu() instanceof ChatHud))
									openMenu(new ChatHud(client, self::closeMenu, chatMessageHistory, chatSentHistory));
								break;
							}
						} else {
							Set<Integer> keyWithModifiers = new HashSet<Integer>();
							keyWithModifiers.add(arg0.getKeyCode());
							keyWithModifiers.addAll(getPressedKeys().stream().filter(Character::isISOControl).toList());
							menu.interactKey(new KeyCombo(-1, keyWithModifiers));
						}
					}
				}
			}
			@Override
			public void keyReleased(KeyEvent arg0) {
				synchronized (pressedKeys) {
					pressedKeys.remove(arg0.getKeyCode());
					if (getCurrentMenu() == null) {
						updateControlledInputs();
					}
				}
			}
			@Override
			public void keyTyped(KeyEvent arg0) {
				// Do
			}
		});
	}
	
	private void updateControlledInputs() {
		Universe universe = client.getCurrentUniverse();
		if (universe != null) {
			World world = universe.getCurrentWorld();
			if (world != null) {
				Set<Input> inputDirections = getPressedKeys().stream()
						.map(keyCode -> inputMappings.get(keyCode))
						.collect(Collectors.toSet());
				world.getPhysicsEngine().updateControlInputs(
						client.getControlledIds(),
						inputDirections,
						mouseLocation);
			}
		}
	}
	
	private void interact() {
		client.getSoundEngine().playSound(Identifier.parse("pixadv:sounds/chop"));
		// Process click as block interaction instead
		Universe universe = client.getCurrentUniverse();
		if (universe != null) {
			World world = universe.getCurrentWorld();
			Picasso picasso = client.getRenderingEngine();
			Point rawPoint = picasso.getMouseTile(world, lastPrecomp, mouseLocation);
			Point correctedPoint = world.correctCoord(rawPoint.x, rawPoint.y);
			if (new Rectangle(world.getWidth() * world.getChunkSize(), world.getHeight() * world.getChunkSize()).contains(correctedPoint)) {
				Tile tile = world.getTile(correctedPoint.x, correctedPoint.y, 2);
				if (tile == null)
					world.setTile(new Dirt(), correctedPoint.x, correctedPoint.y, 2);
				else
					world.setTile(null, correctedPoint.x, correctedPoint.y, 2);
			}
		}
	}
	
	@Override
	public void paintComponent(Graphics g) {
		try {
			Stopwatch renderTimes = new Stopwatch();
			RenderStats stats = null;
			lastPrecomp = new Precompute(g);
			renderTimes.mark("precompute");
			// Clear screen and set default mode
//			g.clearRect(0, 0, lastPrecomp.getGBounds().width, lastPrecomp.getGBounds().height);
			g.setColor(Color.BLACK);
			g.fillRect(0, 0, lastPrecomp.getGBounds().width, lastPrecomp.getGBounds().height);
			renderTimes.mark("clear");
			g.setColor(Color.WHITE);
			g.setFont(getDefaultFont().deriveFont(20f));
			FontMetrics gfm = g.getFontMetrics();
			if (client.getRegistry().isInitialized()) {
				// Render current universe
				Universe universe = client.getCurrentUniverse();
				if (universe != null) {
					stats = client.getRenderingEngine().paint(g, universe.getCurrentWorld(), lastPrecomp, mouseLocation, renderTimes);
				} else {
					
				}
				if (showUI) {
					autoScale(lastPrecomp.getGBounds().width, lastPrecomp.getGBounds().height);
					// Render current GUI layers
					if (!guiLayers.isEmpty()) {
						// Draw all GUI layers
						for (GuiLayer layout : guiLayers) {
							layout.updateBounds(lastPrecomp.getGBounds());
							layout.paint(g, client.getRegistry());
						}
						renderTimes.mark("ui-layers");
					}
					// Draw current menu if any
					if (!menuStack.isEmpty()) {
						GuiMenu menu = getCurrentMenu();
						menu.updateBounds(lastPrecomp.getGBounds());
						menu.paint(g, client.getRegistry());
						renderTimes.mark("ui-menu");
					}
					if (menuStack.isEmpty() && guiLayers.isEmpty() && universe == null) {
						g.drawString("No content loaded", 5, gfm.getHeight());
					}
					paintOverlay(g, universe, lastPrecomp, renderTimes, stats);
				}
			} else {
				// Registry is not finished loading
				g.drawString("Building registry...", 5, gfm.getHeight());
			}
			// Delay next frame as to not pass the fps cap
			long frameTime = renderTimes.getTotalDuration();
			if (frameCap > 0) {
				int frameExpectedTime = 1000000000 / frameCap;
				if (frameTime < frameExpectedTime) {
					try {
						Thread.sleep((frameExpectedTime - frameTime) / 1000000);
					} catch (InterruptedException e) {
						Logger.logException("Interrupted!", e);
					}
				}
			}
			// Calculate FPS
			long frameFullTime = System.nanoTime();
			if (frameFullTime > framesResetTime + 1000000000) {
				fps = frames;
				frames = 0;
				framesResetTime = frameFullTime;
			}
			frames++;
		} catch (Exception e) {
			Logger.logException("Uncaught exception while rendering!", e);
			setLastExceptionInfo(e.getMessage(), System.nanoTime());
			g.setColor(Color.RED);
			g.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			g.drawString("%s: %s".formatted(e.getClass().getName(), e.getMessage()), 5, 15);
		}
	}
	
	protected void paintOverlay(Graphics g, Universe universe, Precompute precomp, Stopwatch renderTimes, RenderStats stats) {
		// Draw general debug info
		if (showDebugMenu) {
			paintDebugOverlay(g, universe, precomp, renderTimes, stats);
		} else {
			if (showFps) {
				// Draw FPS in corner
				g.setFont(client.getGamePanel().getDefaultFont().deriveFont(16f));
				FontMetrics gfm = g.getFontMetrics();
				g.setColor(Color.WHITE);
				g.drawString(client.getGamePanel().getFps() + " FPS", 5, gfm.getHeight());
			}
		}
	}
	
	protected void paintDebugOverlay(Graphics g, Universe universe, Precompute precomp, Stopwatch renderTimes, RenderStats stats) {
		paintCameraBounds(g, universe, precomp);
		// Left anchored text lines
		List<String> leftLines = new ArrayList<String>();
		leftLines.add(client.getGamePanel().getFps() + " FPS");
		if (universe == null) {
			leftLines.add("No universe loaded");
		} else {
			World world = universe.getCurrentWorld();
			if (world == null) {
				leftLines.add("Loading world...");
			} else {
				leftLines.add("World: '%s'".formatted(world.getName()));
				Point minChunk = world.getChunkOf(precomp.getMinX(), precomp.getMinY());
				Point maxChunk = world.getChunkOf(precomp.getMaxX(), precomp.getMaxY());
				leftLines.add("Tiles: (x%d,y%d)-(x%d,y%d)".formatted(
						precomp.getMinX(),
						precomp.getMinY(),
						precomp.getMaxX(),
						precomp.getMaxY()));
				leftLines.add("Chunks: (%d,%d)-(%d,%d)".formatted(
						minChunk.x,
						minChunk.y,
						maxChunk.x,
						maxChunk.y));
				leftLines.add("Entities: %d/%d".formatted(
						stats.getUniqueEntities(),
						stats.getTotalEntities()));
			}
		}
		// Right anchored text lines
		List<String> rightLines = new ArrayList<String>();
		rightLines.add("Heap: %d/%dMB".formatted(
				Runtime.getRuntime().totalMemory() / 1048576,
				Runtime.getRuntime().maxMemory() / 1048576));
		rightLines.add("Chunk Cache: " + client.getRenderingEngine().getCacheSize());
		rightLines.add("Modules: %d".formatted(client.getRegistry().getModulesList().size()));
		// Debug info bar
		g.setFont(client.getGamePanel().getDefaultFont().deriveFont(16f));
		FontMetrics gfm = g.getFontMetrics();
		g.setColor(new Color(255, 255, 255, 127));
		g.fillRect(0, 0, precomp.getGBounds().width, (gfm.getHeight() * Math.max(leftLines.size(), rightLines.size())) + 5);
		g.setColor(Color.BLACK);
		// Draw left anchored info lines
		for (int i = 0; i < leftLines.size(); i++) {
			String line = leftLines.get(i);
			if (!line.isBlank()) {
				g.drawString(line,
						5,
						gfm.getHeight() * (i + 1));
			}
		}
		// Draw right anchored info lines
		for (int i = 0; i < rightLines.size(); i++) {
			String line = rightLines.get(i);
			if (!line.isBlank()) {
				g.drawString(line,
						(precomp.getGBounds().width - (gfm.stringWidth(line) + 5)),
						gfm.getHeight() * (i + 1));
			}
		}
		// Exceptions while rendering
		if (lastExceptionTime != -1 && System.currentTimeMillis() - lastExceptionTime < 15000) {
			g.clearRect(0, precomp.getGBounds().height - 20, precomp.getGBounds().width, 20);
			g.setColor(Color.RED);
			g.drawString(lastExceptionText, 5, precomp.getGBounds().height - 5);
		}
//		paintTickChart(g, precomp.getGBounds());
		renderTimes.mark("overlay");
		paintDebugPie(g, precomp.getGBounds(), renderTimes);
	}
	
	protected void paintCameraBounds(Graphics g, Universe universe, Precompute precomp) {
		if (universe != null) {
			World world = universe.getCurrentWorld();
			if (world != null) {
				// Camera position and bounding box
				g.setColor(Color.WHITE);
				g.drawRect(
						precomp.getCamBounds().x + (precomp.getCamBounds().width / 2) - 1,
						precomp.getCamBounds().y + (precomp.getCamBounds().height / 2) - 1,
						2, 2);
				if (!precomp.getCamBounds().equals(precomp.getGBounds())) {
					g.drawRect(
							precomp.getCamBounds().x,
							precomp.getCamBounds().y,
							precomp.getCamBounds().width,
							precomp.getCamBounds().height);
				}
			}
		}
	}
	
	protected void paintDebugPie(Graphics g, Rectangle gBounds, Stopwatch renderTimes) {
		double totalRenderTime = renderTimes.getTotalDuration();
		// Pie chart variables
		int padding = 10;
		int size = (int) Math.round(100 * GuiLayer.scale);
		int border = 2;
		int angle = 0;
		int line = 0;
		// Render pie chart border
		g.setColor(Color.WHITE);
		g.fillArc(padding, gBounds.height - (padding + size), size, size, 0, 360);
		// Render pie chart slices
		g.setFont(new Font("Consolas", Font.BOLD, 16));
		FontMetrics gfm = g.getFontMetrics();
		int pieKeyHeight = Math.max(renderTimes.getTimes().size() * gfm.getHeight(), size + padding);
		for (Entry<String, Long> entry : renderTimes.getTimes()) {
			double percent = entry.getValue() / totalRenderTime;
			int angleDelta = (int) Math.round(360 * percent);
			if (debugPieColors.containsKey(entry.getKey())) {
				g.setColor(debugPieColors.get(entry.getKey()));
			} else {
				int color = new Random(entry.getKey().hashCode()).nextInt(255 * 255 * 255);
				g.setColor(new Color(color));
			}
			g.fillArc(
					padding + border,
					gBounds.height - (size + padding - border),
					size - (2 * border),
					size - (2 * border),
					angle, angleDelta);
			g.drawString("%7.3f%% %9dns  %s".formatted(
					percent * 100,
					entry.getValue(),
					entry.getKey()),
					(size + padding) + 20,
					(gBounds.height - pieKeyHeight + padding) + (gfm.getHeight() * line));
			angle += angleDelta;
			line++;
		}
	}
	
	protected void paintTickChart(Graphics g, Rectangle gBounds) {
		Universe universe = client.getCurrentUniverse();
		int width = 3;
		int baseHeight = 20;
		if (universe != null) {
			for (String worldName : universe.getWorldNames()) {
				Long[] physicsTickTimes = universe.getWorld(worldName).getPhysicsTickTimes();
				// Calculate tick average
				double average = 0;
				for (Long tick : physicsTickTimes)
					average += tick;
				average /= physicsTickTimes.length;
				// Draw a bar for each tick
				for (int i = 0; i < physicsTickTimes.length; i++) {
					long tick = physicsTickTimes[i];
					double percent = tick / average;
					int height = (int) Math.round(baseHeight * percent);
					g.setColor(new Color(
							Math.max(Math.min((int) Math.round(255 * percent), 255), 0),
							Math.max(Math.min((int) Math.round(255 * (1 / percent)), 255), 0),
							0));
					g.fillRect(
							gBounds.width - (i * width),
							gBounds.height - height,
							width,
							height);
				}
			}
		}
	}
	
	public boolean takeScreenshot(Point mouseLocation) {
		return takeScreenshot(client.getGamePanel().getWidth(), client.getGamePanel().getHeight(), mouseLocation);
	}
	
	public boolean takeScreenshot(int width, int height, Point mouseLocation) {
		boolean success = false;
		BufferedImage screenshotImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
		Graphics g = screenshotImage.createGraphics();
		g.setClip(0, 0, width, height);
		paintComponent(g);
		try {
			screenshotsDir.mkdirs();
			File screenshotFile = new File(screenshotsDir, "%s.png".formatted(LocalDateTime.now().format(screenshotNameFormatter)));
			success = ImageIO.write(screenshotImage, "PNG", screenshotFile);
			if (success)
				Logger.logMessage(Logger.INFO, "Screenshot saved as '%s'", screenshotFile.getName());
			else
				Logger.logMessage(Logger.ERROR, "Failed to save screenshot!");
		} catch (IOException e) {
			Logger.logException("Failed to save screenshot!", e);
		}
		return success;
	}
	
	public void autoScale(int screenWidth, int screenHeight) {
		int area = screenWidth * screenHeight;
		// Auto scale the menu
		if (area < 300000)
			GuiLayer.scale = 0.5;
		else if (area >= 300000 && area < 500000)
			GuiLayer.scale = 1.0;
		else if (area >= 500000 && area < 900000)
			GuiLayer.scale = 1.5;
		else
			GuiLayer.scale = 2.0;
	}
	
	public Set<Integer> getPressedKeys() {
		synchronized (pressedKeys) {
			return Set.copyOf(pressedKeys);
		}
	}
	
	private void clearInputs() {
		synchronized (pressedKeys) {
			pressedKeys.clear();
			updateControlledInputs();
		}
	}
	
	public Client getClient() {
		return client;
	}
	
	public List<GuiLayer> getGuiLayers() {
		return guiLayers;
	}
	
	public GuiLayer getTopGui() {
		if (menuStack.isEmpty()) {
			if (guiLayers.size() > 0)
				return guiLayers.get(guiLayers.size() - 1);
			else
				return null;
		} else {
			return menuStack.peek();
		}
	}
	
	public GuiMenu getCurrentMenu() {
		if (menuStack.size() > 0)
			return menuStack.peek();
		else
			return null;
	}
	
	public void openMenu(GuiMenu menu) {
		clearInputs();
		menuStack.push(menu);
	}
	
	public GuiMenu closeMenu() {
		GuiMenu closedMenu =  menuStack.pop();
		closedMenu.onClose();
		return closedMenu;
	}
	
	public boolean closeMenu(GuiMenu menu) {
		menu.onClose();
		return menuStack.remove(menu);
	}
	
	public void closeAllMenus() {
		menuStack.clear();
	}
	
	public Font getDefaultFont() {
		return defaultFont;
	}
	
	public int getFps() {
		return fps;
	}
	
	public String getLastExceptionText() {
		return lastExceptionText;
	}
	
	public long getLastExceptionTime() {
		return lastExceptionTime;
	}
	
	public void setLastExceptionInfo(String lastExceptionText, long lastExceptionTime) {
		this.lastExceptionText = lastExceptionText;
		this.lastExceptionTime = lastExceptionTime;
	}

}
