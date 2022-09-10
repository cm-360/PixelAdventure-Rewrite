package com.github.cm360.pixadv.builtin.pixadv.java.gui.huds;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import com.github.cm360.pixadv.graphics.gui.input.KeyCombo;
import com.github.cm360.pixadv.graphics.gui.layouts.GuiMenu;
import com.github.cm360.pixadv.network.endpoints.Client;
import com.github.cm360.pixadv.registry.Registry;
import com.github.cm360.pixadv.world.storage.universe.Universe;

public class ChatHud extends GuiMenu {

	protected Client client;
	protected Consumer<ChatHud> selfClose;
	protected List<String> messageHistory;
	protected List<String> sentHistory;
	protected String text;
	protected int caret;
	protected int selection;
	
	public ChatHud(Client client, Consumer<ChatHud> selfClose) {
		this(client, selfClose, new ArrayList<String>(), new ArrayList<String>());
	}
	
	public ChatHud(Client client, Consumer<ChatHud> selfClose, List<String> messageHistory, List<String> sentHistory) {
		this(client, selfClose, "", messageHistory, sentHistory);
	}
	
	public ChatHud(Client client, Consumer<ChatHud> selfClose, String text, List<String> messageHistory, List<String> sentHistory) {
		this.client = client;
		this.selfClose = selfClose;
		this.messageHistory = messageHistory;
		this.sentHistory = sentHistory;
		this.text = text;
	}
	
	@Override
	protected void paintSelf(Graphics g, Registry registry) {
		Rectangle gBounds = g.getClipBounds();
		
		//
		int screenBottom = gBounds.y + gBounds.height;
		
		//
		int boxMargin = 8;
		int boxSpacing = 12;
		int textSize = 20;
		int textMargin = 10;
		//
		int boxWidth = (gBounds.width / 2) - (boxMargin * 2);
		// Font
		g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, textSize));
		FontMetrics gfm = g.getFontMetrics();
		// Draw chat boxes
		int leftX = gBounds.x + boxMargin;
		
		g.setColor(new Color(0, 0, 0, 192));
		// Chat input box
		g.fillRect(
				leftX,
				screenBottom - (gfm.getHeight() + boxMargin + textMargin),
				boxWidth,
				gfm.getHeight() + textMargin);
		// Chat history box
		int historySize = messageHistory.size();
		int historyHeight = (historySize * gfm.getHeight()) + textMargin;
		if (historySize > 0) {
			g.fillRect(
					leftX,
					screenBottom - ((gfm.getHeight() + boxMargin + textMargin) + boxSpacing + historyHeight),
					boxWidth,
					historyHeight);
		}
		// Draw chat content
		g.setColor(Color.WHITE);
		g.drawString(text,
				leftX + textMargin,
				screenBottom - (boxMargin + textMargin));
		// TODO draw cursor
		if (historySize > 0) {
			for (int i = 0; i < historySize; i++) {
				g.drawString(messageHistory.get(i),
						leftX + textMargin,
						screenBottom - ((gfm.getHeight() + boxMargin + textMargin) + boxSpacing + historyHeight) + ((i + 1) * (gfm.getHeight())));
			}
		}
	}
	
	@Override
	public void interactKey(KeyCombo keys) {
		Set<Integer> keyCodes = Set.of(keys.getKeys());
		for (int keyCode : keyCodes) {
			switch (keyCode) {
			case KeyEvent.VK_ENTER:
				Universe universe = client.getCurrentUniverse();
				if (universe != null) {
					sentHistory.add(text);
					if (text.startsWith("/")) {
						messageHistory.add(universe.getCommandProcessor().processCommand(text.substring(1)));
					} else {
						// TODO send chat message
						messageHistory.add(text);
					}
					sentHistory.add(text);
					text = "";
					selfClose.accept(this);
				}
				break;
			case KeyEvent.VK_BACK_SPACE:
				if (!text.isEmpty())
					text = text.substring(0, text.length() - 1);
				break;
			default:
				if (!Character.isISOControl(keyCode)) {
					String typed = Character.toString(keyCode);
					if (keyCodes.contains(KeyEvent.VK_SHIFT))
						typed = typed.toUpperCase();
					else
						typed = typed.toLowerCase();
					text += typed;
				}
			}
		}
	}
	
	

	public List<String> getMessageHistory() {
		return messageHistory;
	}

	public List<String> getSentHistory() {
		return sentHistory;
	}

	@Override
	public void onClose() {
		// TODO Auto-generated method stub
		
	}

}
