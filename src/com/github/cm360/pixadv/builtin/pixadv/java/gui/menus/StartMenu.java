package com.github.cm360.pixadv.builtin.pixadv.java.gui.menus;

import java.awt.Container;
import java.awt.Font;
import java.awt.Window;
import java.awt.geom.Rectangle2D;
import java.io.File;

import com.github.cm360.pixadv.builtin.pixadv.java.entities.types.terra.HumanPlayer;
import com.github.cm360.pixadv.builtin.pixadv.java.gui.components.generic.GuiButton;
import com.github.cm360.pixadv.builtin.pixadv.java.gui.components.generic.GuiImage;
import com.github.cm360.pixadv.graphics.gui.input.KeyCombo;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiMenu;
import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Identifier;
import com.github.cm360.pixadv.util.Logger;

public class StartMenu extends GuiMenu {

	public StartMenu(Client client) {
		super();
		// Main font
		Font menuFont = client.getGamePanel().getDefaultFont().deriveFont(24f);
		// Logo
		GuiImage logoImage = new GuiImage(this, parentBounds -> {
			return new Rectangle2D.Double(
					(parentBounds.getWidth() * 0.5) - (155 * scale),
					(parentBounds.getHeight() * 0.5) - (130 * scale),
					310 * scale,
					110 * scale
				);
		});
		logoImage.setTextures(new Identifier[] { Identifier.parse("pixadv:textures/gui/menu/title/logo") });
		children.add(logoImage);
		// Singleplayer button
		GuiButton singleplayerButton = new GuiButton(this, parentBounds -> {
			return new Rectangle2D.Double(
					(parentBounds.getWidth() * 0.5) - (120 * scale),
					(parentBounds.getHeight() * 0.5) + (16 * scale),
					240 * scale,
					32 * scale
				);
		}, "Singleplayer", menuFont);
		singleplayerButton.setTextures(new Identifier[] { Identifier.parse("pixadv:textures/gui/menu/title/singleplayer") });
		singleplayerButton.registerEvent(new KeyCombo(1), () -> {
			// TODO load singleplayer menu
			client.getGamePanel().closeMenu();
			client.getTaskQueueManager().addGenericTask(() -> {
				client.load(new File(".\\data\\saves\\Universe Zero"));
				// pixadv:textures/entities/girl
				HumanPlayer player = new HumanPlayer(client.getRegistry().getTexture(Identifier.parse("pixadv:mario")));
				client.getCurrentUniverse().getCurrentWorld().addEntity(client.getPlayerId(), player);
			});
		});
		children.add(singleplayerButton);
		// Multiplayer button
		GuiButton multiplayerButton = new GuiButton(this, parentBounds -> {
			return new Rectangle2D.Double(
					(parentBounds.getWidth() * 0.5) - (120 * scale),
					(parentBounds.getHeight() * 0.5) + (52 * scale),
					240 * scale,
					32 * scale
				);
		}, "Multiplayer", menuFont);
		multiplayerButton.setTextures(new Identifier[] { Identifier.parse("pixadv:textures/gui/menu/title/multiplayer") });
		multiplayerButton.registerEvent(new KeyCombo(1), () -> {
			// TODO load multiplayer menu
			client.getGamePanel().closeMenu();
			client.connect("127.0.0.1", 43234);
		});
		children.add(multiplayerButton);
		// Options button
		GuiButton optionsButton = new GuiButton(this, parentBounds -> {
			return new Rectangle2D.Double(
					(parentBounds.getWidth() * 0.5) - (120 * scale),
					(parentBounds.getHeight() * 0.5) + (88 * scale),
					118 * scale,
					32 * scale
				);
		}, "Options", menuFont);
		optionsButton.setTextures(new Identifier[] { Identifier.parse("pixadv:textures/gui/menu/title/options") });
		optionsButton.registerEvent(new KeyCombo(1), () -> {
			// TODO load options menu
		});
		children.add(optionsButton);
		// Quit button
		GuiButton quitButton = new GuiButton(this, parentBounds -> {
			return new Rectangle2D.Double(
					(parentBounds.getWidth() * 0.5) + (2 * scale),
					(parentBounds.getHeight() * 0.5) + (88 * scale),
					118 * scale,
					32 * scale
				);
		}, "Quit", menuFont);
		quitButton.setTextures(new Identifier[] { Identifier.parse("pixadv:textures/gui/menu/title/quit") });
		quitButton.registerEvent(new KeyCombo(1), () -> {
			Container gamePanelContainer = client.getClientFrame();
			if (gamePanelContainer instanceof Window) {
				((Window) gamePanelContainer).dispose();
			} else {
				Logger.logMessage(Logger.WARNING, "Top level ancestor is not an instance of '%s'!", Window.class.getName());
			}
		});
		children.add(quitButton);
	}

	@Override
	public void onClose() {
		// TODO Auto-generated method stub
		
	}

}
